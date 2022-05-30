package fr.cnes.regards.framework.notification.client;

import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Notification client interface
 *
 * @author Marc SORDI
 */
public interface INotificationClientContract {

    /**
     * Notify a set of roles with a text plain message
     *
     * @param message message to notify
     * @param title   message title
     * @param level   {@link NotificationLevel}
     * @param roles   list of roles to notify
     */
    default void notify(String message, String title, NotificationLevel level, DefaultRole... roles) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, roles);
    }

    /**
     * Notify a set of roles
     *
     * @param message  message to notify
     * @param title    message title
     * @param level    {@link NotificationLevel}
     * @param mimeType MIME type ({@link MediaType} can be used!)
     * @param roles    list of roles to notify
     */
    default void notify(String message,
                        String title,
                        NotificationLevel level,
                        MimeType mimeType,
                        DefaultRole... roles) {
        notifyRoles(message, title, level, mimeType, Arrays.stream(roles).map(Enum::name).collect(Collectors.toSet()));
    }

    /**
     * Notify a set of roles
     *
     * @param message  message to notify
     * @param title    message title
     * @param level    {@link NotificationLevel}
     * @param mimeType MIME type ({@link MediaType} can be used!)
     * @param roles    list of roles to notify
     */
    void notifyRoles(String message, String title, NotificationLevel level, MimeType mimeType, Set<String> roles);

    /**
     * Notify a set of users with a text plain message
     *
     * @param message message to notify
     * @param title   message title
     * @param level   {@link NotificationLevel}
     * @param users   list of users to notify
     */
    default void notify(String message, String title, NotificationLevel level, String... users) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, users);
    }

    /**
     * Notify a set of users
     *
     * @param message  message to notify
     * @param title    message title
     * @param level    {@link NotificationLevel}
     * @param mimeType MIME type ({@link MediaType} can be used!)
     * @param users    list of users to notify
     */
    void notify(String message, String title, NotificationLevel level, MimeType mimeType, String... users);

    /**
     * Notify a user and a set of roles with a text plain message
     *
     * @param message message to notify
     * @param title   message title
     * @param level   {@link NotificationLevel}
     * @param user    user to notify
     * @param roles   list of roles to notify
     */
    default void notify(String message, String title, NotificationLevel level, String user, DefaultRole... roles) {
        notify(message, title, level, MimeTypeUtils.TEXT_PLAIN, user, roles);
    }

    /**
     * Notify a user and a set of roles
     *
     * @param message  message to notify
     * @param title    message title
     * @param level    {@link NotificationLevel}
     * @param mimeType MIME type ({@link MediaType} can be used!)
     * @param user     user to notify
     * @param roles    list of roles to notify
     */
    default void notify(String message,
                        String title,
                        NotificationLevel level,
                        MimeType mimeType,
                        String user,
                        DefaultRole... roles) {
        notifyUserAndRoles(message,
                           title,
                           level,
                           mimeType,
                           user,
                           Arrays.stream(roles).map(Enum::name).collect(Collectors.toSet()));
    }

    /**
     * Notify a user and a set of roles
     *
     * @param message  message to notify
     * @param title    message title
     * @param level    {@link NotificationLevel}
     * @param mimeType MIME type ({@link MediaType} can be used!)
     * @param user     user to notify
     * @param roles    list of roles to notify
     */
    void notifyUserAndRoles(String message,
                            String title,
                            NotificationLevel level,
                            MimeType mimeType,
                            String user,
                            Set<String> roles);
}
