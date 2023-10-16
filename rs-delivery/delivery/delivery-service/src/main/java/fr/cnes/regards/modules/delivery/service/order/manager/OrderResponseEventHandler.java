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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handle amqp messages {@link OrderResponseDtoEvent}s to update delivery request
 * {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest} in database and if necessary to send
 * amqp messages {@link DeliveryResponseDtoEvent}.
 *
 * @author Stephane Cortine
 */
@Component
public class OrderResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderResponseDtoEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderResponseEventHandler.class);

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final DeliveryFromOrderService deliveryFromOrderService;

    private final Validator validator;

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public OrderResponseEventHandler(@Value("${regards.order.response.bulk.size:1000}") int bulkSize,
                                     ISubscriber subscriber,
                                     IPublisher publisher,
                                     DeliveryFromOrderService deliveryFromOrderService,
                                     Validator validator) {
        this.bulkSize = bulkSize;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.deliveryFromOrderService = deliveryFromOrderService;
        this.validator = validator;
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
    public Errors validate(OrderResponseDtoEvent event) {
        Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
        // Send message to DLQ if correlation id not provided
        if (event.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "OrderResponseDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        return errors;
    }

    /**
     * Verify the integrity of {@link OrderResponseDtoEvent}s received and handle events in error separately.
     *
     * @param events sent by rs-order.
     * @return valid events
     */
    private List<OrderResponseDtoEvent> validateEvents(List<OrderResponseDtoEvent> events) {
        List<OrderResponseDtoEvent> orderResponseEvtsInError = new ArrayList<>();
        List<DeliveryResponseDtoEvent> deliveryResponseEvtsInError = new ArrayList<>();
        // handle invalid events and send error responses
        events.forEach(event -> {
            Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
            // validate model
            validator.validate(event, errors);
            // build errors from message if any
            if (errors.hasErrors()) {
                orderResponseEvtsInError.add(event);
                String errorsFormatted = ErrorTranslator.getErrorsAsString(errors);
                LOGGER.error("""
                                 Errors were detected while validating OrderResponseDtoEvent with correlation id "{}".
                                 The request is therefore DENIED and will not be processed.
                                 Refer to the DeliveryResponseDtoEvent response for more information.
                                 List of errors detected:
                                 {}""", event.getCorrelationId(), errorsFormatted);

                deliveryResponseEvtsInError.add(new DeliveryResponseDtoEvent(event.getCorrelationId(),
                                                                             DeliveryRequestStatus.DENIED,
                                                                             DeliveryErrorType.INTERNAL_ERROR,
                                                                             errorsFormatted,
                                                                             null,
                                                                             null,
                                                                             event.getOriginRequestAppId().orElse(null),
                                                                             event.getOriginRequestPriority()
                                                                                  .orElse(null)));
            }
        });
        publisher.publish(deliveryResponseEvtsInError);
        // only keep valid events
        events.removeAll(orderResponseEvtsInError);
        return events;
    }

    @Override
    public void handleBatch(List<OrderResponseDtoEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Handling {} OrderResponseEvents", events.size());

        List<DeliveryResponseDtoEvent> deliveryResponseEvts = deliveryFromOrderService.updateDeliveryRequestFromOrderResponseEvt(
            validateEvents(events));

        publisher.publish(deliveryResponseEvts);

        LOGGER.info("{} delivery response events created from {} order response events. Handled in {}ms.",
                    deliveryResponseEvts.size(),
                    events.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

}
