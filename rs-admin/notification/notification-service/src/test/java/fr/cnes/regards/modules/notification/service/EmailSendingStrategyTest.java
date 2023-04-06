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
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.domain.Notification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;

/**
 * Test class for {@link IEmailClient}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class EmailSendingStrategyTest {

    /**
     * The notification's sender
     */
    private static final String SENDER = "Sender";

    /**
     * The recipients
     */
    private static final String[] RECIPIENTS = { "recipient0", "recipient1" };

    /**
     * The message
     */
    private static final String MESSAGE = "Message";

    /**
     * Feign client from module Email
     */
    private IEmailClient emailClient;

    /**
     * Tested class
     */
    private ISendingStrategy strategy;

    /**
     * Sent notification
     */
    private Notification notification;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        // Mock
        emailClient = Mockito.mock(IEmailClient.class);

        // Instanciate the tested class
        strategy = new EmailSendingStrategy(emailClient);

        // Define the sent notification
        notification = new Notification();
        notification.setId(0L);
        notification.setMessage(MESSAGE);
        notification.setSender(SENDER);
    }

    /**
     * Check that the system allows te send notifications as email through the email feign client.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows te send notifications as email through the email feign client.")
    public void send() {
        // Given : define expected mail
        final SimpleMailMessage expectedMailMessage = new SimpleMailMessage();
        expectedMailMessage.setSubject("[" + SENDER + "]" + notification.getTitle());
        expectedMailMessage.setText(MESSAGE);
        expectedMailMessage.setTo(RECIPIENTS);

        // When : call the tested method
        strategy.send(notification, RECIPIENTS);

        // Then: verify method call.
        Mockito.verify(emailClient, Mockito.times(1)).sendEmail(Mockito.refEq(expectedMailMessage, "sentDate"));
    }

}
