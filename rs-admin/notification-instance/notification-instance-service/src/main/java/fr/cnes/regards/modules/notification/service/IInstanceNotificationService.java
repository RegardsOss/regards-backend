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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Strategy interface to handle CRUD operations on Notification entities
 *
 * @author Sylvain Vissiere-Guerinet
 */
public interface IInstanceNotificationService {

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
    Notification retrieveNotification(Long pId) throws ModuleException;

    /**
     * Update the {@link Notification#getStatus()}
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
     * Retrieve the pages {@link List} of all (@link {@link NotificationLight}s filtered by given properties
     */
    Page<NotificationLight> findAllOrderByDateDesc(SearchNotificationParameters filters, int page, int pageSize)
        throws ModuleException;

    /**
     * Delete all notifications that match filters
     *
     * @param filters search parameters
     */
    void deleteNotifications(SearchNotificationParameters filters);
}
