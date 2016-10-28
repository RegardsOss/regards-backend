/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Test class for {@link EmailService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class EmailServiceTest {

    /**
     * The tested service
     */
    private IEmailService emailService;

    /**
     * Mock repository
     */
    private IEmailRepository emailRepository;

    /**
     * Interface defining a strategy for sending mails
     */
    private JavaMailSenderImpl mailSender;

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
     *
     * @see <a href=http://www.icegreen.com/greenmail/#scenarios>Green Mail documentation</a>
     */
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        emailRepository = mock(IEmailRepository.class);

        mailSender = new JavaMailSenderImpl();

        // Inject the SMTP session from GreenMail server to the JavaMailSender
        final Session smtpSession = greenMail.getSmtp().createSession();
        mailSender.setSession(smtpSession);

        emailService = new EmailService(emailRepository, mailSender);
    }

    /**
     * Check that the system allows to retrieve the list of sent emails.
     *
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve the list of sent emails.")
    public void retrieveEmails() {
        // Create a few emails
        final Email email0 = createDummyEmail();
        final Email email1 = createDummyEmail();

        // Mock the method wich will be called
        final List<Email> expected = new ArrayList<>();
        expected.add(email0);
        expected.add(email1);
        when(emailRepository.findAll()).thenReturn(expected);

        // The service returns what is returned by the repository
        final List<Email> result = emailService.retrieveEmails();
        assertThat("Received list is same size as expected", result.size(), equalTo(expected.size()));

        // Check received mails are same as expected
        for (int i = 0; i < expected.size(); i++) {
            assertThat("Expected and received emails are different", expected.get(i), is(equalTo(result.get(i))));
        }

        // Check that the repository's method was called with right arguments
        verify(emailRepository).findAll();
    }

    /**
     * Check that the system allows to send an email to a list of recipients.
     *
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
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

    /**
     * Check that the system allows to retrieve a single email.
     *
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve a single email.")
    public void retrieveEmail() throws MessagingException {
        final Long id = 0L;

        // Create dummy email
        final Email expected = createDummyEmail();
        expected.setId(id);

        // Mock the method wich will be called
        when(emailRepository.findOne(0L)).thenReturn(expected);

        // The service returns what is returned by the repository
        final Email received = emailService.retrieveEmail(id);

        // Check that the expected and received emails are equal
        assertThat("Expected and received emails are different", expected, is(equalTo(received)));

        // Check that the repository's method was called with right arguments
        verify(emailRepository).findOne(0L);
    }

    /**
     * Check that the system handles the case where an email cannot be found when re-sending.
     */
    @Test(expected = Exception.class)
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system handles the case where an email cannot be found when re-sending.")
    public void resendEmailNotFound() {
        final Long id = 999L;
        assumeTrue("EmailDTO passed id is expected to not be found", !emailService.exists(id));
        emailService.resendEmail(id);
    }

    /**
     * Check that the system allows to re-send an email.
     *
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to re-send an email.")
    public void resendEmail() throws MessagingException {
        final Long id = 0L;

        // Create dummy email with random subject and content and add a recipient
        final Email expected = createDummyEmail();
        final String[] recipients = new String[] { "recipient@test.com" };
        expected.setTo(recipients);

        // Mock the method wich will be called
        when(emailRepository.findOne(id)).thenReturn(expected);

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailService.resendEmail(id);

        // Retrieve the mail
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        final MimeMessage result = emails[0];
        checkEmailVsMimeMessage(expected, result);

        // Check that the repository's method was called with right arguments
        verify(emailRepository).findOne(0L);
    }

    /**
     * Check that the system allows to delete an email.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to delete an email.")
    public void deleteEmail() {
        emailService.deleteEmail(0L);

        // Check that the repository's method was called with right arguments
        verify(emailRepository).delete(0L);
    }

    /**
     * Creates a {@link SimpleMailMessage} with some random values initialized.
     *
     * @return The mail
     */
    private SimpleMailMessage createDummyMessage() {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("sender@test.com");
        message.setSubject("subject");
        message.setText("message");
        message.setTo("xavier-alexandre.brochard@c-s.fr");
        return message;
    }

    /**
     * Creates a {@link Email} with some random values initialized.
     *
     * @return The mail
     */
    private Email createDummyEmail() {
        // Create an empty message
        final Email email = new Email();

        // With random content to avoid potential residual lingering problems
        final String subject = GreenMailUtil.random();
        final String sender = GreenMailUtil.random();
        final String body = GreenMailUtil.random();

        // Set content on the mail
        email.setSubject(subject);
        email.setFrom(sender + "@test.com");
        email.setText(body);

        return email;
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     *
     * @param pExpected
     *            The expected email as {@code Email}
     * @param pResult
     *            The compared email as {@code MimeMessage}
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
     */
    private void checkEmailVsMimeMessage(final Email pExpected, final MimeMessage pResult) throws MessagingException {
        // Check subject
        assertThat("Expected and actual subjects are different", pExpected.getSubject(),
                   is(equalTo(pResult.getSubject())));
        // Check mail body
        assertThat("Expected and actual bodies are different", pExpected.getText(),
                   is(equalTo(GreenMailUtil.getBody(pResult))));
        // Check mail sender address
        assertThat("Expected and actual From are different", pExpected.getFrom(),
                   is(equalTo(pResult.getFrom()[0].toString())));
        // Check mail recipients
        final List<String> expectedRecipients = Arrays.asList(pExpected.getTo());
        final List<Address> resultAddresses = Arrays.asList(pResult.getRecipients(RecipientType.TO));
        final List<String> resultRecipients = resultAddresses.stream().map(a -> a.toString())
                .collect(Collectors.toList());
        assertThat(expectedRecipients, is(equalTo(resultRecipients)));
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     *
     * @param pExpected
     *            The expected email as {@code Email}
     * @param pResult
     *            The compared email as {@code MimeMessage}
     * @throws MessagingException
     *             Exception thrown by getters of {@link MimeMessage}
     */
    private void checkSMMVsMimeMessage(final SimpleMailMessage pExpected, final MimeMessage pResult)
            throws MessagingException {
        // Check subject
        assertThat("Expected and actual subjects are different", pExpected.getSubject(),
                   is(equalTo(pResult.getSubject())));
        // Check mail body
        assertThat("Expected and actual bodies are different", pExpected.getText(),
                   is(equalTo(GreenMailUtil.getBody(pResult))));
        // Check mail sender address
        assertThat("Expected and actual From are different", pExpected.getFrom(),
                   is(equalTo(pResult.getFrom()[0].toString())));
        // Check mail recipients
        final List<String> expectedRecipients = Arrays.asList(pExpected.getTo());
        final List<Address> resultAddresses = Arrays.asList(pResult.getRecipients(RecipientType.TO));
        final List<String> resultRecipients = resultAddresses.stream().map(a -> a.toString())
                .collect(Collectors.toList());
        assertThat(expectedRecipients, is(equalTo(resultRecipients)));
    }

}
