/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import com.icegreen.greenmail.util.GreenMailUtil;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO;
import fr.cnes.regards.modules.emails.domain.Recipient;
import fr.cnes.regards.modules.emails.service.IEmailService;

/**
 * Integration tests for the email module
 *
 * @author xbrochar
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ImportResource({ "classpath*:mailSender.xml" })
public class EmailControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(EmailControllerIT.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Method authorization service.Autowired by Spring.
     */
    @Autowired
    private MethodAuthorizationService authService;

    /**
     * The jwt string
     */
    private String jwt;

    /**
     * Email service handling mailing operations
     */
    @Autowired
    private IEmailService emailService;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        final String tenant = "test-1";
        jwt = jwtService.generateToken(tenant, "email", "SVG", "USER");
        authService.setAuthorities(tenant, "/emails", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/emails", RequestMethod.POST, "USER");
        authService.setAuthorities(tenant, "/emails/{mail_id}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/emails/{mail_id}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/emails/{mail_id}", RequestMethod.DELETE, "USER");
    }

    /**
     * Check that the system allows to retrieve the list of sent emails.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve the list of sent emails.")
    public void retrieveEmails() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet("/emails", jwt, expectations, "Unable to retrieve the emails list");
    }

    /**
     * Check that the system allows to send an email to a list of recipients.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() {
        // Create dummy email
        final Email email = createDummyEmail();
        email.setId(3543L);
        // Create recipients
        final Set<Recipient> recipients = new HashSet<>();
        final Recipient recipient = new Recipient();
        recipient.setAddress("xavier-alexandre.brochard@c-s.fr");
        recipients.add(recipient);

        // Generate the associated dto
        final EmailWithRecipientsDTO dto = new EmailWithRecipientsDTO();
        dto.setRecipients(recipients);
        dto.setEmail(email);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost("/emails", jwt, dto, expectations, "Unable to send the email");
    }

    /**
     * Check that the allows to retrieve a single email and handle fail cases.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the allows to retrieve a single email and handle fail cases.")
    public void retrieveEmail() {
        final Long id = 0L;
        assertTrue(emailService.exists(id));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet("/emails/{mail_id}", jwt, expectations, "Unable to retrieve email", id);

        final Long wrongId = 999L;
        assertFalse(emailService.exists(wrongId));

        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet("/emails/{mail_id}", jwt, expectations, "Unable to retrieve email", wrongId);
    }

    /**
     * Check that the system allows to re-send an email and handles fail cases.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to re-send an email and handles fail cases.")
    public void resendEmail() {
        final Long id = 555l;

        // First send an email to ensure it was sent
        final Email email = createDummyEmail();
        email.setId(id);
        // Create recipients
        final Set<Recipient> recipients = new HashSet<>();
        final Recipient recipient = new Recipient();
        recipient.setAddress("xavier-alexandre.brochard@c-s.fr");
        recipients.add(recipient);

        // Generate the associated dto
        final EmailWithRecipientsDTO dto = new EmailWithRecipientsDTO();
        dto.setRecipients(recipients);
        dto.setEmail(email);

        emailService.sendEmail(recipients, email);

        // Then re-send it
        assertTrue(emailService.exists(id));
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut("/emails/{mail_id}", jwt, null, expectations, "Unable to resend the email", id);

        final Long wrongId = 999L;
        assertFalse(emailService.exists(wrongId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performPut("/emails/{mail_id}", jwt, null, expectations, "Unable to resend the email", wrongId);
    }

    /**
     * Check that the system allows to delete an email.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to delete an email.")
    public void deleteEmail() {
        final Long id = 0L;
        assertTrue(emailService.exists(id));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete("/emails/{mail_id}", jwt, expectations, "Unable to delete the email", id);
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
        final String body = GreenMailUtil.random();

        // Set content on the mail
        email.setSubject(subject);
        email.setFrom("sender@test.com");
        email.setText(body);

        return email;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
