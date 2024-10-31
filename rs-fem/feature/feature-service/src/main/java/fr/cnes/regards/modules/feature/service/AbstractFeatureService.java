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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.Errors;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */
public abstract class AbstractFeatureService<R extends AbstractFeatureRequest> implements IAbstractFeatureService<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureService.class);

    protected static final int MAX_PAGE_TO_DELETE = 50;

    @Value("${regards.fem.requests.retry.max.page:50}")
    protected int MAX_PAGE_TO_RETRY;

    @Value("${regards.fem.requests.retry.max.entity.per.page:2000}")
    protected int MAX_ENTITY_PER_PAGE;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    protected IFeatureEntityRepository featureEntityRepository;

    @Override
    public void validateRequest(AbstractRequestEvent event, Errors errors) {
        if (!event.hasRequestId()) {
            errors.reject("missing.request.id.header", "Missing request id header");
        }
        if (!event.hasRequestDate()) {
            errors.reject("missing.request.date.header", "Missing request date header");
        }
        if (!event.hasRequestOwner()) {
            errors.reject("missing.request.owner.header", "Missing request owner header");
        }
    }

    @Override
    public Optional<FeatureRequestEvent> buildNotConvertedDeniedResponse(Message message, String errorMessage) {
        String requestId = AbstractRequestEvent.getRequestId(message.getMessageProperties());
        if (requestId == null) {
            LOGGER.error("AMQP Message with tag {} cannot be processed because its requestId is null",
                         message.getMessageProperties().getDeliveryTag());
            return Optional.empty();
        }

        String requestOwner = AbstractRequestEvent.getRequestOwner(message.getMessageProperties());
        // Monitoring log
        logRequestDenied(requestOwner, requestId, Sets.newHashSet(errorMessage));
        // Build DENIED request
        return Optional.of(FeatureRequestEvent.build(getRequestType(),
                                                     requestId,
                                                     requestOwner,
                                                     null,
                                                     null,
                                                     RequestState.DENIED,
                                                     Sets.newHashSet(errorMessage)));
    }

    @Override
    public RequestHandledResponse deleteRequests(SearchFeatureRequestParameters selection) {
        long nbHandled = 0;
        long total = 0;
        Pageable page = PageRequest.of(0, MAX_ENTITY_PER_PAGE);
        Page<R> requestsPage;
        String message;
        int cpt = 0;
        boolean stop = false;
        // Delete only deletable requests
        selection.withSteps(Arrays.stream(FeatureRequestStep.values()).filter(step -> !step.isProcessing()).toList());

        do {
            requestsPage = findRequests(selection, page);
            if (total == 0) {
                total = requestsPage.getTotalElements();
            }
            sessionInfoUpdateForDelete(requestsPage.toList());
            List<String> storageRequestToCancel = requestsPage.stream()
                                                              .filter(r -> r.getGroupId() != null)
                                                              .map(AbstractFeatureRequest::getGroupId)
                                                              .toList();
            if (!storageRequestToCancel.isEmpty()) {
                storageClient.cancelRequests(storageRequestToCancel);
            }
            getRequestsRepository().deleteAll(requestsPage);
            postRequestDeleted(requestsPage.getContent());
            nbHandled += requestsPage.getNumberOfElements();
            if (!requestsPage.hasNext() || (cpt >= MAX_PAGE_TO_DELETE)) {
                stop = true;
            } else {
                cpt++;
            }
        } while (!stop);
        if (nbHandled < total) {
            message = String.format("All requests has not been handled. Limit of deletable requests (%d) exceeded",
                                    MAX_PAGE_TO_DELETE * MAX_ENTITY_PER_PAGE);
        } else {
            message = "All deletable requested handled";
        }
        return RequestHandledResponse.build(total, nbHandled, message);
    }

    /**
     * Specific action to do after deletion of a list of requests.
     */
    protected abstract void postRequestDeleted(Collection<R> requests);

    @Override
    public RequestHandledResponse retryRequests(SearchFeatureRequestParameters selection) {
        long nbHandled = 0;
        long total = 0;
        int pageCount = 0;
        String message;
        // Sort requests on requestDate to avoid handling same requests multiple times during pagination
        // The request date is updated to now() for each handled request.
        Pageable page = PageRequest.of(0, MAX_ENTITY_PER_PAGE, Sort.by(Sort.Order.asc("requestDate")));
        Page<R> requestsPage;
        boolean stop = false;
        OffsetDateTime startDate = OffsetDateTime.now();
        // Retry only retryable requests
        selection.withSteps(Arrays.stream(FeatureRequestStep.values())
                                  .filter(FeatureRequestStep::isRetryableErrorStep)
                                  .toList());
        // Add search criterion on lastUpdate to handle only previous requests and not new ones.
        if (selection.getLastUpdate() == null
            || selection.getLastUpdate().getBefore() == null
            || selection.getLastUpdate().getBefore().isAfter(startDate)) {
            selection.withLastUpdateBefore(startDate);
        }
        do {
            requestsPage = findRequests(selection, page);
            if (total == 0) {
                total = requestsPage.getTotalElements();
            }
            List<R> toUpdate = requestsPage.map(this::globalUpdateForRetry).toList();
            sessionInfoUpdateForRetry(toUpdate);
            toUpdate = getRequestsRepository().saveAll(toUpdate);
            nbHandled += toUpdate.size();
            if ((requestsPage.getNumber() > pageCount) || nbHandled >= total) {
                stop = true;
            }
            pageCount++;
        } while (!stop);
        if (nbHandled < total) {
            message = String.format("All requests has not been handled. Limit of retryable requests (%d) exceeded",
                                    pageCount * MAX_ENTITY_PER_PAGE);
        } else {
            message = "All retryable requested handled";
        }

        LOGGER.info("UPDATED for retry {} / {}", nbHandled, total);
        return RequestHandledResponse.build(total, nbHandled, message);
    }

    private R globalUpdateForRetry(R request) {
        request.setLastExecErrorStep(request.getStep());
        if (request.getStep() == FeatureRequestStep.REMOTE_NOTIFICATION_ERROR) {
            request.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
        } else {
            request.setStep(FeatureRequestStep.LOCAL_DELAYED);
        }
        request.setRequestDate(OffsetDateTime.now());
        request.setState(RequestState.GRANTED);
        // Reset errors
        request.setErrors(Sets.newHashSet());
        return updateForRetry(request);
    }

    @Override
    public Map<FeatureUniformResourceName, ILightFeatureEntity> getSessionInfoByUrn(Collection<FeatureUniformResourceName> uniformResourceNames) {
        return featureEntityRepository.findLightByUrnIn(uniformResourceNames)
                                      .stream()
                                      .collect(Collectors.toMap(ILightFeatureEntity::getUrn, Function.identity()));
    }

    /**
     * Update given request with an error status and error message
     *
     * @param request    Request to update
     * @param errorCause Error cause message
     */
    protected void addRemoteStorageError(AbstractFeatureRequest request, String errorCause) {
        request.setState(RequestState.ERROR);
        request.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
        request.addError(String.format("Error during file storage : %s", errorCause));
    }

    public abstract Page<R> findRequests(SearchFeatureRequestParameters filters, Pageable page);

    protected abstract IAbstractFeatureRequestRepository<R> getRequestsRepository();

    protected abstract FeatureRequestType getRequestType();

    protected abstract void logRequestDenied(String requestOwner, String requestId, Set<String> errors);

    protected abstract R updateForRetry(R request);

    protected abstract void sessionInfoUpdateForRetry(Collection<R> requests);

    protected abstract void sessionInfoUpdateForDelete(Collection<R> requests);

}
