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
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

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
    private INotificationRepository repo;

    @MockBean
    private IAccessGroupClient accessGroupClient;

    @MockBean
    private IEmailClient emailClient;

    @MockBean
    private IStorageSettingClient storageSettingClient;

    @Before
    public void init() throws EntityException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        repo.deleteAll();
        if (!roleService.existByName(authResolver.getRole())) {
            roleService.createRole(new Role(authResolver.getRole(),
                                            roleService.retrieveRole(DefaultRole.REGISTERED_USER.toString())));
        }

    }

    @Test
    public void deleteNotifications() {

        NotificationDTO notification = new NotificationDtoBuilder("message",
                                                                  "title",
                                                                  NotificationLevel.INFO,
                                                                  "moi").toRolesAndUsers(Sets.newHashSet(DefaultRole.ADMIN.toString()),
                                                                                         Sets.newHashSet("jeanclaude"));
        notificationService.createNotification(notification);

        notification = new NotificationDtoBuilder("message2", "title2", NotificationLevel.INFO, "moi").toRolesAndUsers(
            Sets.newHashSet(authResolver.getRole()),
            Sets.newHashSet(authResolver.getUser()));
        notificationService.createNotification(notification);

        notification = new NotificationDtoBuilder("message3",
                                                  "title3",
                                                  NotificationLevel.INFO,
                                                  "moi").toRoles(Sets.newHashSet(authResolver.getRole()));
        notificationService.createNotification(notification);
        Assert.assertTrue("notif should exists", repo.findAll().size() == 3);

        notificationService.deleteReadNotifications();

        List<Notification> notifs = repo.findAll();
        Assert.assertTrue("notif should still exists as no one is in READ status", notifs.size() == 3);

        notificationService.markAllNotificationAs(NotificationStatus.READ);

        notificationService.deleteReadNotifications();

        notifs = repo.findAll();
        Assert.assertEquals("One notif should remains for ADMIN role", 1, notifs.size());
        Assert.assertTrue("One notif should remains for ADMIN role as UNREAD",
                          notifs.get(0).getStatus() == NotificationStatus.UNREAD);

    }

}
