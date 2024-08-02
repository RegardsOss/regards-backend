/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.smtp.SMTPAddressFailedException;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.EmailRequestRepository;
import fr.cnes.regards.modules.emails.domain.EmailRequest;
import fr.cnes.regards.modules.emails.exception.RsEmailException;
import fr.cnes.regards.modules.notification.service.IInstanceNotificationService;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link EmailRequestService}.
 *
 * @author Xavier-Alexandre Brochard
 */
@RegardsTransactional
@ContextConfiguration(classes = EmailRequestConfiguration.class)
@TestPropertySource(properties = { "regards.accounts.root.user.login=rootAdminUserLoginTest" })
public class EmailRequestServiceIT extends AbstractRegardsIT {

    private static final LocalDateTime SEND_DATE = LocalDateTime.now().minusMinutes(5);

    private static final String TO = "xavier-alexandre.brochard@c-s.fr";

    private static final String FROM = "regards@noreply.fr";

    private static final String SUBJECT = "subject";

    /**
     * The tested service
     */
    private EmailRequestService emailRequestService;

    @Autowired
    private EmailRequestRepository emailRequestRepository;

    @Autowired
    private IInstanceNotificationService instanceNotificationService;

    /**
     * Defines a rule starting an SMTP server before each test. All test emails will be sent to this server instead of
     * the production SMTP. This will allow us to retrieve the emails we sent to this server and check that they were
     * properly sent and correspond to the expected.
     * </p>
     * <b>WARNING:</b><br>
     * The Green Mail server allows to retrieve mail as {@link jakarta.mail.internet.MimeMessage}, and not {@link SimpleMailMessage}!
     * </p>
     * About the library:<br>
     * GreenMail is an open source, intuitive and easy-to-use test suite of email servers for both receiving and
     * retrieving emails from Java. <br>
     * GreenMail responds like a regular SMTP server but does not deliver any email, which enables it to be used in real
     * life applications and real test cases. Messages can easily be extracted, verified and modified.
     * <p>
     */
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Before
    public void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // Inject the SMTP session from GreenMail server to the JavaMailSender
        final Session smtpSession = greenMail.getSmtp().createSession();
        mailSender.setSession(smtpSession);

        emailRequestService = new EmailRequestService(emailRequestRepository, mailSender, instanceNotificationService);
    }

    @Test
    public void saveEmail() {
        // Given : create dummy mail message
        final SimpleMailMessage expected = createDummyMailMessage();

        // When
        EmailRequest emailRequest = emailRequestService.saveEmailRequest(expected, null, null);

        // Then
        assertNotNull(emailRequest);
        assertNotNull(emailRequest.getId());
        assertArrayEquals(expected.getTo(), emailRequest.getTo());
        assertEquals(expected.getFrom(), emailRequest.getFrom());
        assertEquals(expected.getSubject(), emailRequest.getSubject());
        assertEquals(expected.getText(), emailRequest.getText());
        assertNotNull(emailRequest.getNextTryDate());
        assertTrue(emailRequest.getNextTryDate().isBefore(OffsetDateTime.now()));
        assertEquals(0, emailRequest.getNbUnsuccessfullTry());
        assertNull(emailRequest.getAttachmentName());
        assertNull(emailRequest.getAttachment());
    }

    /**
     * Check that the system allows to send an email to a list of recipients.
     *
     * @throws jakarta.mail.MessagingException Exception thrown by getters of {@link jakarta.mail.internet.MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail_without_attachment() throws RsEmailException, MessagingException {
        // Given
        // Create dummy email with random subject and content
        final SimpleMailMessage expected = createDummyMailMessage();

        // When : Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailRequestService.sendEmailWithSender(expected, null, null, FROM);

        // Then : retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        check_mailMessage_with_mimeMessage(expected, emails[0]);
    }

    @Test
    public void sendEmail_with_attachment() throws MessagingException, IOException, RsEmailException {
        // Given
        // Create dummy email with random subject and content
        final SimpleMailMessage expected = createDummyMailMessage();

        String attachmentName = "fichier.txt";
        InputStreamSource attachmentSource = () -> this.getClass().getResourceAsStream(File.separator + attachmentName);

        // When : send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailRequestService.sendEmail(expected, attachmentName, attachmentSource);

        // Then : retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Only presence of attachments is interesting us
        final MimeMessage result = emails[0];
        assertEquals(attachmentName, ((MimeMultipart) result.getContent()).getBodyPart(1).getFileName());
        assertEquals("text/plain; charset=UTF-8; name=" + attachmentName,
                     ((MimeMultipart) result.getContent()).getBodyPart(1).getContentType());

    }

    @Test
    public void sendEmail_with_zipAttachment() throws MessagingException, IOException, RsEmailException {
        // Given : create dummy mail message
        final SimpleMailMessage expected = createDummyMailMessage();

        String attachmentName = "fichier.zip";
        InputStreamSource attachmentSource = () -> this.getClass().getResourceAsStream(File.separator + attachmentName);

        // When : send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailRequestService.sendEmail(expected, attachmentName, attachmentSource);

        // Then : retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Only presence of attachments is interesting us
        final MimeMessage result = emails[0];
        assertEquals(attachmentName, ((MimeMultipart) result.getContent()).getBodyPart(1).getFileName());
        assertEquals("application/zip; name=" + attachmentName,
                     ((MimeMultipart) result.getContent()).getBodyPart(1).getContentType());

    }

    @Test
    public void sendEmail_with_failure() {
        // Given
        JavaMailSenderImpl mockMailSender = Mockito.mock(JavaMailSenderImpl.class);
        String rootCauseMessage = "User Unknown";
        SMTPAddressFailedException smtpAddressFailedException = new SMTPAddressFailedException(new InternetAddress(),
                                                                                               "cmd",
                                                                                               550,
                                                                                               rootCauseMessage);
        SendFailedException sendFailedException = new SendFailedException("Invalid Adresses",
                                                                          smtpAddressFailedException);
        MailSendException mailSendException = new MailSendException("Could not Send mail", sendFailedException);
        // Throw mailSendException when send email
        Mockito.doThrow(mailSendException).when(mockMailSender).send(any(MimeMessage.class));

        Mockito.when(mockMailSender.createMimeMessage()).thenCallRealMethod();
        // Create a new service for EmailRequest
        emailRequestService = new EmailRequestService(emailRequestRepository,
                                                      mockMailSender,
                                                      instanceNotificationService);

        LogCaptor logCaptor = LogCaptor.forClass(EmailRequestService.class);

        // When
        try {
            emailRequestService.sendEmail(createDummyMailMessage(), null, null);
            fail();
        } catch (RsEmailException e) {
            // Then
            Assertions.assertThat(logCaptor.getWarnLogs()).hasSize(1);
            Assertions.assertThat(logCaptor.getWarnLogs().get(0))
                      .contains(TO, SUBJECT, rootCauseMessage, smtpAddressFailedException.getClass().getSimpleName());
        }
    }

    /**
     * Creates a {@link SimpleMailMessage}.
     *
     * @return The dummy mail message
     */
    private SimpleMailMessage createDummyMailMessage() {
        final SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(TO);
        mailMessage.setFrom(FROM);
        mailMessage.setSubject(SUBJECT);
        mailMessage.setText("messageTest");
        mailMessage.setSentDate(Date.from(SEND_DATE.atZone(ZoneId.systemDefault()).toInstant()));

        return mailMessage;
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     *
     * @param expected The expected email as {@code Email}
     * @param result   The compared email as {@code MimeMessage}
     * @throws MessagingException Exception thrown by getters of {@link MimeMessage}
     */
    private void check_mailMessage_with_mimeMessage(final SimpleMailMessage expected, final MimeMessage result)
        throws MessagingException {
        // Check subject
        assertEquals("Expected and actual subject are different", expected.getSubject(), result.getSubject());
        // Check mail body
        assertEquals("Expected and actual body are different", expected.getText(), GreenMailUtil.getBody(result));
        // Check mail sender address
        assertEquals("Expected and actual From are different", expected.getFrom(), result.getFrom()[0].toString());
        // Check mail recipients
        List<String> expectedRecipients = Arrays.asList(expected.getTo());

        List<Address> resultAddresses = Arrays.asList(result.getRecipients(MimeMessage.RecipientType.TO));
        List<String> resultRecipients = resultAddresses.stream().map(Address::toString).collect(Collectors.toList());

        assertNotNull(expectedRecipients);
        assertNotNull(resultAddresses);
        assertNotNull(resultRecipients);
        assertEquals(expectedRecipients.size(), resultRecipients.size());
    }

}
