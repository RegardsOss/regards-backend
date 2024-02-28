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
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * Strategy interface to handle CRUD operations on Notification entities
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationService {

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
     * Update the {@link Notification}
     *
     * @param pId     The notification <code>id</code>
     * @param pStatus The new status value
     * @return The {@link Notification}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    Notification updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException;

    /**
     * Delete a notification
     *
     * @param pId The notification <code>id</code>
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    void deleteNotification(Long pId) throws EntityNotFoundException;

    /**
     * Delete all notifications that match filters
     *
     * @param filters search parameters
     */
    void deleteNotifications(SearchNotificationParameters filters);

    /**
     * Retrieve all notifications which should be sent
     *
     * @return The list of notifications
     */
    Page<Notification> retrieveNotificationsToSend(Pageable page);

    /**
     * Gather the list of recipients on a notification
     *
     * @param pNotification The notification
     * @return The stream of project users
     */
    Set<String> findRecipients(Notification pNotification);

    /**
     * Counter number of unread notifications for current user
     *
     * @return long
     */
    Long countUnreadNotifications();

    /**
     * Counter number of read notifications for current user
     *
     * @return long
     */
    Long countReadNotifications();

    /**
     * Retrieve a notification light page matching filter
     *
     * @param filters  search parameters
     * @param pageable the paging information
     * @return a notification light page
     */
    Page<NotificationLight> findAll(SearchNotificationParameters filters, Pageable pageable);
}
