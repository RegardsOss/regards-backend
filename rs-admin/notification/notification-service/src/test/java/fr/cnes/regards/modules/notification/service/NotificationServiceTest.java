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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.NotificationLightCustomNativeQueryRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationMode;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final NotificationLevel TYPE = NotificationLevel.INFO;

    /**
     * A notification
     */
    private static final Notification notification = new Notification();

    /**
     * A project user
     */
    private static final ProjectUser projectUser0 = new ProjectUser();

    /**
     * An other project user
     */
    private static final ProjectUser projectUser1 = new ProjectUser();

    /**
     * An other project user
     */
    private static final ProjectUser projectUser2 = new ProjectUser();

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
    private IAuthenticationResolver authenticationResolver;

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private INotificationRepository notificationRepository;

    /**
     * Native query repository to manage notification
     */
    private NotificationLightCustomNativeQueryRepository notificationLightCustomNativeQueryRepository;

    /**
     * CRUD repository managing roles. Autowired by Spring.
     */
    private IRoleService roleService;

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
        projectUser0.setMetadata(new HashSet<>());
        projectUser0.setPermissions(new ArrayList<>());
        projectUser0.setStatus(UserStatus.ACCESS_GRANTED);
        projectUser0.setRole(role0);

        projectUser1.setId(1L);
        projectUser1.setEmail(RECIPIENT_1);
        projectUser1.setLastConnection(OffsetDateTime.now().minusDays(2));
        projectUser1.setLastUpdate(OffsetDateTime.now().minusHours(1));
        projectUser1.setMetadata(new HashSet<>());
        projectUser1.setPermissions(new ArrayList<>());
        projectUser1.setStatus(UserStatus.ACCESS_GRANTED);
        projectUser1.setRole(role0);

        projectUser2.setId(2L);
        projectUser2.setEmail(RECIPIENT_2);
        projectUser2.setLastConnection(OffsetDateTime.now().minusDays(2));
        projectUser2.setLastUpdate(OffsetDateTime.now().minusHours(1));
        projectUser2.setMetadata(new HashSet<>());
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
        notification.setLevel(TYPE);
        notification.setProjectUserRecipients(Sets.newHashSet(RECIPIENT_1));
        notification.setRoleRecipients(Sets.newHashSet(ROLE_NAME_1));

        // Mock services
        authenticationResolver = Mockito.mock(IAuthenticationResolver.class);
        notificationRepository = Mockito.mock(INotificationRepository.class);
        roleService = Mockito.mock(IRoleService.class);

        // Instanciate the tested service
        notificationService = new NotificationService(notificationRepository,
                                                      roleService,
                                                      Mockito.mock(ApplicationEventPublisher.class),
                                                      authenticationResolver,
                                                      notificationLightCustomNativeQueryRepository,
                                                      NotificationMode.MULTITENANT);
    }

    /**
     * Check that the system allows to create notifications in order to send to users.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_SET_520")
    @Requirement("REGARDS_DSL_CMP_ARC_150")
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system allorws to create notifications in order to send to users.")
    public void createNotification() throws EntityNotFoundException {
        // Define input
        NotificationDTO dto = new NotificationDTO();
        dto.setMessage(MESSAGE);
        dto.setTitle(TITLE);
        Set<String> projectUserRecipientsAsString = new HashSet<>();
        projectUserRecipientsAsString.add(RECIPIENT_0);
        projectUserRecipientsAsString.add(RECIPIENT_1);
        dto.setProjectUserRecipients(projectUserRecipientsAsString);
        Set<String> roleRecipientsAsString = new HashSet<>();
        roleRecipientsAsString.add(ROLE_NAME_0);
        dto.setRoleRecipients(roleRecipientsAsString);
        dto.setSender(SENDER);
        dto.setLevel(NotificationLevel.INFO);

        Mockito.when(roleService.getDescendants(roleService.retrieveRole(ROLE_NAME_0)))
               .thenReturn(Sets.newHashSet(new Role(ROLE_NAME_0)));

        // Define expected
        Notification expected = new Notification();
        expected.setMessage(MESSAGE);
        expected.setTitle(TITLE);
        expected.setSender(SENDER);
        expected.setStatus(NotificationStatus.UNREAD);
        expected.setLevel(NotificationLevel.INFO);
        Set<String> projectUserRecipients = new HashSet<>();
        projectUserRecipients.add(RECIPIENT_0);
        projectUserRecipients.add(RECIPIENT_1);
        expected.setProjectUserRecipients(projectUserRecipients);
        Set<String> roleRecipients = new HashSet<>();
        roleRecipients.add(ROLE_NAME_0);
        expected.setRoleRecipients(roleRecipients);

        // Call method
        notificationService.createNotification(dto);
    }

    /**
     * Check that the system fails when trying to retrieve a non existing notification.
     *
     * @throws EntityNotFoundException Thrown if no entity with expected id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system fails when trying to retrieve a non existing notification.")
    public void retrieveNotificationNotFound() throws EntityNotFoundException {
        // Define expected
        Long id = 0L;

        // Mock methods
        Mockito.when(notificationRepository.existsById(id)).thenReturn(false);

        // Call tested method
        notificationService.retrieveNotification(id);
    }

    /**
     * Check that the system allows to retrieve a notification.
     *
     * @throws EntityNotFoundException Thrown if no entity with expected id could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_SET_520")
    @Requirement("REGARDS_DSL_DAM_SET_530")
    @Requirement("REGARDS_DSL_DAM_SET_540")
    @Purpose("Check that the system allows to retrieve a notification.")
    public void retrieveNotification() throws EntityNotFoundException {
        // Define expected
        Long id = 0L;
        Notification expected = new Notification();
        expected.setId(id);

        // Mock methods
        Mockito.when(notificationRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationRepository.findById(id)).thenReturn(Optional.of(expected));

        // Call tested method
        Notification actual = notificationService.retrieveNotification(id);

        // Check that expected is equel to acutal
        checkFieldsEqual(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).findById(id);
    }

    /**
     * Check that the system allows to update the parameter generating or not a notification.
     *
     * @throws EntityNotFoundException Thrown when no {@link Notification} with passed id could not be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update the parameter generating or not a notification.")
    public void updateNotificationStatus() throws EntityNotFoundException {
        // Define expected
        Long id = 0L;
        Notification initial = new Notification();
        initial.setId(id);
        initial.setStatus(NotificationStatus.UNREAD);

        // Mock the repository returned value
        Mockito.when(notificationRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationRepository.findById(id)).thenReturn(Optional.of(initial));

        // Perform the update
        notificationService.updateNotificationStatus(id, NotificationStatus.READ);

        // Define expected
        Notification expected = new Notification();
        expected.setId(id);
        expected.setStatus(NotificationStatus.READ);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system fails when trying to update a non existing notification.
     *
     * @throws EntityNotFoundException Thrown when no {@link Notification} with passed id could not be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system fails when trying to update a non existing notification.")
    public void updateNotificationStatusNotFound() throws EntityNotFoundException {
        Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.existsById(id)).thenReturn(false);

        // Perform the update
        notificationService.updateNotificationStatus(id, NotificationStatus.READ);
    }

    /**
     * Check that the system fails when trying to delete a non existing notification.
     *
     * @throws EntityNotFoundException Thrown when no {@link Notification} with passed id could not be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system fails when trying to delete a non existing notification.")
    public void deleteNotificationNotFound() throws EntityNotFoundException {
        Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.existsById(id)).thenReturn(false);

        // Perform the update
        notificationService.deleteNotification(id);
    }

    /**
     * Check that the system allorws to delete a notification.
     *
     * @throws EntityNotFoundException Thrown when no {@link Notification} with passed id could not be found
     */
    @Test
    @Purpose("Check that the system allows to delete a notification.")
    public void deleteNotification() throws EntityNotFoundException {
        // Define a notif
        Long id = 0L;

        // Mock the repository returned value
        Mockito.when(notificationRepository.existsById(id)).thenReturn(true);

        // Perform the update
        notificationService.deleteNotification(id);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).deleteById(id);
    }

    /**
     * Check that the system allorws to retrieve only notifications which should be sent.
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_150")
    @Purpose("Check that the system allows to retrieve only notifications which should be sent.")
    public void retrieveNotificationsToSend() {
        // Define expected
        List<Notification> expected = new ArrayList<>();
        expected.add(new Notification());
        expected.get(0).setStatus(NotificationStatus.UNREAD);

        // Mock the repository returned value
        Mockito.when(notificationRepository.findByStatus(NotificationStatus.UNREAD, PageRequest.of(0, 100)))
               .thenReturn(new PageImpl<>(expected));

        // Perform the update
        Page<Notification> actual = notificationService.retrieveNotificationsToSend(PageRequest.of(0, 100));

        // Check expected equals actual
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(new PageImpl<>(expected))));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationRepository).findByStatus(NotificationStatus.UNREAD, PageRequest.of(0, 100));
    }

    /**
     * Check that the system properly aggregates the list of notification recipients.
     */
    @Test
    @Purpose("Check that the system properly aggregates the list of notification recipients.")
    public void findRecipients() throws EntityNotFoundException {
        // Define expected
        List<ProjectUser> expected = new ArrayList<>();
        expected.add(projectUser0); // Expected from the notif roleRecipients attribute
        expected.add(projectUser1); // Expected from the notif projectUserRecipients attribute
        expected.add(projectUser2); // Expected from the notif roleRecipients attribute via parent role

        PageImpl<ProjectUser> expectedPage = new PageImpl<>(expected);
        // Mock
        Mockito.when(roleService.retrieveRoleProjectUserList(Mockito.anyString(), Mockito.any(Pageable.class)))
               .thenReturn(expectedPage);
        // Result
        Set<String> actual = notificationService.findRecipients(notification);

        // Compare
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(actual.containsAll(expected.stream()
                                                     .map(ProjectUser::getEmail)
                                                     .collect(Collectors.toList())));
    }

    /**
     * Check that the passed {@link Role} has same attributes as the passed {@link Role}.
     *
     * @param pExpected The expected role0
     * @param pActual   The actual role0
     */
    private void checkFieldsEqual(Notification pExpected, Notification pActual) {
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
