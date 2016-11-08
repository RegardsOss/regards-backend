/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;

/**
 * Test class for {@link SendingScheduler}.
 *
 * @author xbrochar
 */
public class SendingSchedulerTest {

    /**
     * Constant
     */
    private static final Long ZERO = 0L;

    /**
     * Constant
     */
    private static final Long ONE = 1L;

    /**
     * Constant
     */
    private static final Long TWO = 2L;

    /**
     * Constant
     */
    private static final Long THREE = 3L;

    /**
     * Constant
     */
    private static final Long FOUR = 4L;

    /**
     * Constant
     */
    private static final Integer FOUR_INT = 4;

    /**
     * An email
     */
    private static final String EMAIL0 = "email0@test.com";

    /**
     * An other email
     */
    private static final String EMAIL1 = "email1@test.com";

    /**
     * An other email
     */
    private static final String EMAIL2 = "email2@test.com";

    /**
     * An other email
     */
    private static final String EMAIL3 = "email3@test.com";

    /**
     * An other email
     */
    private static final String EMAIL4 = "email4@test.com";

    /**
     * A message
     */
    private static final String MESSAGE = "message";

    /**
     * The list of all notifications
     */
    private List<Notification> notifications;

    /**
     * The list of all project users
     */
    private List<ProjectUser> projectUsers;

    /**
     * The list of all roles
     */
    private List<Role> roles;

    /**
     * The list of all notification settings
     */
    private List<NotificationSettings> settings;

    /**
     * Tested class
     */
    private SendingScheduler scheduler;

    /**
     * Sending strategy
     */
    private ISendingStrategy strategy;

    /**
     * The service responsible for managing notifications
     */
    private INotificationService notificationService;

    /**
     * The service responsible for managing notification settings
     */
    private INotificationSettingsRepository notificationSettingsRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        // Populate first batch of recipients
        final ProjectUser projectUser0 = new ProjectUser(EMAIL0, null, null, null);
        final ProjectUser projectUser1 = new ProjectUser(EMAIL1, null, null, null);
        final ProjectUser projectUser2 = new ProjectUser(EMAIL2, null, null, null);
        final ProjectUser projectUser3 = new ProjectUser(EMAIL3, null, null, null);
        final ProjectUser projectUser4 = new ProjectUser(EMAIL4, null, null, null);
        projectUsers = new ArrayList<>();
        projectUsers.add(projectUser0);
        projectUsers.add(projectUser1);
        projectUsers.add(projectUser2);
        projectUsers.add(projectUser3);
        projectUsers.add(projectUser4);

        // Roles
        final Role role0 = new Role(ZERO, "Role0", null, null, new ArrayList<>());
        final Role role1 = new Role(ONE, "Role1", role0, null, new ArrayList<>());
        final Role role2 = new Role(TWO, "Role2", role1, null, new ArrayList<>());
        roles = new ArrayList<>();
        roles.add(role0);
        roles.add(role1);
        roles.add(role2);

        // Link users and roles
        projectUser0.setRole(role0);
        role0.getProjectUsers().add(projectUser0);
        projectUser1.setRole(role1);
        role1.getProjectUsers().add(projectUser1);
        projectUser2.setRole(role2);
        role2.getProjectUsers().add(projectUser2);
        projectUser3.setRole(role1);
        role1.getProjectUsers().add(projectUser3);

        // Settings
        settings = new ArrayList<>();
        final NotificationSettings settings0 = new NotificationSettings();
        settings0.setId(ZERO);
        settings0.setDays(1);
        settings0.setHours(2);
        settings0.setFrequency(NotificationFrequency.DAILY);
        settings0.setProjectUser(projectUser0);
        settings.add(settings0);
        final NotificationSettings settings1 = new NotificationSettings();
        settings1.setId(ONE);
        settings1.setDays(2);
        settings1.setHours(2);
        settings1.setFrequency(NotificationFrequency.DAILY);
        settings1.setProjectUser(projectUser1);
        settings.add(settings1);
        final NotificationSettings settings2 = new NotificationSettings();
        settings2.setId(TWO);
        settings2.setDays(2);
        settings2.setHours(2);
        settings2.setFrequency(NotificationFrequency.WEEKLY);
        settings2.setProjectUser(projectUser2);
        settings.add(settings2);
        final NotificationSettings settings3 = new NotificationSettings();
        settings3.setId(THREE);
        settings3.setDays(2);
        settings3.setHours(2);
        settings3.setFrequency(NotificationFrequency.MONTHLY);
        settings3.setProjectUser(projectUser3);
        settings.add(settings3);
        final NotificationSettings settings4 = new NotificationSettings();
        settings4.setId(FOUR);
        settings4.setDays(2);
        settings4.setHours(2);
        settings4.setFrequency(NotificationFrequency.CUSTOM);
        settings4.setProjectUser(projectUser4);
        settings.add(settings4);

        // Populate notifications
        notifications = new ArrayList<>();
        final Notification notification0 = new Notification();
        notification0.setId(ZERO);
        notification0.setDate(LocalDateTime.now().minusDays(FOUR));
        notification0.setMessage("Notification 0 message");
        notification0.setProjectUserRecipients(projectUsers.subList(0, 2));
        notification0.setRoleRecipients(roles.subList(1, 2));
        notifications.add(notification0);
        final Notification notification1 = new Notification();
        notification1.setId(ONE);
        notification1.setDate(LocalDateTime.now().minusHours(THREE));
        notification1.setMessage("Notification 1 message");
        notification1.setProjectUserRecipients(projectUsers.subList(1, 2));
        notification1.setRoleRecipients(new ArrayList<>());
        notifications.add(notification1);

        // Mock strategy
        strategy = Mockito.mock(ISendingStrategy.class);

        // Mock notif service
        notificationService = Mockito.mock(INotificationService.class);
        Mockito.when(notificationService.retrieveNotificationsToSend()).thenReturn(notifications);
        final List<ProjectUser> recipientsNotif0 = new ArrayList<>();
        recipientsNotif0.add(projectUser0); // From field projectUserRecipients
        recipientsNotif0.add(projectUser1); // From field projectUserRecipients
        recipientsNotif0.add(projectUser3); // From field roleRecipients
        Mockito.when(notificationService.findRecipients(notifications.get(0)))
                .thenReturn(recipientsNotif0.parallelStream());
        final List<ProjectUser> recipientsNotif1 = new ArrayList<>();
        recipientsNotif1.add(projectUser1); // From field projectUserRecipients
        Mockito.when(notificationService.findRecipients(notifications.get(1)))
                .thenReturn(recipientsNotif1.parallelStream());

        // Mock notif settings repo
        notificationSettingsRepository = Mockito.mock(INotificationSettingsRepository.class);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(projectUser0)).thenReturn(settings0);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(projectUser1)).thenReturn(settings1);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(projectUser2)).thenReturn(settings2);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(projectUser3)).thenReturn(settings3);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(projectUser4)).thenReturn(settings4);

        // Instanciate the tested class
        scheduler = new SendingScheduler(strategy, notificationService, notificationSettingsRepository);
    }

    /**
     * Check that the system allows to use different sending strategies.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to use different sending strategies.")
    public void testChangeStrategy() {
        // Define a new strategy
        final ISendingStrategy newStrategy = Mockito.mock(ISendingStrategy.class);

        scheduler.changeStrategy(newStrategy);

        // Check with new strategy
        scheduler.sendDaily();
        Mockito.verify(newStrategy, Mockito.times(2)).send(Mockito.anyObject(), Mockito.anyObject());
    }

    /**
     * Check that the system sends the right notifications daily.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system sends the right notifications daily.")
    public void testSendDaily() {
        // Define the list of recipients for notif0 with daily settings
        final String[] recipients0 = { EMAIL0, EMAIL1 };

        // Define the list of recipients for notif1 with daily settings
        final String[] recipients1 = { EMAIL1 };

        // Call the tested metyhod
        scheduler.sendDaily();

        // Verify method call
        Mockito.verify(strategy).send(notifications.get(0), recipients0);
        Mockito.verify(strategy).send(notifications.get(1), recipients1);
        Mockito.verify(strategy, Mockito.times(2)).send(Mockito.anyObject(), Mockito.anyObject());
    }

    /**
     * Check that the system sends the right notifications weekly.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system sends the right notifications weekly.")
    public void testSendWeekly() {
        // Call the tested metyhod
        scheduler.sendWeekly();

        // Verify method call. Here no notification has a recipients with weekly frequency configured
        Mockito.verify(strategy, Mockito.never()).send(Mockito.anyObject(), Mockito.anyObject());
    }

    /**
     * Check that the system sends the right notifications monthly.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system sends the right notifications monthly.")
    public void testSendMonthly() {
        // Define the list of recipients for notif0 with monthly settings
        final List<ProjectUser> recipients = projectUsers.subList(3, 4);

        // Call the tested metyhod
        scheduler.sendMonthly();

        // Verify method call
        Mockito.verify(strategy).send(notifications.get(0),
                                      recipients.stream().map(r -> r.getEmail()).toArray(s -> new String[s]));
        Mockito.verify(strategy, Mockito.times(1)).send(Mockito.anyObject(), Mockito.anyObject());
    }

    /**
     * Check that the system sends the right notifications monthly.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system sends the right notifications with custom frequency.")
    public void testSendCustomDateExpired() {
        final Notification n = new Notification();
        n.setId(ONE);
        n.setDate(LocalDateTime.now().minusDays(THREE));
        n.setMessage(MESSAGE);
        n.setProjectUserRecipients(new ArrayList<>());
        n.getProjectUserRecipients().add(projectUsers.get(0));
        n.getProjectUserRecipients().add(projectUsers.get(2));
        n.getProjectUserRecipients().add(projectUsers.get(FOUR_INT));
        n.setRoleRecipients(new ArrayList<>());

        final List<Notification> toSend = new ArrayList<>();
        toSend.add(n);

        // Define the list of recipients for notif0 with monthly settings
        final List<ProjectUser> recipients = projectUsers.subList(4, 5);

        // Mock
        Mockito.when(notificationService.retrieveNotificationsToSend()).thenReturn(toSend);
        Mockito.when(notificationService.findRecipients(n)).thenReturn(recipients.stream());

        // Call the tested metyhod
        scheduler.sendCustom();

        // Verify method call
        Mockito.verify(strategy).send(n, recipients.stream().map(r -> r.getEmail()).toArray(s -> new String[s]));
        Mockito.verify(strategy, Mockito.times(1)).send(Mockito.anyObject(), Mockito.anyObject());
    }

    /**
     * Check that the system sends the right notifications monthly.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system sends the right notifications with custom frequency.")
    public void testSendCustomDateNotExpired() {
        final Notification n = new Notification();
        n.setId(ONE);
        n.setDate(LocalDateTime.now().minusHours(2));
        n.setMessage(MESSAGE);
        n.setProjectUserRecipients(new ArrayList<>());
        n.getProjectUserRecipients().add(projectUsers.get(0));
        n.getProjectUserRecipients().add(projectUsers.get(2));
        n.getProjectUserRecipients().add(projectUsers.get(FOUR_INT));
        n.setRoleRecipients(new ArrayList<>());

        final List<Notification> toSend = new ArrayList<>();
        toSend.add(n);

        // Define the list of recipients for notif with custom settings
        final List<ProjectUser> recipients = projectUsers.subList(4, 5);

        // Mock
        Mockito.when(notificationService.retrieveNotificationsToSend()).thenReturn(toSend);
        Mockito.when(notificationService.findRecipients(n)).thenReturn(recipients.stream());

        // Call the tested metyhod
        scheduler.sendCustom();

        // Verify method call. We expect no recipient (as the resend delay is not passed)
        Mockito.verify(strategy, Mockito.never()).send(Mockito.anyObject(), Mockito.anyObject());
    }

}
