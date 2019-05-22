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
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.util.ErrorHandler;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * @author Marc SORDI
 *
 */
public class RegardsErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsErrorHandler.class);

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final String microserviceName;

    public RegardsErrorHandler(IInstancePublisher instancePublisher, IPublisher publisher, String microserviceName) {
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.microserviceName = microserviceName;
    }

    @Override
    public void handleError(Throwable t) {

        // Try to notify message
        if (ListenerExecutionFailedException.class.isAssignableFrom(t.getClass())) {
            ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) t;
            if (lefe.getFailedMessage() != null) {
                LOGGER.error("AMQP failed message : {}", lefe.getFailedMessage().toString());
                // Message#toString is already handling encoding and content type if possible
                NotificationDtoBuilder notifBuilder = new NotificationDtoBuilder(lefe.getFailedMessage().toString(),
                        String.format("AMQP event has been routed to DLQ"), NotificationLevel.ERROR, microserviceName);
                notifBuilder.withMimeType(MimeTypeUtils.TEXT_PLAIN);
                Set<String> roles = new HashSet<>();
                roles.add(DefaultRole.PROJECT_ADMIN.name());
                instancePublisher.publish(NotificationEvent.build(notifBuilder.toRoles(roles)));
                // TODO retrieve tenant from message!
            }
        } else {
            // TODO
        }

        LOGGER.error("Listener failed - message rerouted to dead letter queue", t);
    }

}
