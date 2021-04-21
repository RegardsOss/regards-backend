/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationToSendEvent;

/**
 * Service responsible for scheduling the sending of notifications to their recipients.<br>
 * It periodically retrieves the notifications to send, and sends thems to their recipients.<br>
 * Implements a strategy pattern on the sending method in order to easily add ways of sending notifications (mail, ihm,
 * owl...).
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@EnableScheduling
@RegardsTransactional
public class SendingScheduler implements ApplicationListener<NotificationToSendEvent> {

    /**
     * The service responsible for managing notifications
     */
    @Autowired
    private IInstanceNotificationService notificationService;

    /**
     * Notification sending strategy
     */
    @Autowired
    private ISendingStrategy strategy;

    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    /**
     * Find all notifications which should be sent daily and send them with the sending strategy
     */
    @Scheduled(cron = "${regards.notification.cron.daily}")
    public void sendDaily() {
        notificationService.retrieveNotificationsToSend(PageRequest.of(0, 10)).getContent()
                .forEach(n -> sendNotification(n));
    }

    private void sendNotification(Notification notification) {
        String[] recipients = { rootAdminUserLogin };
        strategy.send(notification, recipients);
        // Update sent date
        notification.setDate(OffsetDateTime.now());
    }

    /**
     * Allows to trigger sending process before the frequency
     */
    @Override
    public void onApplicationEvent(NotificationToSendEvent event) {
        Notification notif = event.getNotification();
        sendNotification(notif);
    }
}
