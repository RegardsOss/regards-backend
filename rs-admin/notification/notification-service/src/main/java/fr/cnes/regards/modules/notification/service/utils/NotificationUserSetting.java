/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service.utils;

import java.util.stream.Stream;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;

/**
 * Class NotificationUserSetting
 *
 * This class' purpose it to agregate a {@link Notification}, one of it's recipients as a {@link ProjectUser} and this
 * recipient's {@link NotificationSettings}.<br>
 * This allows a more simple of {@link Stream}s as we can apply filter on this class.
 *
 * @author CS SI
 */
public class NotificationUserSetting {

    /**
     * The notification
     */
    private final Notification notification;

    /**
     * One of the notification's recipients
     */
    private final ProjectUser projectUser;

    /**
     * The recipient's settings
     */
    private final NotificationSettings settings;

    /**
     * Creates a new aggregator
     *
     * @param pNotification
     *            The notification
     * @param pProjectUser
     *            One of the notification's recipients
     * @param pSettings
     *            The recipient's settings
     */
    public NotificationUserSetting(final Notification pNotification, final ProjectUser pProjectUser,
            final NotificationSettings pSettings) {
        notification = pNotification;
        projectUser = pProjectUser;
        settings = pSettings;
    }

    /**
     * @return the notification
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * @return the projectUser
     */
    public ProjectUser getProjectUser() {
        return projectUser;
    }

    /**
     * @return the settings
     */
    public NotificationSettings getSettings() {
        return settings;
    }

}