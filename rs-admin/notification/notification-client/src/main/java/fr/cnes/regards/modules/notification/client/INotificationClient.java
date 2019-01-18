package fr.cnes.regards.modules.notification.client;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.NotificationLevel;

/**
 * Notification client interface
 *
 * @author Marc SORDI
 */
public interface INotificationClient {

    /**
     * Notify a set of roles with a text plain message
     */
    default void notify(String message, String title, NotificationLevel level, DefaultRole... roles) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, roles);
    }

    /**
     * Notify a set of roles
     */
    void notify(String message, String title, NotificationLevel level, MimeType mimeType, DefaultRole... roles);

    /**
     * Notify a set of users with a text plain message
     */
    default void notify(String message, String title, NotificationLevel level, String... users) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, users);
    }

    /**
     * Notify a set of users
     */
    void notify(String message, String title, NotificationLevel level, MimeType mimeType, String... users);

    /**
     * Notify a user and a set of roles with a text plain message
     */
    default void notify(String message, String title, NotificationLevel level, String user, DefaultRole... roles) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, user, roles);
    }

    /**
     * Notify a user and a set of roles
     */
    void notify(String message, String title, NotificationLevel level, MimeType mimeType, String user,
            DefaultRole... roles);
}
