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

import java.io.IOException;
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

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.EmailDTO;

/**
 * Test class for {@link EmailService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class EmailServiceTest {

    /**
     * The tested service
     */
    private IEmailService emailService_;

    /**
     * Mock repository
     */
    private IEmailRepository emailRepository_;

    /**
     * Interface defining a strategy for sending mails
     */
    private JavaMailSenderImpl mailSender_;

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
    public final GreenMailRule greenMail_ = new GreenMailRule(ServerSetupTest.SMTP);

    @Before
    public void setUp() {
        emailRepository_ = mock(IEmailRepository.class);

        mailSender_ = new JavaMailSenderImpl();

        // Inject the SMTP session from GreenMail server to the JavaMailSender
        Session smtpSession = greenMail_.getSmtp().createSession();
        mailSender_.setSession(smtpSession);

        emailService_ = new EmailService(emailRepository_, mailSender_);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve the list of sent emails.")
    public void retrieveEmails() throws IOException, MessagingException {
        // Create a few mime messages
        SimpleMailMessage mail0 = createDummySimpleMailMessage();
        SimpleMailMessage mail1 = createDummySimpleMailMessage();

        // Mock the method wich will be called
        List<SimpleMailMessage> expected = new ArrayList<>();
        expected.add(mail0);
        expected.add(mail1);
        List<EmailDTO> emailDTOs = new ArrayList<>();
        emailDTOs.add(new EmailDTO(0L, expected.get(0)));
        emailDTOs.add(new EmailDTO(1L, expected.get(1)));
        when(emailRepository_.findAll()).thenReturn(emailDTOs);

        // The service returns what is returned by the repository
        List<SimpleMailMessage> result = emailService_.retrieveEmails();
        assertThat("Received list is same size as expected", result.size(), equalTo(expected.size()));

        // Check received mails are same as expected
        for (int i = 0; i < expected.size(); i++) {
            checkMails(expected.get(i), result.get(i));
        }

        // Check that the repository's method was called with right arguments
        verify(emailRepository_).findAll();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() throws MessagingException, IOException {
        // Create dummy mime message with randow subject and content
        SimpleMailMessage expected = createDummySimpleMailMessage();

        // Create recipients
        String[] recipients = { "recipient@test.com" };

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailService_.sendEmail(recipients, expected);

        // Retrieve the mail
        MimeMessage[] emails = greenMail_.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        MimeMessage result = emails[0];
        checkMails(expected, result);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve a single email.")
    public void retrieveEmail() throws MessagingException, IOException {
        Long id = 0L;

        // Create dummy mime message with random subject and content
        SimpleMailMessage expected = createDummySimpleMailMessage();

        // Create the associated DTO object
        EmailDTO emailDTO = new EmailDTO(id, expected);

        // Mock the method wich will be called
        when(emailRepository_.findOne(0L)).thenReturn(emailDTO);

        // The service returns what is returned by the repository
        SimpleMailMessage result = emailService_.retrieveEmail(id);

        // Check that the received mail is same as expected
        checkMails(expected, result);

        // Check that the repository's method was called with right arguments
        verify(emailRepository_).findOne(0L);
    }

    @Test(expected = Exception.class)
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system handles the case where an email cannot be found when re-sending.")
    public void resendEmailNotFound() {
        Long id = 999L;
        assumeTrue("EmailDTO passed id is expected to not be found", !emailService_.exists(id));
        emailService_.resendEmail(id);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to re-send an email.")
    public void resendEmail() throws MessagingException, IOException {
        Long id = 0L;

        // Create dummy message with random subject and content and add a recipient
        SimpleMailMessage expected = createDummySimpleMailMessage();
        expected.setTo("recipient@test.com");

        // Create the associated DTO object
        EmailDTO emailDTO = new EmailDTO(id, expected);

        // Mock the method wich will be called
        when(emailRepository_.findOne(id)).thenReturn(emailDTO);

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailService_.resendEmail(id);

        // Retrieve the mail
        MimeMessage[] emails = greenMail_.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        MimeMessage result = emails[0];
        checkMails(expected, result);

        // Check that the repository's method was called with right arguments
        verify(emailRepository_).findOne(0L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to delete an email.")
    public void deleteEmail() {
        emailService_.deleteEmail(0L);

        // Check that the repository's method was called with right arguments
        verify(emailRepository_).delete(0L);
    }

    private SimpleMailMessage createDummySimpleMailMessage() throws MessagingException {
        // Create an empty message
        SimpleMailMessage message = new SimpleMailMessage();

        // With random content to avoid potential residual lingering problems
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();

        // Set content on the mail
        message.setSubject(subject);
        message.setFrom("sender@test.com");
        message.setText(body);

        return message;
    }

    /**
     * Check that the two {@link SimpleMailMessage}s are equal by comparing their subject, bodies, sender and
     * recipients.
     *
     * @param expected
     * @param result
     * @throws MessagingException
     */
    private void checkMails(final SimpleMailMessage expected, final SimpleMailMessage result)
            throws MessagingException {
        // Check subject
        assertThat("Expected and actual subjects are different", expected.getSubject(),
                   is(equalTo(result.getSubject())));
        // Check mail body
        assertThat("Expected and actual bodies are different", expected.getText(), is(equalTo(result.getText())));
        // Check mail sender address
        assertThat("Expected and actual From are different", expected.getFrom(), is(equalTo(result.getFrom())));
        // Check mail recipients
        assertThat("Expected and actual recipients are different", expected.getTo(), is(equalTo(result.getTo())));
    }

    /**
     * Check that the passed {@link SimpleMailMessage} has same subject, body and recipients as the passed
     * {@link MimeMessage}. We then consider them equal.
     *
     * @param expected
     * @param result
     * @throws MessagingException
     */

    private void checkMails(final SimpleMailMessage expected, final MimeMessage result) throws MessagingException {
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
        List<String> expectedRecipients = Arrays.asList(expected.getTo());
        List<Address> resultAddresses = Arrays.asList(result.getRecipients(RecipientType.TO));
        List<String> resultRecipients = resultAddresses.stream().map(a -> a.toString()).collect(Collectors.toList());
        assertThat(expectedRecipients, is(equalTo(resultRecipients)));
    }

}
