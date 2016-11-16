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

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.projects.DefaultRoleNames;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Validate getter/setters and hibernate constraints on {@link Notification}.
 *
 * @author CS SI
 */
public class NotificationTest {

    /**
     * Id
     */
    private static final Long ID = 0L;

    /**
     * Date
     */
    private static final LocalDateTime DATE = LocalDateTime.now().minusDays(1);

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

    /**
     * Role recipients
     */
    private static List<Role> roleRecipients;

    /**
     * User recipients
     */
    private static List<ProjectUser> projectUserRecipients;

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

        roleRecipients = new ArrayList<>();
        roleRecipients.add(new Role(DefaultRoleNames.PUBLIC.toString(), null));
        notification.setRoleRecipients(roleRecipients);

        projectUserRecipients = new ArrayList<>();
        projectUserRecipients.add(new ProjectUser("user@email.com", null, new ArrayList<>(), new ArrayList<>()));
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
    @Requirement("?")
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertTrue(notification.getId().equals(ID));
        Assert.assertTrue(notification.getMessage().equals(MESSAGE));
        Assert.assertTrue(notification.getSender().equals(SENDER));
        Assert.assertTrue(notification.getTitle().equals(TITLE));
        Assert.assertTrue(notification.getDate().equals(DATE));
        Assert.assertTrue(notification.getProjectUserRecipients().equals(projectUserRecipients));
        Assert.assertTrue(notification.getRoleRecipients().equals(roleRecipients));
        Assert.assertTrue(notification.getStatus().equals(STATUS));
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
