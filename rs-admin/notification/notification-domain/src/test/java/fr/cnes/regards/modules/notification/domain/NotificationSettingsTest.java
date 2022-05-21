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

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Validate getters/setters and hibernate constraints on {@link Notification}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationSettingsTest {

    /**
     * Id
     */
    private static final Long ID = 0L;

    /**
     * Days
     */
    private static final Integer DAYS = 1;

    /**
     * Hours
     */
    private static final Integer HOURS = 2;

    /**
     * Frequency
     */
    private static final NotificationFrequency FREQUENCY = NotificationFrequency.MONTHLY;

    /**
     * User
     */
    private static String user;

    /**
     * Self expl
     */
    private static final Integer TWENTY_FIVE = 25;

    /**
     * Javax validator
     */
    private static Validator validator;

    /**
     * The tested settings
     */
    private NotificationSettings settings;

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
    public void setUpNotification() {
        user = "user@email.com";

        settings = new NotificationSettings();
        settings.setId(ID);
        settings.setDays(DAYS);
        settings.setHours(HOURS);
        settings.setFrequency(FREQUENCY);
        settings.setProjectUserEmail(user);

        // Run the validator
        final Set<ConstraintViolation<NotificationSettings>> constraintViolations = validator.validate(settings);

        // Check no constraint violations so far
        Assert.assertEquals(0, constraintViolations.size());
    }

    /**
     * Check the POJO getters/setters.
     */
    @Test
    @Requirement("?")
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertEquals(settings.getId(), ID);
        Assert.assertEquals(settings.getDays(), DAYS);
        Assert.assertEquals(settings.getHours(), HOURS);
        Assert.assertEquals(settings.getFrequency(), FREQUENCY);
        Assert.assertEquals(settings.getProjectUserEmail(), user);
    }

    /**
     * Check that the system fails when days frequency is lower than 1.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system fails when days frequency is lower than 1.")
    public void daysIsLowerThan1() {
        // Init the malformed object
        settings.setDays(0);

        // Run the validator
        final Set<ConstraintViolation<NotificationSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when days frequency is higher than 24.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system fails when hours frequency is higher than 24.")
    public void hoursIsLowerThan1() {
        // Init the malformed object
        settings.setHours(TWENTY_FIVE);

        // Run the validator
        final Set<ConstraintViolation<NotificationSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system fails when user is null.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system fails when user is null.")
    public void userIsNull() {
        // Init the malformed object
        settings.setProjectUserEmail(null);

        // Run the validator
        final Set<ConstraintViolation<NotificationSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

}
