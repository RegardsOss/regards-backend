/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.signature;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * REST interface to define the entry points of the module.
 *
 * @author CS SI
 */
@RequestMapping("/notifications")
public interface INotificationSignature {

    /**
     * Define the endpoint for retrieving the list of notifications for the logged user
     *
     * @return A {@link List} of {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @GetMapping
    ResponseEntity<List<Notification>> retrieveNotifications();

    /**
     * Define the endpoint for creating a new notification in db for later sending by a scheluder.
     *
     * @param pDto
     *            A DTO for easy parsing of the response body. Mapping to true {@link Notification} is done in service.
     * @return The sent notification as {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @PostMapping
    @ResponseBody
    ResponseEntity<Notification> createNotification(NotificationDTO pDto);

    /**
     * Define the endpoint for retrieving a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     * @return The {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @GetMapping("/{notification_id}")
    ResponseEntity<Notification> retrieveNotification(Long pId) throws EntityNotFoundException;

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pId
     *            The notification <code>id</code>
     * @param pStatus
     *            The new <code>status</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     *
     */
    @PutMapping("/{notification_id}")
    @ResponseBody
    void updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException;

    /**
     * Define the endpoint for deleting a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     * @return
     */
    @DeleteMapping("/{notification_id}")
    void deleteNotification(Long pId) throws EntityNotFoundException;

    /**
     * Define the endpoint for retrieving the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     */
    @GetMapping("/settings")
    ResponseEntity<NotificationSettings> retrieveNotificationSettings();

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pDto
     *            The facade exposing user updatable fields of notification settings
     * @throws EntityNotFoundException
     *             Thrown when no notification settings with passed <code>id</code> could be found
     */
    @PutMapping("/settings")
    void updateNotificationSettings(NotificationSettingsDTO pDto) throws EntityNotFoundException;

}
