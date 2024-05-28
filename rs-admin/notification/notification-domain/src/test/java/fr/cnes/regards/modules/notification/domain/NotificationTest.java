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
package fr.cnes.regards.modules.notification.domain;

import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Validate getter/setters and JPA constraints on {@link Notification}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationTest {

    /**
     * Id
     */
    private static final Long ID = 0L;

    /**
     * Date
     */
    private static final OffsetDateTime DATE = OffsetDateTime.now().minusDays(1);

    /**
     * Message
     */
    private static final String MESSAGE = "Message";

    /**
     * Title
     */
    private static final String TITLE = "Title";

    /**
     * Status
     */
    private static final NotificationStatus STATUS = NotificationStatus.UNREAD;

    private static final NotificationLevel TYPE = NotificationLevel.INFO;

    /**
     * Role recipients
     */
    private static Set<String> roleRecipients;

    /**
     * User recipients
     */
    private static Set<String> projectUserRecipients;

    /**
     * Sender
     */
    private static String SENDER = "sender@email.com";

    /**
     * Javax validator
     */
    private static Validator validator;

    /**
     * The test notification
     */
    private Notification notification;

    /**
     * Set up the validator
     */
    @BeforeClass
    public static void setUpValidator() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Setup a vaid notification before each test in order to unvalidate on by one
     */
    @Before
    public void setupNotification() {
        notification = new Notification();
        notification.setId(ID);
        notification.setDate(DATE);
        notification.setTitle(TITLE);
        notification.setMessage(MESSAGE);
        notification.setSender(SENDER);
        notification.setStatus(STATUS);
        notification.setLevel(TYPE);

        roleRecipients = new HashSet<>();
        roleRecipients.add(DefaultRole.PUBLIC.toString());
        notification.setRoleRecipients(roleRecipients);

        projectUserRecipients = new HashSet<>();
        projectUserRecipients.add("user@email.com");
        notification.setProjectUserRecipients(projectUserRecipients);

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check no constraint violations so far
        Assert.assertEquals(0, constraintViolations.size());
    }

    /**
     * Check the POJO getters/setters.
     */
    @Test
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertEquals(notification.getId(), ID);
        Assert.assertEquals(notification.getMessage(), MESSAGE);
        Assert.assertEquals(notification.getSender(), SENDER);
        Assert.assertEquals(notification.getTitle(), TITLE);
        Assert.assertEquals(notification.getDate(), DATE);
        Assert.assertEquals(notification.getProjectUserRecipients(), projectUserRecipients);
        Assert.assertEquals(notification.getRoleRecipients(), roleRecipients);
        Assert.assertEquals(notification.getStatus(), STATUS);
    }

    /**
     * Check that the system fails when a blank message is set.
     */
    @Test
    @Purpose("Check that the system fails when a blank message is set.")
    public void messageIsBlank() {
        // Init the malformed object
        notification.setMessage("   ");

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when the project user recipients field is null.
     */
    @Test
    @Purpose("Check that the system fails when the project user recipients field is null.")
    public void projectUsersIsNull() {
        // Init the malformed object
        notification.setProjectUserRecipients(null);

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when the role recipients field is null.
     */
    @Test
    @Purpose("Check that the system fails when the role recipients field is null.")
    public void rolesIsNull() {
        // Init the malformed object
        notification.setRoleRecipients(null);

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when a blank sender is set.
     */
    @Test
    @Purpose("Check that the system fails when a blank sender is set.")
    public void senderIsBlank() {
        // Init the malformed object
        notification.setSender("");

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when the status field is null.
     */
    @Test
    @Purpose("Check that the system fails when the status field is null.")
    public void statusIsNull() {
        // Init the malformed object
        notification.setStatus(null);

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }
}
