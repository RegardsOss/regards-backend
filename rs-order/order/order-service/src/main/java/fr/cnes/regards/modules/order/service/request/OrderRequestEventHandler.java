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
package fr.cnes.regards.modules.order.service.request;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This handler creates {@link CreateOrderJob} from the receiving of {@link OrderRequestDtoEvent}s
 *
 * @author Iliana Ghazali
 */
@Component
@MultitenantTransactional
public class OrderRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderRequestDtoEvent> {

    private final ISubscriber subscriber;

    private final JobInfoService jobInfoService;

    private final Validator validator;

    private final IPublisher publisher;

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public OrderRequestEventHandler(@Value("${regards.order.request.bulk.size:1000}") int bulkSize,
                                    ISubscriber subscriber,
                                    JobInfoService jobInfoService,
                                    Validator validator,
                                    IPublisher publisher) {
        this.bulkSize = bulkSize;
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.validator = validator;
        this.publisher = publisher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(OrderRequestDtoEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Class<OrderRequestDtoEvent> getMType() {
        return OrderRequestDtoEvent.class;
    }

    @Override
    public void handleBatch(List<OrderRequestDtoEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Handling {} OrderRequestEvents", events.size());
        JobInfo jobInfo = new JobInfo(false,
                                      0,
                                      Set.of(new JobParameter(CreateOrderJob.ORDER_REQUEST_EVENT, events)),
                                      null,
                                      CreateOrderJob.class.getName());
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Created 1 CreateOrderJob with id {} from {} OrderRequestEvents. Handled in {}ms.",
                     jobInfo.getId(),
                     events.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(OrderRequestDtoEvent requestDto) {
        Errors errors = new MapBindingResult(new HashMap<>(), requestDto.getClass().getName());
        validator.validate(requestDto, errors);
        if (requestDto.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "requestDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        if (errors.hasErrors()) {
            publisher.publish(buildDeniedResponse(requestDto, errors));
        }
        return errors;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

    private OrderRequestResponseDtoEvent buildDeniedResponse(OrderRequestDto orderRequest, Errors errors) {
        List<String> errorsFormatted = errors.getFieldErrors()
                                             .stream()
                                             .map(error -> String.format("Error detected on field \"%s\". Cause: "
                                                                         + "\"%s\".",
                                                                         error.getField(),
                                                                         error.getDefaultMessage()))
                                             .toList();
        String errorsConcat = String.join("\n", errorsFormatted);

        LOGGER.error("""
                         Errors were detected while validating OrderRequestDtoEvent with correlation id "{}".
                         The request is therefore DENIED and will not be processed.
                         Refer to the OrderRequestResponseDtoEvent response for more information.
                         List of errors detected:
                         {}""", orderRequest.getCorrelationId(), errorsConcat);

        return new OrderRequestResponseDtoEvent(OrderRequestStatus.DENIED,
                                                null,
                                                orderRequest.getCorrelationId(),
                                                errorsConcat,
                                                null);
    }
}
