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
package fr.cnes.regards.modules.notification.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationType;

/**
 * @author Christophe Mertz
 *
 */
@Ignore("Fix multitenant and instance conflicts")
@ContextConfiguration(classes = { NotificationDaoTestConfig.class })
public class NotificationDaoIT extends AbstractDaoTransactionalTest {

    @Autowired
    private INotificationRepository notificationRepository;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Test
    public void createNotification() {
        Assert.assertTrue(notificationRepository.count() == 0);
        Assert.assertTrue(projectUserRepository.count() == 0);
        Assert.assertTrue(roleRepository.count() == 0);

        // create a new Notification
        final Notification notif = getNotification("Hello world!", "Bob", NotificationStatus.UNREAD);

        // Set Recipients
        final List<ProjectUser> pUsers = new ArrayList<>();
        pUsers.add(getProjectUser("bob-regards@c-s.fr"));
        pUsers.add(getProjectUser("jo-regards@c-s.fr"));
        notif.setProjectUserRecipients(pUsers);
        Assert.assertTrue(projectUserRepository.count() == 2);

        // Set Roles
        final Role publicRole = getRole("PUBLIC");
        notif.setRoleRecipients(Arrays.asList(publicRole));
        Assert.assertTrue(roleRepository.count() == 1);

        // Save the notification
        final Notification notifSaved = notificationRepository.save(notif);
        Assert.assertTrue(notificationRepository.count() == 1);

        Assert.assertNotNull(notifSaved);
        Assert.assertNotNull(notifSaved.getId());

        Assert.assertNotNull(notificationRepository.findOne(notifSaved.getId()));

        // create a second notification
        final Notification secondNotif = getNotification("Hello Paris!", "jack", NotificationStatus.UNREAD);

        // Set recipient
        secondNotif.setProjectUserRecipients(Arrays.asList(getProjectUser("jack-regards@c-s.fr")));

        // Set Role
        secondNotif.setRoleRecipients(Arrays.asList(publicRole));

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

    private ProjectUser getProjectUser(String pEmail) {
        final ProjectUser projectUser = new ProjectUser();
        projectUser.setEmail(pEmail);
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        return projectUserRepository.save(projectUser);
    }

    private Role getRole(String pName) {
        final Role role = new Role(pName, null);
        return roleRepository.save(role);
    }

}
