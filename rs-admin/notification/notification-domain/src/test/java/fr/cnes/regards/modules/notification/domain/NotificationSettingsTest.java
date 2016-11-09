/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Validate getters/setters and hibernate constraints on {@link Notification}.
 *
 * @author CS SI
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
    private static ProjectUser user;

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
        user = new ProjectUser(0L, LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusMonths(1),
                UserStatus.ACCESS_GRANTED, new ArrayList<>(), null, new ArrayList<>(), "user@email.com");

        settings = new NotificationSettings();
        settings.setId(ID);
        settings.setDays(DAYS);
        settings.setHours(HOURS);
        settings.setFrequency(FREQUENCY);
        settings.setProjectUser(user);

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
        Assert.assertTrue(settings.getId().equals(ID));
        Assert.assertTrue(settings.getDays().equals(DAYS));
        Assert.assertTrue(settings.getHours().equals(HOURS));
        Assert.assertTrue(settings.getFrequency().equals(FREQUENCY));
        Assert.assertTrue(settings.getProjectUser().equals(user));
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
        settings.setProjectUser(null);

        // Run the validator
        final Set<ConstraintViolation<NotificationSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

}
