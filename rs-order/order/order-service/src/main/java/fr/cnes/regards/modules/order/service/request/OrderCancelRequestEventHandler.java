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
package fr.cnes.regards.modules.order.service.request;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.order.amqp.input.OrderCancelRequestDtoEvent;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.job.OrderJobPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * This handler creates {@link CancelOrderJob} from the receiving of {@link OrderCancelRequestDtoEvent}s
 *
 * @author Stephane Cortine
 */
@Component
@MultitenantTransactional
public class OrderCancelRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderCancelRequestDtoEvent> {

    private final ISubscriber subscriber;

    private final JobInfoService jobInfoService;

    private final Validator validator;

    private final IPublisher publisher;

    private final IOrderService orderService;

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public OrderCancelRequestEventHandler(@Value("${regards.order.cancel.request.bulk.size:1000}") int bulkSize,
                                          ISubscriber subscriber,
                                          Validator validator,
                                          IPublisher publisher,
                                          JobInfoService jobInfoService,
                                          IOrderService orderService) {
        this.bulkSize = bulkSize;
        this.subscriber = subscriber;
        this.validator = validator;
        this.publisher = publisher;
        this.jobInfoService = jobInfoService;
        this.orderService = orderService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(OrderCancelRequestDtoEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Class<OrderCancelRequestDtoEvent> getMType() {
        return OrderCancelRequestDtoEvent.class;
    }

    @Override
    public void handleBatch(List<OrderCancelRequestDtoEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Handling {} OrderCancelRequestDtoEvents", events.size());

        List<String> validCorrelationIds = validateEvents(events);

        if (!validCorrelationIds.isEmpty()) {
            // Find all orders in db with list of correlation identifiers and
            // with all status of order, except deleted status.
            List<Order> orders = orderService.findByCorrelationIdsAndStatus(validCorrelationIds,
                                                                            EnumSet.complementOf(EnumSet.of(OrderStatus.DELETED)));

            JobInfo jobInfo = new JobInfo(false,
                                          OrderJobPriority.CANCEL_ORDER_JOB_PRIORITY,
                                          Set.of(new JobParameter(CancelOrderJob.ORDER, orders)),
                                          null,
                                          CancelOrderJob.class.getName());
            jobInfo = jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("Created 1 CancelOrderJob with id [{}] from {} Orders. Handled in " + "{}ms.",
                         jobInfo.getId(),
                         orders.size(),
                         System.currentTimeMillis() - start);
        }
    }

    private List<String> validateEvents(List<OrderCancelRequestDtoEvent> events) {
        List<String> validCorrelationIds = new ArrayList<>();
        events.forEach(event -> {
            Errors errors = new MapBindingResult(new HashMap<>(), OrderCancelRequestDtoEvent.class.getName());
            // Validate request
            validator.validate(event, errors);

            if (errors.hasErrors()) {
                String errorsFormatted = ErrorTranslator.getErrorsAsString(errors);
                LOGGER.error("Correlation identifier [] not valid. Cause : ",
                             event.getCorrelationId(),
                             errorsFormatted);
            } else {
                validCorrelationIds.add(event.getCorrelationId());
            }
        });
        return validCorrelationIds;
    }

    @Override
    public Errors validate(OrderCancelRequestDtoEvent event) {
        Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
        // Send message to DLQ if correlation id not provided
        if (event.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "requestDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        return errors;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

}
