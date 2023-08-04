/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp;

/**
 * Data of metadata property of notification request event
 * {@link fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent}
 *
 * @author Stephane Cortine
 */
public class NotificationEventMetadata {

    private final String recipientId;

    private final Integer priority;

    private final String messageType;

    public NotificationEventMetadata(String recipientId, Integer priority, String messageType) {
        this.recipientId = recipientId;
        this.priority = priority;
        this.messageType = messageType;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getMessageType() {
        return messageType;
    }
}
