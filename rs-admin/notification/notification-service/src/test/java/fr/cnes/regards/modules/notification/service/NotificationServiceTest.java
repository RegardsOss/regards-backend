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
package fr.cnes.regards.modules.notification.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;

/**
 * Test class for {@link NotificationService}.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
public class NotificationServiceTest {

    /**
     * A sender
     */
    private static final String SENDER = "Sender";

    /**
     * A role name
     */
    private static final String ROLE_NAME_0 = "Role0";

    /**
     * An other role name
     */
    private static final String ROLE_NAME_1 = "Role1";

    /**
     * A recipient
     */
    private static final String RECIPIENT_0 = "recipient0@email.com";

    /**
     * An other recipient
     */
    private static final String RECIPIENT_1 = "recipient1@email.com";

    /**
     * An other recipient
     */
    private static final String RECIPIENT_2 = "recipient2@email.com";

    /**
     * A message
     */
    private static final String MESSAGE = "Message";

    /**
     * A title
     */
    private static final String TITLE = "Title";

    private static final NotificationType TYPE = NotificationType.INFO;

    /**
     * A notification
     */
    private static Notification notification = new Notification();

    /**
     * A project user
     */
    private static ProjectUser projectUser0 = new ProjectUser();

    /**
     * An other project user
     */
    private static ProjectUser projectUser1 = new ProjectUser();

    /**
     * An other project user
     */
    private static ProjectUser projectUser2 = new ProjectUser();

    /**
     * A role
     */
    private static Role role0 = new Role();

    /**
     * An other role
     */
    private static Role role1 = new Role();

    /**
     * The tested service
     */
    private NotificationService notificationService;

    /**
     * Service handle CRUD operations on {@link ProjectUser}s. Autowired by Spring.
     */
    private IProjectUserService projectUserService;

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private INotificationRepository notificationRepository;

    /**
     * CRUD repository managing project users. Autowired by Spring.
     */
    private IProjectUserRepository projectUserRepository;

    /**
     * CRUD repository managing roles. Autowired by Spring.
     */
    private IRoleRepository roleRepository;

    /**
     * Feign client for {@link ProjectUser}s. Autowired by Spring.
     */
    private IProjectUsersClient projectUserClient;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        // Init some roles
        role0 = new Role();
        role0.setId(0L);
        role0.setName(ROLE_NAME_0);
        role0.setNative(false);
        role0.setParentRole(null);
        role0.setPermissions(new HashSet<>());

        role1 = new Role();
        role1.setId(1L);
        role1.setName(ROLE_NAME_1);
        role1.setNative(false);
        role1.setParentRole(null);
        role1.setPermissions(new HashSet<>());

        // Init some users
        projectUser0.setId(0L);
        projectUser0.setEmail(RECIPIENT_0);
        projectUser0.setLastConnection(OffsetDateTime.now().minusDays(2));
        projectUser0.setLastUpdate(OffsetDateTime.now().minusHours(1));
        projectUser0.setMetadata(new ArrayList<>());
        projectUser0.setPermissions(new ArrayList<>());
        projectUser0.setStatus(UserStatus.ACCESS_GRANTED);
        projectUser0.setRole(role0);

        projectUser1.setId(1L);
        projectUser1.setEmail(RECIPIENT_1);
        projectUser1.setLastConnection(OffsetDateTime.now().minusDays(2));
        projectUser1.setLastUpdate(OffsetDateTime.now().minusHours(1));
        projectUser1.setMetadata(new ArrayList<>());
        projectUser1.setPermissions(new ArrayList<>());
        projectUser1.setStatus(UserStatus.ACCESS_GRANTED);
        projectUser1.setRole(role0);

        projectUser2.setId(2L);
        projectUser2.setEmail(RECIPIENT_2);
        projectUser2.setLastConnection(OffsetDateTime.now().minusDays(2));
        projectUser2.setLastUpdate(OffsetDateTime.now().minusHours(1));
        projectUser2.setMetadata(new ArrayList<>());
        projectUser2.setPermissions(new ArrayList<>());
        projectUser2.setStatus(UserStatus.ACCESS_GRANTED);
        projectUser2.setRole(role1);

        // Init the notification
        notification.setId(0L);
        notification.setMessage(MESSAGE);
        notification.setDate(OffsetDateTime.now().minusHours(2L));
        notification.setSender(SENDER);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setTitle(TITLE);
        notification.setType(TYPE);
        notification.setProjectUserRecipients(new ArrayList<>());
        notification.getProjectUserRecipients().add(projectUser1);
        notification.setRoleRecipients(new ArrayList<>());
        notification.getRoleRecipients().add(role1);

        // Mock services
        projectUserService = Mockito.mock(IProjectUserService.class);
        notificationRepository = Mockito.mock(INotificationRepository.class);
        projectUserRepository = Mockito.mock(IProjectUserRepository.class);
        roleRepository = Mockito.mock(IRoleRepository.class);
        projectUserClient = Mockito.mock(IProjectUsersClient.class);

        // Instanciate the tested service
        notificationService = new NotificationService(projectUserService, notificationRepository, projectUserRepository,
                roleRepository, projectUserClient, Mockito.mock(ApplicationEventPublisher.class));
    }

    /**
     * Check that the system allows to retrieve all notifications.
     *
     * @throws EntityNotFoundException
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_SET_520")
    @Requirement("REGARDS_DSL_DAM_SET_530")
    @Requirement("REGARDS_DSL_DAM_SET_540")
    @Purpose("Check that the system allows to retrieve all notifications.")
    public void retrieveNotifications() throws EntityNotFoundException {
        // Define expected
        final List<Notification> expected = new ArrayList<>();
        expected.add(new Notification());

        // Mock methods
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(projectUser0);
        Mockito.when(notificationRepository.findByRecipientsContaining(projectUser0, role0)).thenReturn(expected);

        // Call tested method
        final List<Notification> actual = notificationService.retrieveNotifications();

        // Check that expected is equal to actual
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).findByRecipientsContaining(projectUser0, role0);
    }

    /**
     * Check that the system allows to create notifications in order to send to users.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_SET_520")
    @Requirement("REGARDS_DSL_CMP_ARC_150")
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system allorws to create notifications in order to send to users.")
    public void createNotification() {
        // Define input
        final NotificationDTO dto = new NotificationDTO();
        dto.setMessage(MESSAGE);
        dto.setTitle(TITLE);
        final List<String> projectUserRecipientsAsString = new ArrayList<>();
        projectUserRecipientsAsString.add(RECIPIENT_0);
        projectUserRecipientsAsString.add(RECIPIENT_1);
        dto.setProjectUserRecipients(projectUserRecipientsAsString);
        final List<String> roleRecipientsAsString = new ArrayList<>();
        roleRecipientsAsString.add(ROLE_NAME_0);
        dto.setRoleRecipients(roleRecipientsAsString);
        dto.setSender(SENDER);
        dto.setType(NotificationType.INFO);

        // Define expected
        final Notification expected = new Notification();
        expected.setMessage(MESSAGE);
        expected.setTitle(TITLE);
        expected.setSender(SENDER);
        expected.setStatus(NotificationStatus.UNREAD);
        expected.setType(NotificationType.INFO);
        final List<ProjectUser> projectUserRecipients = new ArrayList<>();
        final ProjectUser projectUserRecipient0 = new ProjectUser();
        projectUserRecipient0.setEmail(RECIPIENT_0);
        projectUserRecipients.add(projectUserRecipient0);
        final ProjectUser projectUserRecipient1 = new ProjectUser();
        projectUserRecipient0.setEmail(RECIPIENT_1);
        projectUserRecipients.add(projectUserRecipient1);
        expected.setProjectUserRecipients(projectUserRecipients);
        final List<Role> roleRecipients = new ArrayList<>();
        final Role roleRecipient0 = new Role();
        roleRecipient0.setName(ROLE_NAME_0);
        expected.setRoleRecipients(roleRecipients);

        // Mock
        Mockito.when(projectUserRepository.findByEmailIn(projectUserRecipientsAsString))
                .thenReturn(projectUserRecipients);
        Mockito.when(roleRepository.findByNameIn(roleRecipientsAsString)).thenReturn(roleRecipients);

        // Call method
        notificationService.createNotification(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(projectUserRepository).findByEmailIn(projectUserRecipientsAsString);
        Mockito.verify(roleRepository).findByNameIn(roleRecipientsAsString);
    }

    /**
     * Check that the system fails when trying to retrieve a non existing notification.
     *
     * @throws EntityNotFoundException
     *             Thrown if no entity with expected id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system fails when trying to retrieve a non existing notification.")
    public void retrieveNotificationNotFound() throws EntityNotFoundException {
        // Define expected
        final Long id = 0L;

        // Mock methods
        Mockito.when(notificationRepository.exists(id)).thenReturn(false);

        // Call tested method
        notificationService.retrieveNotification(id);
    }

    /**
     * Check that the system allows to retrieve a notification.
     *
     * @throws EntityNotFoundException
     *             Thrown if no entity with expected id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_SET_520")
    @Requirement("REGARDS_DSL_DAM_SET_530")
    @Requirement("REGARDS_DSL_DAM_SET_540")
    @Purpose("Check that the system allows to retrieve a notification.")
    public void retrieveNotification() throws EntityNotFoundException {
        // Define expected
        final Long id = 0L;
        final Notification expected = new Notification();
        expected.setId(id);

        // Mock methods
        Mockito.when(notificationRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationRepository.findOne(id)).thenReturn(expected);

        // Call tested method
        final Notification actual = notificationService.retrieveNotification(id);

        // Check that expected is equel to acutal
        checkFieldsEqual(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).findOne(id);
    }

    /**
     * Check that the system allows to update the parameter generating or not a notification.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Notification} with passed id could not be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update the parameter generating or not a notification.")
    public void updateNotificationStatus() throws EntityNotFoundException {
        // Define expected
        final Long id = 0L;
        final Notification initial = new Notification();
        initial.setId(id);
        initial.setStatus(NotificationStatus.UNREAD);

        // Mock the repository returned value
        Mockito.when(notificationRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationRepository.findOne(id)).thenReturn(initial);

        // Perform the update
        notificationService.updateNotificationStatus(id, NotificationStatus.READ);

        // Define expected
        final Notification expected = new Notification();
        expected.setId(id);
        expected.setStatus(NotificationStatus.READ);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system fails when trying to update a non existing notification.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Notification} with passed id could not be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system fails when trying to update a non existing notification.")
    public void updateNotificationStatusNotFound() throws EntityNotFoundException {
        final Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.exists(id)).thenReturn(false);

        // Perform the update
        notificationService.updateNotificationStatus(id, NotificationStatus.READ);
    }

    /**
     * Check that the system fails when trying to delete a non existing notification.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Notification} with passed id could not be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system fails when trying to delete a non existing notification.")
    public void deleteNotificationNotFound() throws EntityNotFoundException {
        final Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.exists(id)).thenReturn(false);

        // Perform the update
        notificationService.deleteNotification(id);
    }

    /**
     * Check that the system allorws to delete a notification.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Notification} with passed id could not be found
     */
    @Test
    @Purpose("Check that the system allows to delete a notification.")
    public void deleteNotification() throws EntityNotFoundException {
        // Define a notif
        final Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.exists(id)).thenReturn(true);

        // Perform the update
        notificationService.deleteNotification(id);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).delete(id);
    }

    /**
     * Check that the system allorws to retrieve only notifications which should be sent.
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_150")
    @Purpose("Check that the system allows to retrieve only notifications which should be sent.")
    public void retrieveNotificationsToSend() {
        // Define expected
        final List<Notification> expected = new ArrayList<>();
        expected.add(new Notification());
        expected.get(0).setStatus(NotificationStatus.UNREAD);

        // Mock the repository returned value
        Mockito.when(notificationRepository.findByStatus(NotificationStatus.UNREAD)).thenReturn(expected);

        // Perform the update
        final List<Notification> actual = notificationService.retrieveNotificationsToSend();

        // Check expected equals actual
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).findByStatus(NotificationStatus.UNREAD);
    }

    /**
     * Check that the system properly aggregates the list of notification recipients.
     *
     * @throws EntityNotFoundException
     *             Thrown when no role with passed id could be found
     */
    @Test
    @Purpose("Check that the system properly aggregates the list of notification recipients.")
    public void findRecipients() throws EntityNotFoundException {
        // Define expected
        final List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser0); // Expected from the notif roleRecipients attribute
        expected.add(projectUser1); // Expected from the notif projectUserRecipients attribute
        expected.add(projectUser2); // Expected from the notif roleRecipients attribute via parent role

        // Mock
        PagedResources<Resource<ProjectUser>> expectedFromClient = HateoasUtils.wrapToPagedResources(expected);
        Mockito.when(projectUserClient.retrieveRoleProjectUserList(Mockito.anyLong(), Mockito.anyInt(),
                                                                   Mockito.anyInt()))
                .thenReturn(new ResponseEntity<>(expectedFromClient, HttpStatus.OK));
        // Result
        final List<ProjectUser> actual = notificationService.findRecipients(notification).collect(Collectors.toList());

        // Compare
        Assert.assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
    }

    /**
     * Check that the passed {@link Role} has same attributes as the passed {@link Role}.
     *
     * @param pExpected
     *            The expected role0
     * @param pActual
     *            The actual role0
     */
    private void checkFieldsEqual(final Notification pExpected, final Notification pActual) {
        Assert.assertThat(pActual.getId(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getId())));
        Assert.assertThat(pActual.getMessage(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getMessage())));
        Assert.assertThat(pActual.getSender(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getSender())));
        Assert.assertThat(pActual.getTitle(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getTitle())));
        // Do not compare the date
        // Assert.assertThat(pActual.getDate(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getDate())));
        Assert.assertThat(pActual.getProjectUserRecipients(),
                          CoreMatchers.is(CoreMatchers.equalTo(pExpected.getProjectUserRecipients())));
        Assert.assertThat(pActual.getRoleRecipients(),
                          CoreMatchers.is(CoreMatchers.equalTo(pExpected.getRoleRecipients())));
        Assert.assertThat(pActual.getStatus(), CoreMatchers.is(CoreMatchers.equalTo(pExpected.getStatus())));

    }
}
