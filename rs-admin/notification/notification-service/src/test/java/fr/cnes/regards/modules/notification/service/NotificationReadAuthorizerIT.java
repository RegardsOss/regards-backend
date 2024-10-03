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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.notification.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.NotificationReadAuthorizer;
import fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author tguillou
 */
@ActiveProfiles({ "test", "nomail" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notificationreadit",
                                   "regards.accounts.root.user.login=test@test.fr",
                                   "regards.notification.cache.duration.hours=2",
                                   "regards.notification.stored.max.allowed=2" })
public class NotificationReadAuthorizerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private NotificationReadAuthorizer notificationReadAuthorizer;

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

    @Test
    public void test_read_max_allowed() throws ModuleException, InterruptedException {
        notificationRepository.deleteAll();
        // only 1 notif in database -> read allowed
        createNotifications(1);
        notificationService.countReadNotifications();

        // 11 notif in database -> read still allowed because cache is not expired
        createNotifications(10);

        //  force cache expiration
        notificationReadAuthorizer.clearCache();

        // now read is forbidden
        try {
            notificationService.countReadNotifications();
            Assertions.fail("should throw here");
        } catch (ModuleException e) {
        }
        // read still forbidden
        try {
            notificationService.retrieveNotification(0L);
            Assertions.fail("should throw here");
        } catch (ModuleException e) {
        }

        // delete notifs and reset cache to allow read again
        notificationRepository.deleteAll();
        notificationReadAuthorizer.clearCache();
        notificationService.countUnreadNotifications();
    }

    private void createNotifications(int numberOfNotifications) {
        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.createNotification(new NotificationDtoBuilder("message",
                                                                              "title",
                                                                              NotificationLevel.INFO,
                                                                              "moi").toRolesAndUsers(Sets.newHashSet(
                DefaultRole.EXPLOIT.toString()), Sets.newHashSet("jeanclaude")));
        }
    }
}
