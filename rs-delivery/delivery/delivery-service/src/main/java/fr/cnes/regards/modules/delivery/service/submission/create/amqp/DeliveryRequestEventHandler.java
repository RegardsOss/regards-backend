/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.submission.create.amqp;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.service.submission.create.DeliveryCreateService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This handler creates requests the creation of orders from {@link DeliveryRequestDtoEvent}.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DeliveryRequestDtoEvent> {

    private final DeliveryCreateService createService;

    private final ISubscriber subscriber;

    private final Validator validator;

    private final IPublisher publisher;

    private final UserVerifyService userVerifyService;

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public DeliveryRequestEventHandler(ISubscriber subscriber,
                                       DeliveryCreateService createService,
                                       Validator validator,
                                       IPublisher publisher,
                                       UserVerifyService userVerifyService,
                                       @Value("${regards.delivery.request.bulk.size:1000}") int bulkSize) {
        this.subscriber = subscriber;
        this.createService = createService;
        this.validator = validator;
        this.publisher = publisher;
        this.userVerifyService = userVerifyService;
        this.bulkSize = bulkSize;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        subscriber.subscribeTo(DeliveryRequestDtoEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Class<DeliveryRequestDtoEvent> getMType() {
        return DeliveryRequestDtoEvent.class;
    }

    @Override
    public Errors validate(DeliveryRequestDtoEvent requestDto) {
        // only verify if correlationId is present at this stage. We don't want to route messages in dlq for any
        // other error.
        Errors errors = new MapBindingResult(new HashMap<>(), requestDto.getClass().getName());
        // Send message to DLQ if correlation id not provided (because correlationId is mandatory to respond to the
        // client)
        if (requestDto.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "requestDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        return errors;
    }

    @Override
    public void handleBatch(List<DeliveryRequestDtoEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Handling {} delivery request events...", events.size());
        List<DeliveryRequestDtoEvent> validEvents = validateBatchEventsAndHandleInvalid(events);
        List<DeliveryResponseDtoEvent> responses = createService.handleDeliveryRequestsCreation(validEvents);
        LOGGER.debug("{} delivery responses created from {} delivery request events. Handled in {}ms.",
                     responses.size(),
                     events.size(),
                     System.currentTimeMillis() - start);
    }

    /**
     * Verify the integrity of {@link DeliveryRequestDtoEvent}s received and handle events in error separately.
     *
     * @param events sent by a client. Can be invalid.
     * @return valid events
     */
    private List<DeliveryRequestDtoEvent> validateBatchEventsAndHandleInvalid(List<DeliveryRequestDtoEvent> events) {
        List<DeliveryRequestDtoEvent> eventsInError = new ArrayList<>(events.size());
        List<DeliveryResponseDtoEvent> errorResponses = new ArrayList<>(events.size());
        // handle invalid events and send error responses
        events.forEach(event -> {
            Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
            // validate model
            validator.validate(event, errors);
            // validate user
            boolean validUser = userVerifyService.isValidUser(event, errors);
            // build errors from message if any
            if (errors.hasErrors()) {
                eventsInError.add(event);
                String errorsFormatted = ErrorTranslator.getErrorsAsString(errors);
                LOGGER.error("""
                                 Errors were detected while validating DeliveryRequestDtoEvent with correlation id "{}".
                                 The request is therefore DENIED and will not be processed.
                                 Refer to the DeliveryResponseDtoEvent response for more information.
                                 List of errors detected:
                                 {}""", event.getCorrelationId(), errorsFormatted);

                errorResponses.add(DeliveryResponseDtoEvent.buildDeniedDeliveryResponseEvent(event,
                                                                                             event.getOriginRequestAppId()
                                                                                                  .orElse(null),
                                                                                             event.getOriginRequestPriority()
                                                                                                  .orElse(null),
                                                                                             computeErrorType(validUser),
                                                                                             errorsFormatted));
            }
        });
        if (!errorResponses.isEmpty()) {
            publisher.publish(errorResponses);
        }
        // only keep valid events
        events.removeAll(eventsInError);
        return events;
    }

    private DeliveryErrorType computeErrorType(boolean validUser) {
        DeliveryErrorType errorType;
        if (!validUser) {
            errorType = DeliveryErrorType.FORBIDDEN;
        } else {
            errorType = DeliveryErrorType.INVALID_CONTENT;
        }
        return errorType;
    }

}
