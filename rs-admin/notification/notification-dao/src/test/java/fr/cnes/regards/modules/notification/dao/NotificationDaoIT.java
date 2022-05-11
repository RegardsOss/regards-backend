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
package fr.cnes.regards.modules.notification.dao;

import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;

/**
 * @author Christophe Mertz
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:notif_dao" })
@ContextConfiguration(classes = { NotificationDaoTestConfig.class })
public class NotificationDaoIT extends AbstractDaoTransactionalIT {

    @Autowired
    private INotificationRepository notificationRepository;

    @Test
    public void getAllNotifications() {
        notificationRepository.findByStatusAndRecipientsContaining(NotificationStatus.UNREAD, "null", DefaultRole.PROJECT_ADMIN.toString(), PageRequest.of(0, 10));
    }

    @Test
    public void createNotification() {
        Assert.assertEquals(0, notificationRepository.count());

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
        Assert.assertEquals(1, notificationRepository.count());

        Assert.assertNotNull(notifSaved);
        Assert.assertNotNull(notifSaved.getId());

        Assert.assertTrue(notificationRepository.findById(notifSaved.getId()).isPresent());

        // create a second notification
        final Notification secondNotif = getNotification("Hello Paris!", "jack", NotificationStatus.UNREAD);

        // Set recipient
        secondNotif.setProjectUserRecipients(Sets.newHashSet("jack-regards@c-s.fr"));

        // Set Role
        secondNotif.setRoleRecipients(Sets.newHashSet(DefaultRole.PUBLIC.name()));

        // Save the notification
        notificationRepository.save(secondNotif);
        Assert.assertEquals(2, notificationRepository.count());

    }

    private Notification getNotification(String pMessage, String pSender, NotificationStatus pStatus) {
        final Notification notif = new Notification();
        notif.setMessage(pMessage);
        notif.setSender(pSender);
        notif.setStatus(pStatus);
        notif.setLevel(NotificationLevel.INFO);
        return notif;
    }

    @Test
    public void testUpdateAll() {
        notificationRepository.updateAllNotificationStatusByRole(NotificationStatus.READ.toString(), "ADMIN");
        notificationRepository.updateAllNotificationStatusByUser(NotificationStatus.READ.toString(), "regards@c-s.fr");
    }

    @Test
    public void testRetrievePage() {
        //create 2 UNREAD notification
        createNotification();
        // create read notification
        final Notification readNotif = getNotification("Hello READ!", "Rid", NotificationStatus.READ);
        readNotif.setProjectUserRecipients(Sets.newHashSet("read-regards@c-s.fr"));
        readNotif.setRoleRecipients(Sets.newHashSet(DefaultRole.PROJECT_ADMIN.name()));
        notificationRepository.save(readNotif);

        // now lets retrieve them by status
        Page<INotificationWithoutMessage> notifPage = notificationRepository.findByStatusAndRecipientsContaining(NotificationStatus.UNREAD, "jo-regards@c-s.fr", DefaultRole.PUBLIC.toString(),
                                                                                      PageRequest.of(0,20));
        Assert.assertTrue("There should be 2 notification: 2 notifications UNREAD for role PUBLIC", notifPage.getTotalElements() == 2);
    }
}
