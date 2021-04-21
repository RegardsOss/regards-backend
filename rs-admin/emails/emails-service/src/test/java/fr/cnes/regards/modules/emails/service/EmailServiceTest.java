/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.junit4.SpringRunner;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Test class for {@link EmailService}.
 * @author Xavier-Alexandre Brochard
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class EmailServiceTest {

    /**
     * The tested service
     */
    private EmailService emailService;

    /**
     * Mock repository
     */
    @Autowired
    private IEmailRepository emailRepository;

    /**
     * Interface defining a strategy for sending mails
     */
    private JavaMailSenderImpl mailSender;

    /**
     * Test sentDate
     */
    private static final LocalDateTime SEND_DATE = LocalDateTime.now().minusMinutes(5);

    /**
     * Defines a rule starting an SMTP server before each test. All test emails will be sent to this server instead of
     * the production SMTP. This will allow us to retrieve the emails we sent to this server and check that they were
     * properly sent and correspond to the expected.
     * </p>
     * <b>WARNING:</b><br>
     * The Green Mail server allows to retrieve mail as {@link MimeMessage}, and not {@link SimpleMailMessage}!
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

    @Configuration
    @PropertySource(value = { "classpath:test.properties" })
    public static class Config {

        @Bean
        public IInstanceSubscriber instanceSubscriber() {
            return Mockito.mock(IInstanceSubscriber.class);
        }

        @Bean
        public IInstancePublisher instancePublisher() {
            return Mockito.mock(IInstancePublisher.class);
        }
    }

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        emailRepository.deleteAll();

        mailSender = new JavaMailSenderImpl();

        // Inject the SMTP session from GreenMail server to the JavaMailSender
        final Session smtpSession = greenMail.getSmtp().createSession();
        mailSender.setSession(smtpSession);

        emailService = new EmailService(emailRepository, mailSender);
    }

    /**
     * Check that the system allows to send an email to a list of recipients.
     * @throws MessagingException Exception thrown by getters of {@link MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() throws MessagingException {
        // Create dummy email with random subject and content
        final SimpleMailMessage expected = createDummyMessage();

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        // emailService.sendEmail(recipients, expected);
        emailService.sendEmail(expected);

        // Retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        final MimeMessage result = emails[0];
        checkSMMVsMimeMessage(expected, result);
    }

    @Test
    public void sendEmailWithAttachment() throws MessagingException, IOException {
        // Create dummy email with random subject and content
        final SimpleMailMessage expected = createDummyMessage();

        String attName = "fichier.txt";
        InputStreamSource attSource = () -> this.getClass().getResourceAsStream("/fichier.txt");

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        // emailService.sendEmail(recipients, expected);
        emailService.sendEmail(expected, attName, attSource);

        // Retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Only presence of attachments is interesting us
        final MimeMessage result = emails[0];
        Assert.assertEquals("fichier.txt", ((MimeMultipart) result.getContent()).getBodyPart(1).getFileName());
        Assert.assertEquals("text/plain; charset=UTF-8; name=fichier.txt",
                            ((MimeMultipart) result.getContent()).getBodyPart(1).getContentType());

    }

    @Test
    public void sendEmailWithZipAttachment() throws MessagingException, IOException {
        // Create dummy email with random subject and content
        final SimpleMailMessage expected = createDummyMessage();

        String attName = "fichier.zip";
        InputStreamSource attSource = () -> this.getClass().getResourceAsStream("/fichier.zip");

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        // emailService.sendEmail(recipients, expected);
        emailService.sendEmail(expected, attName, attSource);

        // Retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Only presence of attachments is interesting us
        final MimeMessage result = emails[0];
        Assert.assertEquals("fichier.zip", ((MimeMultipart) result.getContent()).getBodyPart(1).getFileName());
        Assert.assertEquals("application/zip; name=fichier.zip",
                            ((MimeMultipart) result.getContent()).getBodyPart(1).getContentType());

    }

    /**
     * Check that the system allows to retrieve a single email.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve a single email.")
    public void retrieveEmail() throws ModuleException {
        // Create dummy email
        Email expected = emailService.sendEmail(createDummyMessage());

        // The service returns what is returned by the repository
        final Email received = emailService.retrieveEmail(expected.getId());

        // Check that the expected and received emails are equal
        assertThat("Expected and received emails are different", expected, is(equalTo(received)));
    }

    /**
     * Check that the system handles the case where an email cannot be found when re-sending.
     */
    @Test(expected = Exception.class)
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system handles the case where an email cannot be found when re-sending.")
    public void resendEmailNotFound() throws ModuleException {
        final Long id = 999L;
        assumeTrue("EmailDTO given id is expected to not be found", !emailService.exists(id));
        emailService.resendEmail(id);
    }

    /**
     * Check that the system allows to re-send an email.
     * @throws MessagingException Exception thrown by getters of {@link MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to re-send an email and check that the system allows to retrieve the list "
            + "of sent emails..")
    public void resendEmail() throws MessagingException, ModuleException {
        // Create dummy email with random subject and content and add a recipient
        final String[] recipients = new String[] { "recipient@test.com" };
        SimpleMailMessage msg = createDummyMessage();
        msg.setTo(recipients);
        Email expected = emailService.sendEmail(msg);

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailService.resendEmail(expected.getId());

        // Retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(2, emails.length);

        // Check that the received mail is the same as the sent mail
        final MimeMessage result = emails[1];
        checkEmailVsMimeMessage(expected, result);
    }

    /**
     * Check that the system allows to delete an email.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to delete an email.")
    public void deleteEmail() {
        Email email = emailService.sendEmail(createDummyMessage());
        emailService.deleteEmail(email.getId());
    }

    /**
     * Creates a {@link SimpleMailMessage} with some random values initialized.
     * @return The mail
     */
    private SimpleMailMessage createDummyMessage() {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("sender@test.com");
        message.setSubject("subject");
        message.setText("message");
        message.setTo("xavier-alexandre.brochard@c-s.fr");
        message.setSentDate(Date.from(SEND_DATE.atZone(ZoneId.systemDefault()).toInstant()));
        return message;
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     * @param expected The expected email as {@code Email}
     * @param result The compared email as {@code MimeMessage}
     * @throws MessagingException Exception thrown by getters of {@link MimeMessage}
     */
    private void checkEmailVsMimeMessage(final Email expected, final MimeMessage result) throws MessagingException {
        // Check subject
        assertThat("Expected and actual subjects are different", expected.getSubject(),
                   is(equalTo(result.getSubject())));
        // Check mail body
        assertThat("Expected and actual bodies are different", expected.getText(),
                   is(equalTo(GreenMailUtil.getBody(result))));
        // Check mail sender address
        assertThat("Expected and actual From are different", expected.getFrom(),
                   is(equalTo(result.getFrom()[0].toString())));
        // Check mail recipients
        final List<String> expectedRecipients = Arrays.asList(expected.getTo());
        final List<Address> resultAddresses = Arrays.asList(result.getRecipients(RecipientType.TO));
        final List<String> resultRecipients = resultAddresses.stream().map(Address::toString)
                .collect(Collectors.toList());
        assertThat(expectedRecipients, is(equalTo(resultRecipients)));
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     * @param expected The expected email as {@code Email}
     * @param result The compared email as {@code MimeMessage}
     * @throws MessagingException Exception thrown by getters of {@link MimeMessage}
     */
    private void checkSMMVsMimeMessage(final SimpleMailMessage expected, final MimeMessage result)
            throws MessagingException {
        // Check subject
        assertThat("Expected and actual subjects are different", expected.getSubject(),
                   is(equalTo(result.getSubject())));
        // Check mail body
        assertThat("Expected and actual bodies are different", expected.getText(),
                   is(equalTo(GreenMailUtil.getBody(result))));
        // Check mail sender address
        assertThat("Expected and actual From are different", expected.getFrom(),
                   is(equalTo(result.getFrom()[0].toString())));
        // Check mail recipients
        final List<String> expectedRecipients = Arrays.asList(expected.getTo());
        final List<Address> resultAddresses = Arrays.asList(result.getRecipients(RecipientType.TO));
        final List<String> resultRecipients = resultAddresses.stream().map(Address::toString)
                .collect(Collectors.toList());
        assertThat(expectedRecipients, is(equalTo(resultRecipients)));
    }

}
