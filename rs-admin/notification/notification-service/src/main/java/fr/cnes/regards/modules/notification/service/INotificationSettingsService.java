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
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Strategy interface to handle CRUD operations on {@link NotificationService} entities.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationSettingsService {

    /**
     * Retrieve the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings}
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    NotificationSettings retrieveNotificationSettings() throws EntityNotFoundException;

    /**
     * Retrieve {@link NotificationSettings} for the given user if any. Create it otherwise.
     */
    NotificationSettings retrieveNotificationSettings(String userEmail);

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pNotificationSettings The notification settings
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    NotificationSettings updateNotificationSettings(NotificationSettingsDTO pNotificationSettings)
        throws EntityNotFoundException;

}
