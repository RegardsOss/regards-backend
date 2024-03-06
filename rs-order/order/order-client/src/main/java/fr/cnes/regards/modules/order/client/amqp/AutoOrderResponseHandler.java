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
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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

    private final Validator validator;

    public AutoOrderResponseHandler(@Nullable IAutoOrderResponseClient optListener,
                                    ISubscriber subscriber,
                                    Validator validator,
                                    @Value("${regards.order.client.response.batch.size:1000}") int batchSize) {
        this.listener = optListener;
        this.subscriber = subscriber;
        this.batchSize = batchSize;
        this.validator = validator;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(OrderResponseDtoEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect OrderResponseDtoEvent bus messages !");
        }
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public Errors validate(OrderResponseDtoEvent event) {
        Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
        // Send message to DLQ if correlation id not provided
        if (event.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "orderResponseDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        return errors;
    }

    @Override
    public void handleBatch(List<OrderResponseDtoEvent> events) {
        assert listener != null;
        LOGGER.debug("[ORDER RESPONSES HANDLER] Handling {} order response events", events.size());
        long start = System.currentTimeMillis();

        events = validateEvents(events);
        EnumMap<OrderRequestStatus, List<OrderResponseDtoEvent>> eventsByStatus = events.stream()
                                                                                        .collect(Collectors.groupingBy(
                                                                                            OrderResponseDto::getStatus,
                                                                                            () -> new EnumMap<>(
                                                                                                OrderRequestStatus.class),
                                                                                            Collectors.toList()));
        eventsByStatus.forEach((status, orderResponses) -> {
            switch (status) {
                case GRANTED -> listener.onOrderGranted(orderResponses);
                case DENIED -> listener.onOrderDenied(orderResponses);
                case SUBORDER_DONE -> listener.onSubOrderDone(orderResponses);
                case DONE -> listener.onOrderDone(orderResponses);
                case FAILED -> listener.onOrderFailed(orderResponses);
            }
        });

        LOGGER.debug("[ORDER RESPONSES HANDLER] {} valid order response events handled in {} ms.",
                     events.size(),
                     System.currentTimeMillis() - start);
    }

    /**
     * Verify the integrity of {@link OrderResponseDtoEvent}s received and handle events in error separately.
     *
     * @param events sent by rs-order.
     * @return valid events
     */
    private List<OrderResponseDtoEvent> validateEvents(List<OrderResponseDtoEvent> events) {
        return events.stream().filter(event -> {
            Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
            // validate model
            validator.validate(event, errors);
            // build errors from message if any
            boolean isInvalidEvent = errors.hasErrors();
            if (isInvalidEvent) {
                String errorsFormatted = ErrorTranslator.getErrorsAsString(errors);
                LOGGER.error("""
                                 Errors were detected while validating OrderResponseDtoEvent with correlation id "{}".
                                 The request is therefore DENIED and will not be processed.
                                 List of errors detected:
                                 {}""", event.getCorrelationId(), errorsFormatted);
            }
            // keep only valid event
            return !isInvalidEvent;
        }).toList();
    }

}
