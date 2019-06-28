/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Default implementation, doing nothing. This implementation is only used if no other implementation is detected by
 * spring.
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DefaultWorkspaceNotifier implements IWorkspaceNotifier {

    @Autowired
    private IPublisher publisher;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Override
    public void sendErrorNotification(String message, String title, DefaultRole role) {
        Set<String> roles = Sets.newHashSet(role.toString());
        NotificationEvent event = NotificationEvent
                .build(new NotificationDtoBuilder(message, title, NotificationLevel.ERROR, microserviceName)
                               .toRoles(roles));

        // Publish
        publisher.publish(event);
    }

    @Override
    public void sendWarningNotification(String message, String title, DefaultRole role) {
        Set<String> roles = Sets.newHashSet(role.toString());
        NotificationEvent event = NotificationEvent
                .build(new NotificationDtoBuilder(message, title, NotificationLevel.WARNING, microserviceName)
                               .toRoles(roles));

        // Publish
        publisher.publish(event);
    }

}
