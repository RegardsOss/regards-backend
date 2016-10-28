/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Strategy interface to handle CRUD operations on {@link NotificationService} entities.
 *
 * @author CS SI
 */
public interface INotificationSettingsService {

    /**
     * Retrieve the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings}
     */
    NotificationSettings retrieveNotificationSettings();

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pNotificationSettings
     *            The notification settings
     */
    void updateNotificationSettings(NotificationSettingsDTO pNotificationSettings);

}
