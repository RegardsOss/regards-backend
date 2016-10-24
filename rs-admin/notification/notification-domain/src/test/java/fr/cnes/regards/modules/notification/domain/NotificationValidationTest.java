/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Validate hibernate constraints on {@link Notification}.
 *
 * @author CS SI
 */
public class NotificationValidationTest {

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
        notification.setId(0L);
        notification.setDate(LocalDateTime.now().minusDays(1));
        notification.setMessage("Message");
        notification.setSender("sender@email.com");
        notification.setStatus(NotificationStatus.UNREAD);

        final List<Role> roleRecipients = new ArrayList<>();
        roleRecipients.add(new Role(0L, "name", null, new ArrayList<>(), new ArrayList<>(), false, true));
        notification.setRoleRecipients(roleRecipients);

        final List<ProjectUser> projectUserRecipients = new ArrayList<>();
        projectUserRecipients
                .add(new ProjectUser(0L, LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusMonths(1),
                        UserStatus.ACCESS_GRANTED, new ArrayList<>(), null, new ArrayList<>(), "user@email.com"));
        notification.setProjectUserRecipients(projectUserRecipients);

        // Run the validator
        final Set<ConstraintViolation<Notification>> constraintViolations = validator.validate(notification);

        // Check no constraint violations so far
        Assert.assertEquals(0, constraintViolations.size());
    }

    /**
     * Check that the system fails when a blank message is set.
     */
    @Test
    @Requirement("?")
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
    @Requirement("?")
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
    @Requirement("?")
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
    @Requirement("?")
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
    @Requirement("?")
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
