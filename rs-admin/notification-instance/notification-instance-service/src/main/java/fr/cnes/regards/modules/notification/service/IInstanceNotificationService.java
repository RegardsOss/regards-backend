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

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Strategy interface to handle CRUD operations on Notification entities
 *
 * @author Sylvain Vissiere-Guerinet
 */
public interface IInstanceNotificationService {

    /**
     * Retrieve the list of notifications intended for the logged user, trough the project user or their role.
     *
     * @return A {@link List} of {@link Notification}
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    Page<INotificationWithoutMessage> retrieveNotifications(Pageable page) throws EntityNotFoundException;

    /**
     * Save a new notification in db for later sending by a scheluder.
     *
     * @param pDto A DTO for easy parsing of the response body. Mapping to true {@link Notification} is expected to be
     *             done here.
     * @return The sent {@link Notification}
     */
    Notification createNotification(NotificationDTO pDto);

    /**
     * Retrieve a notification
     *
     * @param pId The notification <code>id</code>
     * @return The {@link Notification}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    Notification retrieveNotification(Long pId) throws EntityNotFoundException;

    /**
     * Update the {@link Notification#getStatus()}
     *
     * @param pId     The notification <code>id</code>
     * @param pStatus The new status value
     * @return The {@link Notification}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    Notification updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException;

    void markAllNotificationAs(NotificationStatus status);

    /**
     * Delete a notification
     *
     * @param pId The notification <code>id</code>
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    void deleteNotification(Long pId) throws EntityNotFoundException;

    /**
     * Retrieve all notifications which should be sent
     *
     * @return The list of notifications
     */
    Page<Notification> retrieveNotificationsToSend(Pageable page);

    Page<INotificationWithoutMessage> retrieveNotifications(Pageable page, NotificationStatus state)
        throws EntityNotFoundException;

    /**
     * Delete read notifications for current user
     */
    void deleteReadNotifications();

    Page<INotificationWithoutMessage> deleteReadNotificationsPage(Pageable page);
}
