/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * @author Xavier-Alexandre Brochard
 */
public class NotificationUserSetting {

    /**
     * The notification
     */
    private final Notification notification;

    /**
     * One of the notification's recipients
     */
    private final String projectUser;

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
    public NotificationUserSetting(final Notification pNotification, final String pProjectUser,
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
    public String getProjectUser() {
        return projectUser;
    }

    /**
     * @return the settings
     */
    public NotificationSettings getSettings() {
        return settings;
    }

}