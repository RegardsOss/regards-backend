/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.client.amqp;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handler to receive {@link OrderResponseDtoEvent}s from orders previously created automatically.
 *
 * @author Iliana Ghazali
 **/
@Service
public class AutoOrderResponseHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderResponseDtoEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoOrderResponseHandler.class);

    private final int batchSize;

    private final IAutoOrderResponseClient listener;

    private final ISubscriber subscriber;

    public AutoOrderResponseHandler(Optional<IAutoOrderResponseClient> optListener,
                                    ISubscriber subscriber,
                                    @Value("${regards.order.client.response.batch.size:500}") int batchSize) {
        this.listener = optListener.orElse(null);
        this.subscriber = subscriber;
        this.batchSize = batchSize;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public Errors validate(OrderResponseDtoEvent message) {
        // no need to check validity of OrderResponseDtoEvents
        return null;
    }

    @Override
    public void handleBatch(List<OrderResponseDtoEvent> messages) {
        LOGGER.debug("[ORDER RESPONSES HANDLER] Handling {} OrderResponseDtoEvent...", messages.size());
        long start = System.currentTimeMillis();
        handleResponses(messages);
        LOGGER.debug("[ORDER RESPONSES HANDLER] {} OrderResponseDtoEvent handled in {} ms.",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    private void handleResponses(List<OrderResponseDtoEvent> events) {
        Map<OrderRequestStatus, List<OrderResponseDtoEvent>> eventsByStatus = events.stream()
                                                                                    .collect(Collectors.groupingBy(
                                                                                        OrderResponseDto::getStatus));
        eventsByStatus.forEach((status, groupedEvents) -> {
            switch (status) {
                case GRANTED -> listener.onOrderGranted(groupedEvents);
                case DENIED -> listener.onOrderDenied(groupedEvents);
                case SUBORDER_DONE -> listener.onSubOrderDone(groupedEvents);
                case DONE -> listener.onOrderDone(groupedEvents);
                case FAILED -> listener.onOrderFailed(groupedEvents);
            }
        });
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(OrderResponseDtoEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect OrderResponseDtoEvent bus messages !");
        }
    }

}
