/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

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
     * the production SMTP.
     * </p>
     * This will allow us to retrieve the emails we sent to this server and check that they were properly sent and
     * correspond to the expected.
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
    public void retrieveEmails() {
        // Create a few mime messages
        MimeMessage mail0 = mailSender_.createMimeMessage();
        MimeMessage mail1 = mailSender_.createMimeMessage();

        // Mock the method wich will be called
        List<MimeMessage> expected = new ArrayList<>();
        expected.add(mail0);
        expected.add(mail1);
        List<EmailDTO> emailDTOs = new ArrayList<>();
        emailDTOs.add(new EmailDTO(0L, expected.get(0)));
        emailDTOs.add(new EmailDTO(0L, expected.get(1)));
        when(emailRepository_.findAll()).thenReturn(emailDTOs);

        // The service returns what is returned by the repository
        List<MimeMessage> result = emailService_.retrieveEmails();
        assertThat("Emails list is different from expected", result, equalTo(expected));

        // Check that the repository's method was called with right arguments
        verify(emailRepository_).findAll();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() throws MessagingException, IOException {
        // Create dummy mime message with randow subject and content
        MimeMessage expected = createDummyMimeMessage();

        // Create recipients
        String[] recipients = { "recipient@test.com" };

        // Send the email. It will be sent to the Green Mail SMTP server started by the @Rule GreenMailRule
        emailService_.sendEmail(recipients, expected);

        // Retrieve the mail
        MimeMessage[] emails = greenMail_.getReceivedMessages();
        assertEquals(1, emails.length);

        // Check that the received mail is the same as the sent mail
        MimeMessage result = emails[0];
        // Check subject
        assertThat("Expected and actual mail subject are different", expected.getSubject(),
                   is(equalTo(result.getSubject())));
        // Check mail body
        assertThat("Expected and actual mail body are different", GreenMailUtil.getBody(expected),
                   is(equalTo(GreenMailUtil.getBody(result))));
        // Cehck mail sender address
        assertThat("Expected and actual mail From are different", expected.getFrom(), is(equalTo(result.getFrom())));
        // Check mail recipients
        assertThat("Expected and actual mail Recipients are different", expected.getRecipients(RecipientType.TO),
                   is(equalTo(result.getRecipients(RecipientType.TO))));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve a single email.")
    public void retrieveEmail() throws MessagingException {
        // Create dummy mime message with random subject and content
        MimeMessage expected = createDummyMimeMessage();

        // Create the associated DTO object
        EmailDTO emailDTO = new EmailDTO(0L, expected);

        // Mock the method wich will be called
        when(emailRepository_.findOne(0L)).thenReturn(emailDTO);

        // The service returns what is returned by the repository
        MimeMessage result = emailService_.retrieveEmail(0L);
        assertThat("EmailDTO is different from expected", result, is(sameInstance(expected)));

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
    public void resendEmail() throws MessagingException {
        Long id = 0L;

        // Create dummy mime message with random subject and content and add a recipient
        MimeMessage expected = createDummyMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(expected);
        helper.setTo("recipient@test.com");

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
        // Check subject
        assertThat("Expected and actual mail subject are different", expected.getSubject(),
                   is(equalTo(result.getSubject())));
        // Check mail body
        assertThat("Expected and actual mail body are different", GreenMailUtil.getBody(expected),
                   is(equalTo(GreenMailUtil.getBody(result))));
        // Cehck mail sender address
        assertThat("Expected and actual mail From are different", expected.getFrom(), is(equalTo(result.getFrom())));
        // Check mail recipients
        assertThat("Expected and actual mail Recipients are different", expected.getRecipients(RecipientType.TO),
                   is(equalTo(result.getRecipients(RecipientType.TO))));

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

    private MimeMessage createDummyMimeMessage() throws MessagingException {
        // Create an empty mime message
        MimeMessage mimeMessage = mailSender_.createMimeMessage();

        // With random content to avoid potential residual lingering problems
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();

        // Set content on the mail
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setSubject(subject);
        helper.setFrom("toto@toto.com");
        helper.setText(body);

        return mimeMessage;
    }

}
