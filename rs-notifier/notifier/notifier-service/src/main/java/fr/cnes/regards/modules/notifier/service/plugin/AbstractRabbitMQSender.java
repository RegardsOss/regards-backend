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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Abstract plugin to send notification using AMQP
 *
 * @author Kevin Marchois
 * @author Léo Mieulet
 */
public abstract class AbstractRabbitMQSender implements IRecipientNotifier {

    public static final String EXCHANGE_PARAM_NAME = "exchange";

    public static final String QUEUE_PARAM_NAME = "queueName";

    public static final String RECIPIENT_LABEL_PARAM_NAME = "recipientLabel";

    @Autowired
    private IPublisher publisher;

    @PluginParameter(label = "RabbitMQ exchange name", name = EXCHANGE_PARAM_NAME)
    private String exchange;

    @PluginParameter(label = "RabbitMQ queue name", name = QUEUE_PARAM_NAME, optional = true)
    private String queueName;

    @PluginParameter(label = "Recipient label (must be unique)", name = RECIPIENT_LABEL_PARAM_NAME, optional = true)
    private String recipientLabel;

    public <T> Set<NotificationRequest> sendEvents(List<T> toSend, HashMap<String, Object> headers) {
        this.publisher.broadcastAll(exchange, Optional.ofNullable(queueName), Optional.empty(), Optional.empty(), 0, toSend, headers);

        // if there is an issue with amqp then none of the message will be sent
        return Collections.emptySet();
    }

    @Override
    public String getRecipientLabel() {
        return recipientLabel;
    }

}
