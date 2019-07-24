/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.notification.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.amqp.IPublisherContract;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * An implementation of the notification client using asynchronous messaging
 *
 * @author Marc SORDI
 *
 */
@Service
public abstract class AbstractNotificationPublisher implements INotificationClientContract {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void notify(String message, String title, NotificationLevel level, MimeType mimeType, DefaultRole... roles) {
        publish(new NotificationDtoBuilder(message, title, level, applicationName).withMimeType(mimeType)
                        .toRoles(Arrays.stream(roles).map(Enum::name).collect(Collectors.toSet())));
    }

    @Override
    public void notify(String message, String title, NotificationLevel level, MimeType mimeType, String... users) {
        publish(new NotificationDtoBuilder(message, title, level, applicationName).withMimeType(mimeType)
                        .toUsers(Arrays.stream(users).collect(Collectors.toSet())));
    }

    @Override
    public void notify(String message, String title, NotificationLevel level, MimeType mimeType, String user,
            DefaultRole... roles) {
        publish(new NotificationDtoBuilder(message, title, level, applicationName).withMimeType(mimeType)
                        .toRolesAndUsers(Arrays.stream(roles).map(Enum::name).collect(Collectors.toSet()),
                                         new HashSet<>(Arrays.asList(user))));
    }

    private void publish(NotificationDTO notification) {
        getPublisher().publish(NotificationEvent.build(notification));
    }

    protected abstract IPublisherContract getPublisher();
}
