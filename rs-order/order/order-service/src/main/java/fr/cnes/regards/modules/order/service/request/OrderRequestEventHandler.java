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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.OrderErrorType;
import fr.cnes.regards.modules.order.service.job.OrderJobPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This handler creates {@link CreateOrderJob} from the receiving of {@link OrderRequestDtoEvent}s
 *
 * @author Iliana Ghazali
 */
@Component
public class OrderRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<OrderRequestDtoEvent> {

    private final ISubscriber subscriber;

    private final JobInfoService jobInfoService;

    private final Validator validator;

    private final IPublisher publisher;

    private final IProjectUsersClient projectUsersClient;

    /**
     * Cache to indicates if a given user login (email) is a valid REGARDS user or not.
     */
    private final Cache<String, Boolean> regardsUsers = Caffeine.newBuilder()
                                                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                                                .maximumSize(100)
                                                                .build();

    /**
     * Bulk size limit to handle messages
     */
    private final int bulkSize;

    public OrderRequestEventHandler(@Value("${regards.order.request.bulk.size:1000}") int bulkSize,
                                    ISubscriber subscriber,
                                    JobInfoService jobInfoService,
                                    Validator validator,
                                    IPublisher publisher,
                                    IProjectUsersClient projectUsersClient) {
        this.bulkSize = bulkSize;
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.validator = validator;
        this.publisher = publisher;
        this.projectUsersClient = projectUsersClient;
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

        List<OrderRequestDtoEvent> validEvents = denyInvalidMessages(events);
        if (validEvents.size() != events.size()) {
            LOGGER.warn("{} OrderRequestEvents denied.", events.size() - validEvents.size());
        }
        if (!validEvents.isEmpty()) {
            JobInfo jobInfo = new JobInfo(false,
                                          OrderJobPriority.CREATE_ORDER_JOB_PRIORITY,
                                          Set.of(new JobParameter(CreateOrderJob.ORDER_REQUEST_EVENT, validEvents)),
                                          null,
                                          CreateOrderJob.class.getName());
            jobInfo = jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("Created 1 CreateOrderJob with id {} from {} OrderRequestDtoEvents. Handled in {}ms.",
                         jobInfo.getId(),
                         validEvents.size(),
                         System.currentTimeMillis() - start);
        }
    }

    @Override
    public Errors validate(OrderRequestDtoEvent event) {
        Errors errors = new MapBindingResult(new HashMap<>(), event.getClass().getName());
        // Send message to DLQ if correlation id not provided
        if (event.getCorrelationId() == null) {
            errors.rejectValue("correlationId",
                               "requestDto.correlationId.notnull.error.message",
                               "correlationId is mandatory!");
        }
        return errors;
    }

    /**
     * Send denied response for invalid messages and return valid ones.
     */
    public List<OrderRequestDtoEvent> denyInvalidMessages(List<OrderRequestDtoEvent> events) {
        List<OrderRequestDtoEvent> validEvents = new ArrayList<>();

        events.forEach(event -> {
            OrderErrorType errorType = OrderErrorType.INTERNAL_ERROR;
            Errors errors = new MapBindingResult(new HashMap<>(), OrderRequestDtoEvent.class.getName());
            // Validate request
            validator.validate(event, errors);

            // With amqp api, user is mandatory and must be an existing user.
            if (event.getUser() == null) {
                errors.rejectValue("user", OrderErrorType.INVALID_CONTENT.name(), "User should be present");
            } else {
                Boolean isValidUser = regardsUsers.get(event.getUser(), email -> {
                    FeignSecurityManager.asSystem();
                    try {
                        ResponseEntity<EntityModel<ProjectUser>> response = projectUsersClient.retrieveProjectUserByEmail(
                            event.getUser());
                        return response != null && response.getStatusCode() == HttpStatus.OK;
                    } catch (HttpClientErrorException | HttpServerErrorException e) {
                        LOGGER.error(e.getMessage(), e);
                        return false;
                    } finally {
                        FeignSecurityManager.reset();
                    }
                });
                if (!isValidUser) {
                    errorType = OrderErrorType.FORBIDDEN;
                    errors.rejectValue("user", OrderErrorType.FORBIDDEN.name(), "Unknown user : " + event.getUser());
                }
            }

            if (errors.hasErrors()) {
                publisher.publish(OrderResponseDtoEvent.buildDeniedResponse(event, errors, errorType));
            } else {
                validEvents.add(event);
            }
        });
        return validEvents;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

}
