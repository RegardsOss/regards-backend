/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * An implementation of {@link IEmailService}
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Service
@MultitenantTransactional
public class EmailService extends AbstractEmailService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    /**
     * CRUD repository managing emails
     */
    private final IEmailRepository emailRepository;

    /**
     * Spring Framework interface for sending email
     */
    private final JavaMailSender mailSender;

    /**
     * Creates an {@link EmailService} wired to the given {@link IEmailRepository}.
     * @param pEmailRepository Autowired by Spring. Must not be {@literal null}.
     * @param pMailSender Autowired by Spring. Must not be {@literal null}.
     */
    public EmailService(final IEmailRepository pEmailRepository, final JavaMailSender pMailSender) {
        super();
        emailRepository = pEmailRepository;
        mailSender = pMailSender;
    }

    @Override
    public List<Email> retrieveEmails() {
        final List<Email> emails = new ArrayList<>();
        final Iterable<Email> results = emailRepository.findAll();
        if (results != null) {
            results.forEach(emails::add);
        }
        return emails;
    }

    @Override
    public Email sendEmail(final SimpleMailMessage msg) {
        // Create the saveable DTO
        Email email = createEmailFromSimpleMailMessage(msg);
        email = emailRepository.save(email);
        sendMailWithSender(msg);
        return email;
    }

    @Override
    public Email sendEmail(SimpleMailMessage msg, String attName, InputStreamSource attSource) {
        Email email = createEmailFromSimpleMailMessage(msg, attName, attSource);
        email = emailRepository.save(email);
        sendMailWithSender(msg, attName, attSource);
        return email;
    }

    @Override
    public Email retrieveEmail(final Long id) throws ModuleException {
        final Email email = emailRepository.findOne(id);
        if (email == null) {
            throw new EntityNotFoundException(id, Email.class);
        }
        return email;
    }

    @Override
    public void resendEmail(final Long id) throws ModuleException {
        final Email email = retrieveEmail(id);
        final SimpleMailMessage message = createSimpleMailMessageFromEmail(email);
        mailSender.send(message);
    }

    @Override
    public void deleteEmail(final Long id) {
        emailRepository.delete(id);
    }

    @Override
    public boolean exists(final Long id) {
        return emailRepository.exists(id);
    }

    /**
     * Create a {@link SimpleMailMessage} with same content as the passed {@link Email}.
     * @param pEmail The {@link Email}
     * @return The {@link SimpleMailMessage}
     */
    private SimpleMailMessage createSimpleMailMessageFromEmail(final Email pEmail) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setBcc(pEmail.getBcc());
        message.setCc(pEmail.getCc());
        message.setFrom(pEmail.getFrom());
        message.setReplyTo(pEmail.getReplyTo());
        if (pEmail.getSentDate() != null) {
            message.setSentDate(Date.from(pEmail.getSentDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        message.setSubject(pEmail.getSubject());
        message.setText(pEmail.getText());
        message.setTo(pEmail.getTo());
        return message;
    }

    /**
     * Create a domain {@link Email} with same content as the passed {@link SimpleMailMessage} in order to save it in
     * db.
     * @param msg The message
     * @return The savable email
     */
    private Email createEmailFromSimpleMailMessage(final SimpleMailMessage msg) {
        return createEmailFromSimpleMailMessage(msg, null, null);
    }

    /**
     * Create a domain {@link Email} with same content as the passed {@link SimpleMailMessage} in order to save it in
     * db.
     * @param pMessage The message
     * @return The savable email
     */
    private Email createEmailFromSimpleMailMessage(final SimpleMailMessage pMessage, String attName,
            InputStreamSource source) {
        final Email email = new Email();
        email.setBcc(pMessage.getBcc());
        email.setCc(pMessage.getCc());
        email.setFrom(pMessage.getFrom());
        email.setReplyTo(pMessage.getReplyTo());
        if (pMessage.getSentDate() != null) {
            email.setSentDate(LocalDateTime.ofInstant(pMessage.getSentDate().toInstant(), ZoneId.systemDefault()));
        }
        email.setSubject(pMessage.getSubject());
        email.setText(pMessage.getText());
        email.setTo(pMessage.getTo());

        if ((attName != null) && (source != null)) {
            try {
                InputStream is = source.getInputStream();
                // Use atachment name to know if it is a zipped file (simplest method)
                if (attName.toLowerCase().endsWith("zip")) {
                    email.setAttName(attName);
                    // already a zip
                    email.setAttachment(ByteStreams.toByteArray(is));
                } else { // else zip the file
                    email.setAttName(attName + ".zip");
                    // Write zip file into a byte array
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(baos);
                    zos.putNextEntry(new ZipEntry(attName));
                    ByteStreams.copy(is, zos);
                    zos.closeEntry();
                    zos.flush();
                    zos.finish();
                    email.setAttachment(baos.toByteArray());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return email;
    }

    @Override
    protected JavaMailSender getMailSender() {
        return mailSender;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
