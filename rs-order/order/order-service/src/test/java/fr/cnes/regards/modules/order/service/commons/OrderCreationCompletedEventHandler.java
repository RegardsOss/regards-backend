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
package fr.cnes.regards.modules.order.service.commons;

import fr.cnes.regards.modules.order.service.OrderCreationService;
import io.vavr.collection.List;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author gandrieu
 **/
@Component
public class OrderCreationCompletedEventHandler
    implements ApplicationListener<OrderCreationService.OrderCreationCompletedEvent> {

    protected final java.util.Queue<OrderCreationService.OrderCreationCompletedEvent> events = new ConcurrentLinkedQueue<>();

    protected Consumer<OrderCreationService.OrderCreationCompletedEvent> consumer = e -> {
    };

    public void setConsumer(Consumer<OrderCreationService.OrderCreationCompletedEvent> consumer) {
        this.consumer = consumer;
    }

    @Override
    public synchronized void onApplicationEvent(OrderCreationService.OrderCreationCompletedEvent event) {
        events.add(event);
        consumer.accept(event);
    }

    public List<OrderCreationService.OrderCreationCompletedEvent> getEvents() {
        return List.ofAll(events);
    }

    public void clear() {
        events.clear();
    }

}
