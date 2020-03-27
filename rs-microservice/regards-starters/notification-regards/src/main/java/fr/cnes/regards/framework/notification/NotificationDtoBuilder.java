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
package fr.cnes.regards.framework.notification;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link NotificationDTO} builder
 *
 * @author Marc SORDI
 *
 */
public class NotificationDtoBuilder {

    private final NotificationDTO notification;

    public NotificationDtoBuilder(String message, String title, NotificationLevel level, String sender) {
        notification = new NotificationDTO();
        Assert.hasText(message, "Notification message is required");
        notification.setMessage(message);
        Assert.hasText(title, "Notification title is required");
        notification.setTitle(title);
        Assert.notNull(level, "Notification level is required");
        notification.setLevel(level);
        Assert.hasText(sender, "Notification sender is required");
        notification.setSender(sender);
    }

    public NotificationDtoBuilder withMimeType(MimeType mimeType) {
        Assert.notNull(mimeType, "Mime type is required");
        notification.setMimeType(mimeType);
        return this;
    }

    public NotificationDTO toRoles(Set<String> roles) {
        Assert.notEmpty(roles, "At least one role is required");
        notification.setRoleRecipients(roles);
        notification.setProjectUserRecipients(new HashSet<>());
        return notification;
    }

    public NotificationDTO toUsers(Set<String> users) {
        Assert.notEmpty(users, "At least one user is required");
        notification.setRoleRecipients(new HashSet<>());
        notification.setProjectUserRecipients(users);
        return notification;
    }

    public NotificationDTO toRolesAndUsers(Set<String> roles, Set<String> users) {
        Assert.notEmpty(roles, "At least one role is required");
        notification.setRoleRecipients(roles);
        Assert.notEmpty(users, "At least one user is required");
        notification.setProjectUserRecipients(users);
        return notification;
    }

}
