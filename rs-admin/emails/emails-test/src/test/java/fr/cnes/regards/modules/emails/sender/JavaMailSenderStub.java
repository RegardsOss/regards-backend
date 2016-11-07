/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.sender;

import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 *
 * Stub class to not really send emails.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 *
 */
public class JavaMailSenderStub implements JavaMailSender {

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        // Nothing to do
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        // Nothing to do
    }

    @Override
    public MimeMessage createMimeMessage() {
        return null;
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        return null;
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        // Nothing to do
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        // Nothing to do
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        // Nothing to do
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        // Nothing to do
    }

}
