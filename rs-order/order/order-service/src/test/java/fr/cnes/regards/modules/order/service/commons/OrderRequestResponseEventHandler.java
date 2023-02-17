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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to receive {@link OrderResponseDtoEvent}. Only for testing purposes.
 *
 * @author Iliana Ghazali
 */
@Component
public class OrderRequestResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderResponseDtoEvent> {

    private final ISubscriber subscriber;

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public OrderRequestResponseEventHandler(@Value("${regards.order.request.response.bulk.size:1000}") int bulkSize,
                                            ISubscriber subscriber) {
        this.bulkSize = bulkSize;
        this.subscriber = subscriber;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(OrderResponseDtoEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Class<OrderResponseDtoEvent> getMType() {
        return OrderResponseDtoEvent.class;
    }

    @Override
    public void handleBatch(List<OrderResponseDtoEvent> events) {
        LOGGER.debug("Handling {} OrderRequestResponseDtoEvent", events.size());
    }

    @Override
    public Errors validate(OrderResponseDtoEvent requestDto) {
        return null;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

}
