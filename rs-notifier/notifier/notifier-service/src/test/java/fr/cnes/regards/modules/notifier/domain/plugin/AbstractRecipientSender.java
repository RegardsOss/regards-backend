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
package fr.cnes.regards.modules.notifier.domain.plugin;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Marc SORDI
 */
public abstract class AbstractRecipientSender<E extends ISubscribable> implements IRecipientNotifier, IHandler<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRecipientSender.class);

    public static final String RECIPIENT_LABEL = "recipientLabelTest";

    private int count = 0;

    @Autowired
    IPublisher publisher;

    abstract E buildEvent(JsonElement element, JsonElement action);

    @Override
    public Collection<NotificationRequest> send(Collection<NotificationRequest> requestsToSend) {
        this.publisher.publish(requestsToSend.stream()
                                             .map(toSend -> buildEvent(toSend.getPayload(), toSend.getMetadata()))
                                             .collect(Collectors.toList()));
        return Collections.EMPTY_LIST;
    }

    @Override
    public void handle(TenantWrapper<E> wrapper) {
        count++;
        LOGGER.debug("{} message(s) received in class {}", count, this.getClass().getName());
    }

    @Override
    public String getRecipientLabel() {
        return AbstractRecipientSender.RECIPIENT_LABEL;
    }

    @Override
    public boolean isAckRequired() {
        return false;
    }

}
