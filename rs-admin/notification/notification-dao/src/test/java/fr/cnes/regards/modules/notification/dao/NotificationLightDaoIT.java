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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSpecificationBuilder;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Christophe Mertz
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notif_dao_light" })
@ContextConfiguration(classes = { NotificationDaoTestConfig.class })
@ActiveProfiles({ "noscheduler" })
public class NotificationLightDaoIT extends AbstractDaoTransactionalIT {

    @Autowired
    private INotificationLightRepository notificationLightRepository;

    private Set<String> createRecipients() {
        final Set<String> pUsers = new HashSet<>();
        pUsers.add("bob-regards@c-s.fr");
        pUsers.add("jo-regards@c-s.fr");
        return pUsers;
    }

    @Test
    public void createNotification() {

        // Given

        Assert.assertEquals(0, notificationLightRepository.count());

        // create a new Notification
        final NotificationLight notif = createNotificationLight("Bob", NotificationStatus.UNREAD);

        // Set Recipients
        final Set<String> pUsers = createRecipients();
        notif.setProjectUserRecipients(pUsers);

        // Set Roles
        notif.setRoleRecipients(Sets.newHashSet(DefaultRole.PUBLIC.name()));

        // Save the notification
        final NotificationLight notifSaved = notificationLightRepository.save(notif);
        Assert.assertEquals(1, notificationLightRepository.count());

        Assert.assertNotNull(notifSaved);
        Assert.assertNotNull(notifSaved.getId());

        Assert.assertTrue(notificationLightRepository.findById(notifSaved.getId()).isPresent());

        // create a second notification
        final NotificationLight secondNotif = createNotificationLight("jack", NotificationStatus.UNREAD);

        // Set recipient
        secondNotif.setProjectUserRecipients(Sets.newHashSet("jack-regards@c-s.fr"));

        // Set Role
        secondNotif.setRoleRecipients(Sets.newHashSet(DefaultRole.PUBLIC.name()));

        // When

        // Save the notification
        notificationLightRepository.save(secondNotif);

        // Then

        Assert.assertEquals(2, notificationLightRepository.count());

    }

    private NotificationLight createNotificationLight(String pSender, NotificationStatus pStatus) {
        final NotificationLight notif = new NotificationLight();
        notif.setSender(pSender);
        notif.setStatus(pStatus);
        notif.setLevel(NotificationLevel.INFO);
        return notif;
    }

    @Test
    public void testRetrievePage() {

        // Given

        //create 2 UNREAD notification
        createNotification();
        // create read notification
        final NotificationLight readNotif = createNotificationLight("Rid", NotificationStatus.READ);
        readNotif.setProjectUserRecipients(Sets.newHashSet("read-regards@c-s.fr"));
        readNotif.setRoleRecipients(Sets.newHashSet(DefaultRole.PROJECT_ADMIN.name()));
        notificationLightRepository.save(readNotif);

        // When

        // now lets retrieve them by status
        Pageable page = PageRequest.of(0, 100);
        SearchNotificationParameters filters = new SearchNotificationParameters().withStatusIncluded(Arrays.asList(
            NotificationStatus.UNREAD));
        Page<NotificationLight> notifPage = notificationLightRepository.findAll(new NotificationSpecificationBuilder().withParameters(
            filters).build(), page);

        // Then
        Assert.assertTrue("There should be 2 notification: 2 notifications UNREAD for role PUBLIC",
                          notifPage.getTotalElements() == 2);
        Assert.assertTrue("Project user recipients must be correctly set",
                          notifPage.stream().allMatch(notification -> notification.getProjectUserRecipients() != null));
    }
}
