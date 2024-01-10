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
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author sbinda
 */
@ActiveProfiles({ "test", "nomail" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notif_tests",
                                   "regards.accounts.root.user.login:test@test.fr",
                                   "purge.cron.expression=0 0 5 * * ?" })
public class NotificationServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private INotificationRepository notificationRepository;

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

        Assert.assertTrue("notif should exists", notificationRepository.findAll().size() == 3);

        // When
        notificationService.deleteNotification(notification.getId());

        // Then
        Assert.assertTrue("notif should be deleted", notificationRepository.findAll().size() == 2);
    }

    @Test
    public void test_deletion_notification_with_filter() {
        // Given
        Notification notification = notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                                                      "title",
                                                                                                      NotificationLevel.INFO,
                                                                                                      "moi").toRolesAndUsers(
            Sets.newHashSet(DefaultRole.ADMIN.toString()),
            Sets.newHashSet("jeanclaude")));

        SearchNotificationParameters filters = new SearchNotificationParameters();
        filters.withIdsIncluded(notification.getId());

        // When
        notificationService.deleteNotifications(filters, PageRequest.of(0, 10));

        // Then
        Assert.assertTrue("notif should be deleted", notificationRepository.findAll().size() == 0);
    }

}
