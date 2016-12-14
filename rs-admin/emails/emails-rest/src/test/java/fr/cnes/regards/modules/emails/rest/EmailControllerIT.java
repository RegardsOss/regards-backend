/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.domain.Email;
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
     * The JWT string
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
        jwt = jwtService.generateToken(DEFAULT_TENANT, "email", DEFAULT_ROLE);
        authService.setAuthorities(DEFAULT_TENANT, "/emails", RequestMethod.GET, DEFAULT_ROLE);
        authService.setAuthorities(DEFAULT_TENANT, "/emails", RequestMethod.POST, DEFAULT_ROLE);
        authService.setAuthorities(DEFAULT_TENANT, "/emails/{mail_id}", RequestMethod.GET, DEFAULT_ROLE);
        authService.setAuthorities(DEFAULT_TENANT, "/emails/{mail_id}", RequestMethod.PUT, DEFAULT_ROLE);
        authService.setAuthorities(DEFAULT_TENANT, "/emails/{mail_id}", RequestMethod.DELETE, DEFAULT_ROLE);
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
        final SimpleMailMessage message = createDummyMessage();
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost("/emails", jwt, message, expectations, "Unable to send the email");
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
        // Retrieve an sent email from the db
        final Email email = emailService.retrieveEmails().get(0);
        final Long id = email.getId();

        // Then re-send it
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

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
