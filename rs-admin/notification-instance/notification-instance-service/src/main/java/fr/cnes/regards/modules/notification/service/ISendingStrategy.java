/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Strategy interface to define a method for sending notifications. It can be mail, ihm...
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface ISendingStrategy {

    /**
     * Send the passed notification
     *
     * @param pNotification
     *            The notification to send
     * @param pRecipients
     *            The list of recipients' emails
     */
    void send(Notification pNotification, String[] pRecipients);
}
