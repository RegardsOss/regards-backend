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
package fr.cnes.regards.modules.feature.service.abort;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.service.request.FeatureRequestService;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.feature.dto.FeatureRequestStep.*;

/**
 * Service to abort running requests. It is a temporary workaround to unblock running requests.
 *
 * @author Iliana Ghazali
 **/
@Service
public class FeatureRequestAbortService {

    // CONSTANTS

    /**
     * Matrix of feature request types and steps that can be aborted. All other requests will be ignored. The
     * corresponding error step is associated to the step to abort as follows Map(TYPE, MAP(STEP_TO_ABORT,
     * CORRESPONDING_ERROR_STEP)).
     */
    public static final Map<FeatureRequestTypeEnum, Map<FeatureRequestStep, FeatureRequestStep>> STEPS_CORRELATION_TABLE = Map.of(
        FeatureRequestTypeEnum.NOTIFICATION,
        Map.of(REMOTE_NOTIFICATION_REQUESTED, REMOTE_NOTIFICATION_ERROR),
        FeatureRequestTypeEnum.CREATION,
        Map.of(REMOTE_STORAGE_REQUESTED, REMOTE_STORAGE_ERROR));

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureRequestAbortService.class);

    // SERVICES

    private final FeatureRequestService featureRequestService;

    private final FeatureSessionNotifier featureSessionNotifier;

    private final int nbRequestsToAbortPerPage;

    private final int abortDelayInHours;

    public FeatureRequestAbortService(FeatureRequestService featureRequestService,
                                      FeatureSessionNotifier featureSessionNotifier,
                                      @Value("${regards.feature.abort.page.size:1000}") int nbRequestsToAbortPerPage,
                                      @Value("${regards.feature.abort.delay.hours:1}") int abortDelayInHours) {
        this.featureRequestService = featureRequestService;
        this.featureSessionNotifier = featureSessionNotifier;
        this.nbRequestsToAbortPerPage = nbRequestsToAbortPerPage;
        this.abortDelayInHours = abortDelayInHours;
    }

    /**
     * Abort a page of requests.
     *
     * @param searchParameters filters provided by the client to search for requests to abort.
     * @param requestType      discriminent type of the feature request.
     * @return response containing the number of requests actually aborted.
     */
    @MultitenantTransactional
    public RequestHandledResponse abortRequests(SearchFeatureRequestParameters searchParameters,
                                                FeatureRequestTypeEnum requestType) {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        long nbAbortedRequests = 0;
        long nbRequestsToAbort = 0;

        if (STEPS_CORRELATION_TABLE.containsKey(requestType)) {
            Pageable page = PageRequest.of(0, nbRequestsToAbortPerPage);
            Page<FeatureRequestDTO> requestsPage;
            boolean hasNext;
            do {
                requestsPage = featureRequestService.findAll(requestType, searchParameters, page);
                if (requestsPage.hasContent()) {
                    nbAbortedRequests += forceErrorStateAndStep(filterRequestsThatCanBeAborted(requestsPage.getContent(),
                                                                                               start), requestType);
                }
                hasNext = requestsPage.hasNext();
                if (hasNext) {
                    page = requestsPage.nextPageable();
                }
            } while (hasNext);
            nbRequestsToAbort = requestsPage.getTotalElements();
        } else {
            LOGGER.warn("Cannot abort requests because the request type '{}' cannot be aborted. Valid abort types : "
                        + "{}.", requestType, STEPS_CORRELATION_TABLE.keySet());
        }

        LOGGER.info("Aborted {} feature requests / {} requested in {} ms",
                    nbAbortedRequests,
                    nbRequestsToAbort,
                    start.until(OffsetDateTime.now(), ChronoUnit.MILLIS));
        return RequestHandledResponse.build(nbRequestsToAbort,
                                            nbAbortedRequests,
                                            getGlobalResponseMessage(nbRequestsToAbort, nbAbortedRequests));
    }

    /**
     * Filter out all requests that cannot be aborted according to their states and registration dates.
     *
     * @param requestsToBeAborted requests retrieved from the database according to the provided search parameters.
     * @param start               starting time of the service transaction.
     */
    private Map<FeatureRequestStep, Set<FeatureRequestDTO>> filterRequestsThatCanBeAborted(List<FeatureRequestDTO> requestsToBeAborted,
                                                                                           OffsetDateTime start) {
        Predicate<FeatureRequestDTO> validAbortPredicate = requestDto -> {
            OffsetDateTime requestRegistrationDate = requestDto.getRegistrationDate();
            boolean isValidAbortState = requestDto.getState() == RequestState.GRANTED
                                        && requestRegistrationDate.plusHours(abortDelayInHours).isBefore(start);
            if (!isValidAbortState) {
                LOGGER.warn("Cannot abort request with id '{}' because the state '{}' and/or the minimal required "
                            + "delay '{}' compared to current time '{}' are not valid.",
                            requestDto.getId(),
                            requestDto.getState(),
                            requestRegistrationDate.plusHours(abortDelayInHours),
                            start);
            }
            return isValidAbortState;
        };
        return requestsToBeAborted.stream()
                                  .filter(validAbortPredicate)
                                  .collect(Collectors.groupingBy(FeatureRequestDTO::getStep,
                                                                 Collectors.mapping(featureDto -> featureDto,
                                                                                    Collectors.toSet())));
    }

    /**
     * Abort requests by setting their states and steps to ERROR and the corresponding error step.
     * A request can only be aborted if :
     * <ul>
     *     <li>the type has a match in the {@link FeatureRequestAbortService#STEPS_CORRELATION_TABLE},</li>
     *     <li>the minimal required delay before aborting the request is met,</li>
     *     <li>the state is {@link RequestState#GRANTED},</li>
     *     <li>the step has a match in the {@link FeatureRequestAbortService#STEPS_CORRELATION_TABLE}.</li>
     * </ul>
     *
     * @param requestsByStep requests to cancel grouped by {@link FeatureRequestStep}.
     * @param requestType    type of request. Feature requests are handled by type.
     * @return the number of requests actually aborted.
     */
    private int forceErrorStateAndStep(Map<FeatureRequestStep, Set<FeatureRequestDTO>> requestsByStep,
                                       FeatureRequestTypeEnum requestType) {
        int nbRequestsUpdated = 0;
        // Abort requests by setting their states and steps to ERROR and the corresponding error step
        for (Map.Entry<FeatureRequestStep, Set<FeatureRequestDTO>> entry : requestsByStep.entrySet()) {
            FeatureRequestStep step = entry.getKey();
            Set<FeatureRequestDTO> featureDtos = entry.getValue();
            Set<Long> ids = featureDtos.stream().map(FeatureRequestDTO::getId).collect(Collectors.toSet());
            // check if the step can be aborted
            FeatureRequestStep mappedStepToUpdate = STEPS_CORRELATION_TABLE.get(requestType).get(step);
            if (mappedStepToUpdate != null) {
                featureRequestService.updateRequestStateAndStep(ids, RequestState.ERROR, mappedStepToUpdate);
                updateSourceAndSession(featureDtos, requestType);
                nbRequestsUpdated += featureDtos.size();
            } else {
                LOGGER.warn("Cannot abort {} requests because their step '{}' cannot be aborted. Allowed steps "
                            + "to be aborted for request type '{}' are : {}. Not aborted request ids: {}.",
                            ids.size(),
                            step,
                            requestType,
                            STEPS_CORRELATION_TABLE.get(requestType).keySet(),
                            ids);
            }
        }
        LOGGER.trace("Aborted page of {} feature requests with step and ids : {} ", nbRequestsUpdated, requestsByStep);

        return nbRequestsUpdated;
    }

    /**
     * Update source and session counts for monitoring purposes, as the requests aborted were forced to ERROR state.
     */
    private void updateSourceAndSession(Set<FeatureRequestDTO> requestsAborted, FeatureRequestTypeEnum requestType) {
        Map<String, Map<String, Long>> sessionsBySource = requestsAborted.stream()
                                                                         .filter(request -> request.getSource() != null
                                                                                            && request.getSession()
                                                                                               != null)
                                                                         .collect(Collectors.groupingBy(
                                                                             FeatureRequestDTO::getSource,
                                                                             Collectors.groupingBy(FeatureRequestDTO::getSession,
                                                                                                   Collectors.counting())));
        sessionsBySource.forEach((source, sessions) -> sessions.forEach((session, numberOfFeaturesAborted) -> {
            switch (requestType) {
                case NOTIFICATION -> {
                    featureSessionNotifier.decrementCount(source,
                                                          session,
                                                          FeatureSessionProperty.RUNNING_NOTIFY_REQUESTS,
                                                          numberOfFeaturesAborted);
                    featureSessionNotifier.incrementCount(source,
                                                          session,
                                                          FeatureSessionProperty.IN_ERROR_NOTIFY_REQUESTS,
                                                          numberOfFeaturesAborted);
                }
                case CREATION -> {
                    featureSessionNotifier.decrementCount(source,
                                                          session,
                                                          FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS,
                                                          numberOfFeaturesAborted);
                    featureSessionNotifier.incrementCount(source,
                                                          session,
                                                          FeatureSessionProperty.IN_ERROR_REFERENCING_REQUESTS,
                                                          numberOfFeaturesAborted);
                }
                default -> { // do nothing other cases are not yet possible
                }
            }
        }));
    }

    private String getGlobalResponseMessage(long nbRequestsToAbort, long nbAbortedRequests) {
        String message = null;
        if (nbRequestsToAbort != nbAbortedRequests) {
            message = "WARNING: the number of requests to abort is different from the number of requests actually "
                      + "aborted. It means that some requests could not be aborted either because their types or steps "
                      + "are not valid. Please see the logs to have more information on the matter.";
        }
        return message;
    }
}
