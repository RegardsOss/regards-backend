/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.dao.EmailRequestRepository;
import fr.cnes.regards.modules.emails.service.EmailRequestService;
import fr.cnes.regards.modules.notification.service.IInstanceNotificationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the email module
 *
 * @author xbrochar
 */
@RegardsTransactional
@ContextConfiguration(classes = EmailRequestConfiguration.class)
@TestPropertySource(properties = { "regards.accounts.root.user.login=rootAdminUserLoginTest" })
public class EmailRequestControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Email request service handling CRUD operations and mailing operations
     */
    @Autowired
    private EmailRequestService emailRequestService;

    @Autowired
    private EmailRequestRepository emailRequestRepository;

    @Autowired
    private IInstanceNotificationService instanceNotificationService;

    /**
     * Check that the system allows to send an email to a list of recipients.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to send an email to a list of recipients.")
    public void sendEmail() {
        performDefaultPost(EmailRequestController.EMAIL_ROOT_PATH,
                           createDummyMessage(),
                           customizer().expectStatusCreated(),
                           "Unable to send the email");
    }

    /**
     * Creates a {@link SimpleMailMessage} with some random values initialized.
     *
     * @return The mail
     */
    private SimpleMailMessage createDummyMessage() {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("sender@test.com");
        mailMessage.setSubject("subjectTest");
        mailMessage.setText("messageTest");
        mailMessage.setTo("user-test@c-s.fr");

        return mailMessage;
    }

}
