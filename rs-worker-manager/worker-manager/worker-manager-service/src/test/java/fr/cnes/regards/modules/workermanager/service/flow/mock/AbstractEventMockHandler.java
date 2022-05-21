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
package fr.cnes.regards.modules.workermanager.service.flow.mock;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractEventMockHandler<T extends ISubscribable>
    implements IBatchHandler<T>, ApplicationListener<ApplicationReadyEvent> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventMockHandler.class);

    private final Class<T> eventType;

    @Autowired
    private ISubscriber subscriber;

    private List<T> events = Lists.newArrayList();

    private Optional<String> queueName;

    private Optional<String> exchangeName;

    private List<Message> rawEvents = Lists.newArrayList();

    protected AbstractEventMockHandler(Class<T> eventType, Optional<String> queueName, Optional<String> exchangeName) {
        this.eventType = eventType;
        this.exchangeName = exchangeName;
        this.queueName = queueName;
    }

    protected AbstractEventMockHandler(Class<T> eventType, String workerType, RequestService requestService) {
        this.eventType = eventType;
        this.exchangeName = Optional.of(requestService.getExchangeName(workerType));
        this.queueName = Optional.of(requestService.getExchangeName(workerType));
    }

    @Override
    public Errors validate(T message) {
        return null;
    }

    @Override
    public void handleBatch(List<T> events) {
        // Nothing to do, work is done in handleBatchWithRaw method
    }

    @Override
    public void handleBatchWithRaw(List<T> messages, List<Message> rawMessages) {
        LOGGER.info("[{}}] {} new message received.", this.getClass().getName(), events.size());
        this.events.addAll(messages);
        this.rawEvents.addAll(rawMessages);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (queueName.isPresent() && exchangeName.isPresent()) {
            subscriber.subscribeTo(this.eventType, this, queueName.get(), exchangeName.get(), true);
        } else {
            subscriber.subscribeTo(this.eventType, this, true);
        }
        reset();
    }

    public Optional<String> getQueueName() {
        return queueName;
    }

    public Optional<String> getExchangeName() {
        return exchangeName;
    }

    public void reset() {
        events.clear();
        rawEvents.clear();
    }

    public Collection<T> getEvents() {
        return events;
    }

    public List<Message> getRawEvents() {
        return rawEvents;
    }
}
