package fr.cnes.regards.modules.notification.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Feign client exposing the notification module endpoints to other microservices plugged through Eureka.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/notifications", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface INotificationClient {

    /**
     * Define the endpoint for retrieving the list of notifications for the logged user
     *
     * @return A {@link List} of {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Notification>> retrieveNotifications();

    /**
     * Define the endpoint for creating a new notification in db for later sending by a scheluder.
     *
     * @param pDto
     *            A DTO for easy parsing of the response body. Mapping to true {@link Notification} is done in service.
     * @return The sent notification as {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Notification> createNotification(NotificationDTO pDto);

    /**
     * Define the endpoint for retrieving a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @return The {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.GET)
    ResponseEntity<Notification> retrieveNotification(Long pId);

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pId
     *            The notification <code>id</code>
     * @param pStatus
     *            The new <code>status</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.PUT)
    void updateNotificationStatus(Long pId, NotificationStatus pStatus);

    /**
     * Define the endpoint for deleting a notification
     *
     * @param pId
     *            The notification <code>id</code>
     */
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.DELETE)
    void deleteNotification(Long pId);

    /**
     * Define the endpoint for retrieving the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    ResponseEntity<NotificationSettings> retrieveNotificationSettings();

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pDto
     *            The facade exposing user updatable fields of notification settings
     */
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    void updateNotificationSettings(NotificationSettingsDTO pDto);
}
