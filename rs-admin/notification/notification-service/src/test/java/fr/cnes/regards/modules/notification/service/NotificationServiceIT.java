/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author sbinda
 */
@ActiveProfiles({ "test", "nomail" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notif_tests",
                                   "regards.accounts.root.user.login=test@test.fr",
                                   "purge.cron.expression=0 0 5 * * ?" })
public class NotificationServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @SpyBean
    private IAuthenticationResolver authResolver;

    @Autowired
    protected INotificationRepository notificationRepository;

    @MockBean
    private IAccessGroupClient accessGroupClient;

    @MockBean
    private IEmailClient emailClient;

    @MockBean
    private IStorageSettingClient storageSettingClient;

    @MockBean
    private IStorageDownloadQuotaRestClient storageDownloadQuotaRestClient;

    @MockBean
    private IResourceService resourceService;

    @Before
    public void init() throws EntityException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        notificationRepository.deleteAll();

        if (!roleService.existByName(authResolver.getRole())) {
            roleService.createRole(new Role(authResolver.getRole(),
                                            roleService.retrieveRole(DefaultRole.REGISTERED_USER.toString())));
        }
    }

    @Test
    public void test_retrieve_notif_to_send() throws ModuleException {
        int nbNotifs = 60;
        // Given
        for (int i = 0; i < nbNotifs; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.EXPLOIT.toString()), Sets.newHashSet("jeanclaude")));
        }

        Page<Notification> toSend = notificationService.retrieveNotificationsToSend(PageRequest.of(0, 10));
        Assert.assertEquals(10, toSend.getNumberOfElements());
        Assert.assertEquals(nbNotifs, toSend.getTotalElements());
        toSend.getContent().forEach(n -> Assert.assertEquals(3, n.getRoleRecipients().size()));
        toSend.getContent().forEach(n -> Assert.assertEquals(1, n.getProjectUserRecipients().size()));
    }

    @Test
    public void test_find_all() throws InterruptedException, ModuleException {
        // Given
        OffsetDateTime lastNotificationDate = null;
        for (int i = 0; i < 50; i++) {
            Notification notif = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                   "title",
                                                                                                   NotificationLevel.INFO,
                                                                                                   "moi").toRolesAndUsers(
                Sets.newHashSet(DefaultRole.EXPLOIT.toString()),
                Sets.newHashSet("jeanclaude")));
            // Add a sleep thread to have creation date different for each notification to check for sort function
            Thread.sleep(10);
            lastNotificationDate = notif.getDate();
        }
        // Add non matching notifications
        for (int i = 0; i < 5; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.ADMIN.toString()), Sets.newHashSet("nathalie")));
            // Add a sleep thread to have creation date different for each notification to check for sort function
            Thread.sleep(10);
        }

        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> DefaultRole.EXPLOIT.toString()).when(authResolver).getRole();

        Page<NotificationLight> page = notificationService.findAllOrderByDateDesc(new SearchNotificationParameters(),
                                                                                  0,
                                                                                  10);
        Assert.assertEquals(5, page.getTotalPages());
        Assert.assertEquals(50, page.getTotalElements());
        Assert.assertEquals(10, page.getSize());
        Assert.assertEquals(10, page.getNumberOfElements());
        Assert.assertEquals(0, page.getNumber());
        // Validate that we can access role name associated to a notification.
        Assert.assertTrue(page.getContent()
                              .stream()
                              .allMatch(notification -> notification.getRoleRecipients()
                                                                    .contains(DefaultRole.EXPLOIT.toString())));
        // Validate that we can access user name associated to a notification.
        Assert.assertTrue(page.getContent()
                              .stream()
                              .allMatch(notification -> notification.getProjectUserRecipients()
                                                                    .contains("jeanclaude")));
        // Check sort function by date
        Assert.assertEquals("First element of the page should be the last notification by date",
                            lastNotificationDate.truncatedTo(ChronoUnit.MILLIS).toEpochSecond(),
                            page.getContent().get(0).getDate().truncatedTo(ChronoUnit.MILLIS).toEpochSecond());
        for (int i = 0; i < page.getNumberOfElements() - 1; i++) {
            Assert.assertTrue("Page should be ordered by date",
                              page.getContent().get(i).getDate().isAfter(page.getContent().get(i + 1).getDate()));
        }
    }

    @Test
    public void test_find_all_as_instance() throws ModuleException {
        // Given
        for (int i = 0; i < 50; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));
        }

        Mockito.doAnswer(answer -> null).when(authResolver).getUser();
        Mockito.doAnswer(answer -> null).when(authResolver).getRole();

        // When
        Page<NotificationLight> page = notificationService.findAllOrderByDateDesc(new SearchNotificationParameters(),
                                                                                  0,
                                                                                  10);

        // Then
        Assert.assertEquals(5, page.getTotalPages());
        Assert.assertEquals(50, page.getTotalElements());
        Assert.assertEquals(10, page.getSize());
        Assert.assertEquals(10, page.getNumberOfElements());
        Assert.assertEquals(0, page.getNumber());
    }

    @Test
    public void test_delete_all_as_instance() {
        // Given
        for (int i = 0; i < 50; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));
        }

        Mockito.doAnswer(answer -> null).when(authResolver).getUser();
        Mockito.doAnswer(answer -> null).when(authResolver).getRole();

        // When
        notificationService.deleteNotifications(new SearchNotificationParameters());
        // Then
        Assert.assertEquals(0, notificationRepository.count());
    }

    @Test
    public void test_delete_notification() throws EntityNotFoundException {
        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        notificationService.createNotification(new NotificationDtoBuilder("message2",
                                                                          "title2",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            authResolver.getRole()), Sets.newHashSet(authResolver.getUser())));

        Notification notification = notificationService.createNotification(new NotificationDtoBuilder("message3",
                                                                                                      "title3",
                                                                                                      NotificationLevel.INFO,
                                                                                                      "moi").toRoles(
            Sets.newHashSet(authResolver.getRole())));

        Assert.assertEquals("notif should exists", 3, notificationRepository.findAll().size());

        // When
        notificationService.deleteNotification(notification.getId());

        // Then
        Assert.assertEquals("notif should be deleted", 2, notificationRepository.findAll().size());
    }

    @Test
    public void test_deletion_notification_with_level_filter() {
        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title2",
                                                                          NotificationLevel.ERROR,
                                                                          "lui").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title2",
                                                                          NotificationLevel.FATAL,
                                                                          "lui").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title2",
                                                                          NotificationLevel.WARNING,
                                                                          "lui").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withLevelsIncluded(List.of(NotificationLevel.INFO));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only INFO notification are deleted", 3, notificationRepository.findAll().size());

        // Given
        filters.withLevelsIncluded(List.of(NotificationLevel.ERROR,
                                           NotificationLevel.FATAL,
                                           NotificationLevel.WARNING));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("All notifications are deleted", 0, notificationRepository.findAll().size());
    }

    @Test
    public void test_deletion_notification_with_senders_filter() {
        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title2",
                                                                          NotificationLevel.ERROR,
                                                                          "lui").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withSendersIncluded(List.of("moi"));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only MOI notification are deleted", 1, notificationRepository.findAll().size());
    }

    @Test
    public void test_deletion_notification_with_status_filter() throws EntityNotFoundException {
        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withStatusIncluded(List.of(NotificationStatus.UNREAD));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only UNREAD notification are deleted", 0, notificationRepository.findAll().size());

        Notification notification = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                      "title2",
                                                                                                      NotificationLevel.ERROR,
                                                                                                      "lui").toRolesAndUsers(
            Sets.newHashSet(DefaultRole.PROJECT_ADMIN.toString()),
            Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        notificationService.updateNotificationStatus(notification.getId(), NotificationStatus.READ);

        filters = new SearchNotificationParameters();
        filters.withStatusIncluded(List.of(NotificationStatus.READ));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only READ notification are deleted", 0, notificationRepository.findAll().size());
    }

    @Test
    public void test_deletion_notification_with_dates_filter() {
        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withDateBefore(OffsetDateTime.now().plusDays(1));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only Before tomorrow notification are deleted",
                            0,
                            notificationRepository.findAll().size());

        // Given
        notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                          "title",
                                                                          NotificationLevel.INFO,
                                                                          "moi").toRolesAndUsers(Sets.newHashSet(
            DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        filters = new SearchNotificationParameters();
        filters.withDateAfter(OffsetDateTime.now().minusDays(1));

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only After yesterday notification are deleted",
                            0,
                            notificationRepository.findAll().size());
    }

    @Test
    @Ignore("Ignoring performance test")
    public void test_deletion_perf() {
        for (int i = 0; i < 1_000; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.PROJECT_ADMIN.toString()), Sets.newHashSet("jeanclaude")));
        }
        for (int i = 0; i < 1_000; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.ERROR,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.ADMIN.toString()), Sets.newHashSet("michou")));
        }
        for (int i = 0; i < 1_000; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.WARNING,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.EXPLOIT.toString()), Sets.newHashSet("juliette")));
        }

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        // Then
        notificationService.deleteNotifications(new SearchNotificationParameters().withStatusIncluded(NotificationStatus.UNREAD)
                                                                                  .withLevelsIncluded(List.of(
                                                                                      NotificationLevel.ERROR)));
    }

    @Test
    public void test_deletion_notification_by_user() {
        // Given
        Notification notification = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                      "title",
                                                                                                      NotificationLevel.INFO,
                                                                                                      "moi").toRolesAndUsers(
            Sets.newHashSet(DefaultRole.PROJECT_ADMIN.toString()),
            Sets.newHashSet("jeanclaude")));

        Notification notification2 = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                       "title2",
                                                                                                       NotificationLevel.INFO,
                                                                                                       "lui").toRolesAndUsers(
            Sets.newHashSet(DefaultRole.ADMIN.toString()),
            Sets.newHashSet("michou")));

        Notification notification3 = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                       "title2",
                                                                                                       NotificationLevel.INFO,
                                                                                                       "lui").toRolesAndUsers(
            Sets.newHashSet(DefaultRole.EXPLOIT.toString()),
            Sets.newHashSet("juliette")));

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withIdsIncluded(notification.getId(), notification2.getId(), notification3.getId());

        // Given
        Mockito.doAnswer(answer -> "juliette").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "EXPLOIT").when(authResolver).getRole();

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only exploit notification are deleted", 2, notificationRepository.findAll().size());

        // Given
        Mockito.doAnswer(answer -> "michou").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "ADMIN").when(authResolver).getRole();

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("Only admin notification are deleted", 1, notificationRepository.findAll().size());

        // Given
        Mockito.doAnswer(answer -> "jeanclaude").when(authResolver).getUser();
        Mockito.doAnswer(answer -> "PROJECT_ADMIN").when(authResolver).getRole();

        // When
        notificationService.deleteNotifications(filters);

        // Then
        Assert.assertEquals("All notification are deleted", 0, notificationRepository.findAll().size());
    }

}
