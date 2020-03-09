/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.workspace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Default implementation. notify using notification client.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DefaultWorkspaceNotifier implements IWorkspaceNotifier {

    @Autowired
    private INotificationClient notifClient;

    @Override
    public void sendErrorNotification(String message, String title, DefaultRole role) {
        notifClient.notify(message, title, NotificationLevel.ERROR, role);
    }

    @Override
    public void sendWarningNotification(String message, String title, DefaultRole role) {
        notifClient.notify(message, title, NotificationLevel.WARNING, role);
    }

}
