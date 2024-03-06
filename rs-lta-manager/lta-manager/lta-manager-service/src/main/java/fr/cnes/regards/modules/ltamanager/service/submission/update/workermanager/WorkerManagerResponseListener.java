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
package fr.cnes.regards.modules.ltamanager.service.submission.update.workermanager;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.utils.SubmissionResponseDtoUtils;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Update {@link fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest} states following the
 * receiving of worker {@link ResponseEvent}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class WorkerManagerResponseListener
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ResponseEvent> {

    private final WorkerManagerResponseService responseService;

    private final ISubmissionRequestRepository requestRepository;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final Validator validator;

    public WorkerManagerResponseListener(WorkerManagerResponseService responseService,
                                         ISubmissionRequestRepository requestRepository,
                                         ISubscriber subscriber,
                                         IPublisher publisher,
                                         Validator validator) {
        this.responseService = responseService;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.validator = validator;
        this.requestRepository = requestRepository;
    }

    @Override
    public Errors validate(ResponseEvent responseEvent) {
        Errors errors = new MapBindingResult(new HashMap<>(), responseEvent.getClass().getName());
        validator.validate(responseEvent, errors);
        return errors;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ResponseEvent.class, this);
    }

    @Override
    public void handleBatch(List<ResponseEvent> responseEvents) {
        LOGGER.trace("[LTA WORKER RESPONSE EVENT HANDLER] Handling {} RequestEvents...", responseEvents.size());
        long start = System.currentTimeMillis();
        responseService.updateSubmissionRequestState(responseEvents);

        List<SubmissionResponseDtoEvent> errorSubmissionResponseEvents = responseEvents.stream()
                                                                                       .map(this::linkResponseToRequest)
                                                                                       .filter(this::filterByStateAndExistingRequest)
                                                                                       .map(this::buildErrorResponseFromResponseEvent)
                                                                                       .toList();
        if (!errorSubmissionResponseEvents.isEmpty()) {
            errorSubmissionResponseEvents.forEach(r -> LOGGER.debug(r.toString()));
            publisher.publish(errorSubmissionResponseEvents);
        }
        LOGGER.trace("[LTA WORKER RESPONSE EVENT HANDLER] {} RequestEvents handled in {} ms...",
                     responseEvents.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }

    /**
     * Find {@link SubmissionRequest} associated to given {@link ResponseEvent}
     */
    private Pair<ResponseEvent, Optional<SubmissionRequest>> linkResponseToRequest(ResponseEvent response) {
        return Pair.of(response, requestRepository.findSubmissionRequestByCorrelationId(response.getRequestId()));
    }

    /**
     * Filter given Pair of {@link ResponseEvent} and {@link SubmissionRequest} to only return error response
     * associated to existing request.
     */
    private boolean filterByStateAndExistingRequest(Pair<ResponseEvent, Optional<SubmissionRequest>> responseAndRequest) {
        Set<ResponseStatus> finalStatus = EnumSet.of(ResponseStatus.ERROR,
                                                     ResponseStatus.SKIPPED,
                                                     ResponseStatus.INVALID_CONTENT);
        return finalStatus.contains(responseAndRequest.getLeft().getState()) && responseAndRequest.getRight()
                                                                                                  .isPresent();
    }

    /**
     * Build {@link SubmissionResponseDtoEvent} with error status for given {@link ResponseEvent}
     */
    private SubmissionResponseDtoEvent buildErrorResponseFromResponseEvent(Pair<ResponseEvent, Optional<SubmissionRequest>> responseAndRequest) {
        return SubmissionResponseDtoUtils.createEvent(responseAndRequest.getLeft().getRequestId(),
                                                      responseAndRequest.getRight(),
                                                      SubmissionResponseStatus.ERROR,
                                                      SubmissionResponseDtoUtils.buildErrorMessage(new HashSet<>(
                                                          responseAndRequest.getLeft().getMessage())),
                                                      responseAndRequest.getRight()
                                                                        .map(SubmissionRequest::getOriginRequestAppId)
                                                                        .orElse(null),
                                                      responseAndRequest.getRight()
                                                                        .map(SubmissionRequest::getOriginRequestPriority)
                                                                        .orElse(1));
    }
}
