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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.service.IEmailService;

/**
 * Integration tests for the email module
 *
 * @author xbrochar
 *
 */
@MultitenantTransactional
@ContextConfiguration(classes = EmailConfiguration.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=email_it" })
public class EmailControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(EmailControllerIT.class);

    /**
     * Method authorization service.Autowired by Spring.
     */

    /**
     * Email service handling mailing operations
     */
    @Autowired
    private IEmailService emailService;

    @Autowired
    private IEmailRepository emailRepo;

    private Email testEmail;

    @Before
    public void setUp() {
        final Email email = new Email();
        email.setSubject("test");
        email.setText("test");
        email.setFrom("regards@noreply.fr");
        email.setTo(new String[] { "user@user.fr" });
        testEmail = emailRepo.save(email);
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
        performDefaultGet("/emails", expectations, "Unable to retrieve the emails list");
    }

    /**
     * Check that the system allows to send an email to a list of recipients.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() {
        final SimpleMailMessage message = createDummyMessage();
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performDefaultPost("/emails", message, expectations, "Unable to send the email");
    }

    /**
     * Check that the allows to retrieve a single email and handle fail cases.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the allows to retrieve a single email and handle fail cases.")
    public void retrieveEmail() {
        assertTrue(emailService.exists(testEmail.getId()));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet("/emails/{mail_id}", expectations, "Unable to retrieve email", testEmail.getId());

        final Long wrongId = 999L;
        assertFalse(emailService.exists(wrongId));

        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDefaultGet("/emails/{mail_id}", expectations, "Unable to retrieve email", wrongId);
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
        performDefaultPut("/emails/{mail_id}", null, expectations, "Unable to resend the email", id);

        final Long wrongId = 999L;
        assertFalse(emailService.exists(wrongId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDefaultPut("/emails/{mail_id}", null, expectations, "Unable to resend the email", wrongId);
    }

    /**
     * Check that the system allows to delete an email.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to delete an email.")
    public void deleteEmail() {
        assertTrue(emailService.exists(testEmail.getId()));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete("/emails/{mail_id}", expectations, "Unable to delete the email", testEmail.getId());
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
