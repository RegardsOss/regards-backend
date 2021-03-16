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
package fr.cnes.regards.modules.notifier.service.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;

/**
 * Default plugin notification sender
 * @author Kevin Marchois
 *
 */
@Plugin(author = "REGARDS Team", description = "Default recipient sender", id = RabbitMQSender.PLUGIN_ID,
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class RabbitMQSender implements IRecipientNotifier {

    @Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
    public static class NotificationEvent implements ISubscribable {

        private JsonElement payload;

        private JsonElement metadata;

        public NotificationEvent(NotificationRequest request) {
            this.payload = request.getPayload();
            this.metadata = request.getMetadata();
        }

        public JsonElement getPayload() {
            return payload;
        }

        public void setPayload(JsonElement payload) {
            this.payload = payload;
        }

        public JsonElement getMetadata() {
            return metadata;
        }

        public void setMetadata(JsonElement metadata) {
            this.metadata = metadata;
        }
    }

    public static final String PLUGIN_ID = "RabbitMQSender";

    public static final String EXCHANGE_PARAM_NAME = "exchange";

    public static final String QUEUE_PARAM_NAME = "queueName";

    @Autowired
    private IPublisher publisher;

    @PluginParameter(label = "RabbitMQ exchange name", name = EXCHANGE_PARAM_NAME)
    private String exchange;

    @PluginParameter(label = "RabbitMQ queue name", name = QUEUE_PARAM_NAME, optional = true)
    private String queueName;

    @Override
    public Collection<NotificationRequest> send(Collection<NotificationRequest> requestsToSend) {
        List<NotificationEvent> toSend = requestsToSend.stream().map(NotificationEvent::new)
                .collect(Collectors.toList());
        this.publisher.broadcastAll(exchange, Optional.ofNullable(queueName), 0, toSend, new HashMap<>());
        // if there is an issue with amqp then none of the message will be sent
        return Collections.emptySet();
    }
}
