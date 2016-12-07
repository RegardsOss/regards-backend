/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Validate getter/setters on {@link NotificationDTO}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationDTOTest {

    /**
     * Message
     */
    private static final String MESSAGE = "Message";

    /**
     * Title
     */
    private static final String TITLE = "Title";

    /**
     * Title
     */
    private static final String SENDER = "send@email.com";

    /**
     * A role name
     */
    private static final String ROLE_0 = "Role0";

    /**
     * An other role name
     */
    private static final String ROLE_1 = "Role1";

    /**
     * Role recipients
     */
    private static List<String> roleRecipients;

    /**
     * A user email
     */
    private static final String USER_0 = "user0@email.com";

    /**
     * An other user email
     */
    private static final String USER_1 = "user1@email.com";

    /**
     * User recipients
     */
    private static List<String> projectUserRecipients;

    /**
     * Tested pojo
     */
    private NotificationDTO dto;

    /**
     * Setup a vaid notification before each test in order to unvalidate on by one
     */
    @Before
    public void setupNotification() {
        dto = new NotificationDTO();
        dto.setSender(SENDER);
        dto.setTitle(TITLE);
        dto.setMessage(MESSAGE);

        roleRecipients = new ArrayList<>();
        roleRecipients.add(ROLE_0);
        roleRecipients.add(ROLE_1);
        dto.setRoleRecipients(roleRecipients);

        projectUserRecipients = new ArrayList<>();
        projectUserRecipients.add(USER_0);
        projectUserRecipients.add(USER_1);
        dto.setProjectUserRecipients(projectUserRecipients);
    }

    /**
     * Check the POJO getters/setters.
     */
    @Test
    @Requirement("?")
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertTrue(dto.getMessage().equals(MESSAGE));
        Assert.assertTrue(dto.getSender().equals(SENDER));
        Assert.assertTrue(dto.getTitle().equals(TITLE));
        Assert.assertTrue(dto.getProjectUserRecipients().equals(projectUserRecipients));
        Assert.assertTrue(dto.getRoleRecipients().equals(roleRecipients));
    }

}
