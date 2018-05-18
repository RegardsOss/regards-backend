package fr.cnes.regards.modules.notification.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Feign client exposing the notification module endpoints to other microservices plugged through Eureka.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = INotificationClient.NOTIFICATION_PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface INotificationClient {

    public static final String NOTIFICATION_PATH = "/notifications";

    public static final String NOTIFICATION_ID_PATH = "/{notification_id}";

    public static final String NOTIFICATION_SETTINGS = "/settings";

    /**
     * Define the endpoint for retrieving the list of notifications for the logged user
     *
     * @return A {@link List} of {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Notification>> retrieveNotifications();

    /**
     * Define the endpoint for creating a new notification in db for later sending by a scheluder.
     *
     * @param pDto
     *            A DTO for easy parsing of the response body. Mapping to true {@link Notification} is done in service.
     * @return The sent notification as {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Notification> createNotification(@RequestBody final NotificationDTO pDto);

    /**
     * Define the endpoint for retrieving a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @return The {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = NOTIFICATION_ID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Notification> retrieveNotification(@PathVariable("notification_id") final Long pId);

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pId
     *            The notification <code>id</code>
     * @param pStatus
     *            The new <code>status</code>
     * @return The updated {@link Notification} wrapped in a {@link ResponseEntity}
     *
     */
    @RequestMapping(value = NOTIFICATION_ID_PATH, method = RequestMethod.PUT)
    public ResponseEntity<Notification> updateNotificationStatus(@PathVariable("notification_id") final Long pId,
            @RequestBody final NotificationStatus pStatus);

    /**
     * Define the endpoint for deleting a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @return void
     */
    @RequestMapping(value = NOTIFICATION_ID_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteNotification(@PathVariable("notification_id") final Long pId);

    /**
     * Define the endpoint for retrieving the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = NOTIFICATION_SETTINGS, method = RequestMethod.GET)
    public ResponseEntity<NotificationSettings> retrieveNotificationSettings();

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pNotificationSettings
     *            The facade exposing user updatable fields of notification settings
     * @return The updated {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = NOTIFICATION_SETTINGS, method = RequestMethod.PUT)
    public ResponseEntity<NotificationSettings> updateNotificationSettings(
            @RequestBody NotificationSettingsDTO pNotificationSettings);

    /**
     * Shortcut to create notification for specific roles
     * @param message
     * @param title
     * @param sender
     * @param notificationType
     * @param roles
     */
    default void notifyRoles(String message, String title, String sender, NotificationType notificationType,
            DefaultRole... roles) {
        createNotification(new NotificationDTO(message,
                                               Sets.newHashSet(),
                                               Arrays.stream(roles).map(r -> r.name()).collect(Collectors.toSet()),
                                               sender,
                                               title,
                                               notificationType));
    }

}
