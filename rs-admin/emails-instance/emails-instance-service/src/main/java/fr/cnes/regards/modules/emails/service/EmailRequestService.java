/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.emails.service;

import jakarta.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.emails.dao.EmailRequestRepository;
import fr.cnes.regards.modules.emails.domain.EmailRequest;
import fr.cnes.regards.modules.emails.exception.RsEmailException;
import fr.cnes.regards.modules.notification.service.IInstanceNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * An implementation of email request service
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Profile("!nomail")
@Service
@RegardsTransactional
public class EmailRequestService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRequestService.class);

    /**
     * CRUD repository managing emails
     */
    private final EmailRequestRepository emailRequestRepository;

    /**
     * Spring Framework interface for sending email
     */
    private final JavaMailSender mailSender;

    /**
     * Service class to manage notifications
     */
    private final IInstanceNotificationService instanceNotificationService;

    @Value("${regards.mails.noreply.address:regards@noreply.fr}")
    private String defaultSender;

    /**
     * First range for the delay between attempts to send an email (in second)
     */
    @Value("${regards.send.email.delay.first.range:60}")
    private int firstRangeDelayTrySend;

    /**
     * Second range for the delay between attempts to send an email (in second)
     */
    @Value("${regards.send.email.delay.second.range:3600}")
    private int secondRangeDelayTrySend;

    /**
     * Third range for the delay between attempts to send an email (in second)
     */
    @Value("${regards.send.email.delay.third.range:86400}")
    private int thirdRangeDelayTrySend;

    /**
     * Creates an {@link EmailRequestService} wired to the given {@link EmailRequestRepository} and the given {@link JavaMailSender}.
     *
     * @param emailRequestRepository Autowired by Spring. Must not be {@literal null}.
     * @param mailSender             Autowired by Spring. Must not be {@literal null}.
     */
    public EmailRequestService(final EmailRequestRepository emailRequestRepository,
                               final JavaMailSender mailSender,
                               final IInstanceNotificationService instanceNotificationService) {
        this.emailRequestRepository = emailRequestRepository;
        this.mailSender = mailSender;
        this.instanceNotificationService = instanceNotificationService;
    }

    /**
     * Save the email in database.
     * The email will be send by an asynchronous process thanks to a scheduler : {@link EmailRequestSchedulerService}.
     */
    public EmailRequest saveEmailRequest(SimpleMailMessage mailMessage,
                                         @Nullable String attachmentName,
                                         @Nullable InputStreamSource attachmentSource) {
        return emailRequestRepository.save(createEmailRequestFromMailMessage(mailMessage,
                                                                             attachmentName,
                                                                             attachmentSource));
    }

    /**
     * Helper method to save mail in database without creating SimpleMailMessage before call {@link  #saveEmailRequest}
     *
     * @param message The email message
     * @param subject The email subject
     * @param from    The email sender, if you don't care about who is sending, set null to use default sender
     *                {@link  #defaultSender}.
     * @param to      Recipients
     */
    public EmailRequest saveEmailRequest(String message, String subject, @Nullable String from, String... to) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(from);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailMessage.setSentDate(new Date());

        return saveEmailRequest(mailMessage, null, null);
    }

    /**
     * Send a email with default sender {@link  #defaultSender} if the from of given mail message is empty.
     *
     * @param mailMessage      the mail message
     * @param attachmentName   the name of attachment
     * @param attachmentSource the input stream for attachment
     */
    public void sendEmail(SimpleMailMessage mailMessage,
                          @Nullable String attachmentName,
                          @Nullable InputStreamSource attachmentSource) throws RsEmailException {
        sendEmailWithSender(mailMessage, attachmentName, attachmentSource, defaultSender);
    }

    /**
     * Send a email with given sender if the from of given mail message is empty.
     *
     * @param mailMessage      the mail message
     * @param attachmentName   the name of attachment
     * @param attachmentSource the input stream for attachment
     * @param sender           the sender of email to send
     */
    public void sendEmailWithSender(SimpleMailMessage mailMessage,
                                    @Nullable String attachmentName,
                                    @Nullable InputStreamSource attachmentSource,
                                    @Nullable String sender) throws RsEmailException {

        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            boolean withAttachment = (attachmentName != null) && (attachmentSource != null);
            MimeMessageHelper mimeMsgHelper = new MimeMessageHelper(mimeMsg, withAttachment);

            mimeMsgHelper.setText(mailMessage.getText(), true);
            mimeMsgHelper.setTo(mailMessage.getTo());
            String[] bcc = mailMessage.getBcc();
            if (bcc != null) {
                mimeMsgHelper.setBcc(bcc);
            }
            String[] cc = mailMessage.getCc();
            if (cc != null) {
                mimeMsgHelper.setCc(cc);
            }
            String from = mailMessage.getFrom();
            if (StringUtils.isBlank(from)) {
                mimeMsgHelper.setFrom(sender);
            } else {
                mimeMsgHelper.setFrom(from);
            }
            String replyTo = mailMessage.getReplyTo();
            if (!StringUtils.isBlank(replyTo)) {
                mimeMsgHelper.setReplyTo(replyTo);
            }
            Date sentDate = mailMessage.getSentDate();
            if (sentDate != null) {
                mimeMsgHelper.setSentDate(sentDate);
            }
            String subject = mailMessage.getSubject();
            if (!StringUtils.isBlank(subject)) {
                mimeMsgHelper.setSubject(subject);
            }
            if (withAttachment) {
                mimeMsgHelper.addAttachment(attachmentName, attachmentSource);
            }
            // Send email
            mailSender.send(mimeMsg);
            LOGGER.info("Send a email : {}", mailMessage);
        } catch (MessagingException | MailException e) {
            LOGGER.warn("Error while trying to send an email. Recipient: [{}] - Subject: [{}] - Root Cause: [{}]",
                        mailMessage.getTo(),
                        mailMessage.getSubject(),
                        Throwables.getRootCause(e).toString());
            throw new RsEmailException(e);
        }
    }

    /**
     * Create a domain {@link EmailRequest} with same content as the passed {@link SimpleMailMessage} in order to save it in
     * database.
     *
     * @param mailMessage The message
     * @return The saved email
     */
    private EmailRequest createEmailRequestFromMailMessage(SimpleMailMessage mailMessage,
                                                           @Nullable String attachmentName,
                                                           @Nullable InputStreamSource attachmentSource) {
        final EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(mailMessage.getTo());
        emailRequest.setFrom(StringUtils.isBlank(mailMessage.getFrom()) ? defaultSender : mailMessage.getFrom());
        emailRequest.setBcc(mailMessage.getBcc());
        emailRequest.setCc(mailMessage.getCc());

        String subject = mailMessage.getSubject();
        if (subject != null && subject.length() > EmailRequest.MAX_SUBJECT_SIZE) {
            emailRequest.setSubject(subject.substring(0, EmailRequest.MAX_SUBJECT_SIZE - 1));
        } else {
            emailRequest.setSubject(subject);
        }
        emailRequest.setText(mailMessage.getText());
        emailRequest.setReplyTo(mailMessage.getReplyTo());
        emailRequest.setNextTryDate(OffsetDateTime.now());

        if ((attachmentName != null) && (attachmentSource != null)) {
            try {
                InputStream is = attachmentSource.getInputStream();
                // Use attachment name to know if it is a zipped file (simplest method)
                if (attachmentName.toLowerCase().endsWith("zip")) {
                    emailRequest.setAttachmentName(attachmentName);
                    emailRequest.setAttachment(ByteStreams.toByteArray(is));
                } else { // else zip the file
                    emailRequest.setAttachmentName(attachmentName + ".zip");
                    // Write zip file into a byte array
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ZipOutputStream zos = new ZipOutputStream(baos)) {
                        zos.putNextEntry(new ZipEntry(attachmentName));
                        ByteStreams.copy(is, zos);
                        zos.closeEntry();
                        zos.flush();
                        zos.finish();

                        emailRequest.setAttachment(baos.toByteArray());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error during the setting of attachment for email. Recipient: [{}] - Subject: [{}] - "
                             + "Root Cause: [{}]", emailRequest.getFrom(), emailRequest.getSubject(), e.getMessage());
                throw new RsRuntimeException(e);
            }
        }
        return emailRequest;
    }

    private SimpleMailMessage createMailMessageFromEmailRequest(EmailRequest emailRequest) {
        final SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailRequest.getTo());
        mailMessage.setFrom(emailRequest.getFrom());
        mailMessage.setBcc(emailRequest.getBcc());
        mailMessage.setCc(emailRequest.getCc());
        mailMessage.setSubject(emailRequest.getSubject());
        mailMessage.setText(emailRequest.getText());
        mailMessage.setReplyTo(emailRequest.getReplyTo());
        mailMessage.setSentDate(new Date());

        return mailMessage;
    }

    /**
     * Send a mail with an asynchronous process; method useful in scheduler : {@link EmailRequestSchedulerService}.
     */
    public void sendEmail() {
        List<EmailRequest> emailRequests = emailRequestRepository.findByNextTryDateBefore(OffsetDateTime.now());

        int nbErrorSentEmail = 0;

        for (EmailRequest emailRequest : emailRequests) {
            try {
                ByteArrayResource byteArrayResource = null;
                byte[] attachment = emailRequest.getAttachment();
                if (attachment != null) {
                    byteArrayResource = new ByteArrayResource(attachment);
                }
                // Send email
                sendEmail(createMailMessageFromEmailRequest(emailRequest),
                          emailRequest.getAttachmentName(),
                          byteArrayResource);
                // Delete email request in database after the sending of email without error
                emailRequestRepository.delete(emailRequest);
            } catch (RsEmailException e) {
                nbErrorSentEmail += 1;
                int nbUnsuccessfullTry = emailRequest.getNbUnsuccessfullTry();
                if (nbUnsuccessfullTry >= EmailRequest.MAX_UNSUCCESSFULL_TRY) {
                    LOGGER.error("Unable to send mail. Recipient: [{}] - Subject: [{}] - Root Cause: [{}]",
                                 emailRequest.getTo(),
                                 emailRequest.getSubject(),
                                 Throwables.getRootCause(e).toString());
                    emailRequestRepository.delete(emailRequest);
                } else {
                    nbUnsuccessfullTry += 1;
                    emailRequest.setNextTryDate(emailRequest.getNextTryDate()
                                                            .plusSeconds(getDelayTrySend(nbUnsuccessfullTry)));
                    emailRequest.setNbUnsuccessfullTry(nbUnsuccessfullTry);
                    // Update the email request in order to try a next sending of email
                    emailRequestRepository.save(emailRequest);
                }
            }
        }
        if (nbErrorSentEmail > 0) {
            instanceNotificationService.createNotification(createNotificationDto(nbErrorSentEmail,
                                                                                 emailRequests.size()));
        }
    }

    /**
     * Get the delay to add the date next attempt to send email {@link EmailRequest#setNextTryDate(OffsetDateTime)}
     *
     * @param nbUnsuccessfullTry the number unsuccessfull try
     */
    private int getDelayTrySend(int nbUnsuccessfullTry) {
        return switch (nbUnsuccessfullTry) {
            case 1, 2, 3 -> firstRangeDelayTrySend;
            case 4, 5, 6 -> secondRangeDelayTrySend;
            case 7, 8, 9 -> thirdRangeDelayTrySend;
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Create a notification {@link NotificationDTO}
     *
     * @param nbErrorSentEmail the number of errors which occured during the sending email
     * @param nbEmailRequest   the number of email requests to manage
     * @return the new notification
     */
    private NotificationDTO createNotificationDto(int nbErrorSentEmail, int nbEmailRequest) {
        NotificationDTO notificationDto = new NotificationDTO();
        notificationDto.setMessage(String.format(
            "Number of error(s) during sent emails [%d] - Number of found request(s) to send emails [%d]",
            nbErrorSentEmail,
            nbEmailRequest));
        notificationDto.setTitle("Error during sent emails");
        notificationDto.setSender("rs-admin-instance");
        notificationDto.setLevel(NotificationLevel.INFO);
        notificationDto.setMimeType(MimeType.valueOf("text/plain"));
        notificationDto.setRoleRecipients(Set.of(DefaultRole.INSTANCE_ADMIN.toString()));
        notificationDto.setProjectUserRecipients(Set.of(defaultSender));

        return notificationDto;
    }

}
