/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notification.dao;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationType;

/**
 * @author Christophe Mertz
 *
 */
@Ignore("Fix multitenant and instance conflicts")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:notif_dao" })
@ContextConfiguration(classes = { NotificationDaoTestConfig.class })
public class NotificationDaoIT extends AbstractDaoTransactionalTest {

    @Autowired
    private INotificationRepository notificationRepository;

    @Test
    public void createNotification() {
        Assert.assertTrue(notificationRepository.count() == 0);

        // create a new Notification
        final Notification notif = getNotification("Hello world!", "Bob", NotificationStatus.UNREAD);

        // Set Recipients
        final Set<String> pUsers = new HashSet<>();
        pUsers.add("bob-regards@c-s.fr");
        pUsers.add("jo-regards@c-s.fr");
        notif.setProjectUserRecipients(pUsers);

        // Set Roles
        notif.setRoleRecipients(Sets.newHashSet(DefaultRole.PUBLIC.name()));

        // Save the notification
        final Notification notifSaved = notificationRepository.save(notif);
        Assert.assertTrue(notificationRepository.count() == 1);

        Assert.assertNotNull(notifSaved);
        Assert.assertNotNull(notifSaved.getId());

        Assert.assertNotNull(notificationRepository.findOne(notifSaved.getId()));

        // create a second notification
        final Notification secondNotif = getNotification("Hello Paris!", "jack", NotificationStatus.UNREAD);

        // Set recipient
        secondNotif.setProjectUserRecipients(Sets.newHashSet("jack-regards@c-s.fr"));

        // Set Role
        secondNotif.setRoleRecipients(Sets.newHashSet(DefaultRole.PUBLIC.name()));

        // Save the notification
        notificationRepository.save(secondNotif);
        Assert.assertTrue(notificationRepository.count() == 2);

    }

    private Notification getNotification(String pMessage, String pSender, NotificationStatus pStatus) {
        final Notification notif = new Notification();
        notif.setMessage(pMessage);
        notif.setSender(pSender);
        notif.setStatus(pStatus);
        notif.setType(NotificationType.INFO);
        return notif;
    }

    @Test
    public void testUpdateAll() {
        notificationRepository.updateAllNotificationStatusByRole(NotificationStatus.READ, "ADMIN");
        notificationRepository.updateAllNotificationStatusByUser(NotificationStatus.READ, "regards@c-s.fr");
    }
}
