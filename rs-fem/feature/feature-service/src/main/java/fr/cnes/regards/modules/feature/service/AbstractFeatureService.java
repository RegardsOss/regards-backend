/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.Errors;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;

/**
 * @author Marc SORDI
 */
public abstract class AbstractFeatureService<R extends AbstractFeatureRequest> implements IAbstractFeatureService {

    protected static final int MAX_PAGE_TO_DELETE = 50;

    protected static final int MAX_PAGE_TO_RETRY = 50;

    protected static final int MAX_ENTITY_PER_PAGE = 2000;

    @Autowired
    private IPublisher publisher;

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
    public boolean denyMessage(Message message, String errorMessage) {

        String requestId = AbstractRequestEvent.getRequestId(message.getMessageProperties());
        if (requestId == null) {
            return false;
        }

        String requestOwner = AbstractRequestEvent.getRequestOwner(message.getMessageProperties());
        // Monitoring log
        logRequestDenied(requestOwner, requestId, Sets.newHashSet(errorMessage));
        // Publish DENIED request
        publisher.publish(FeatureRequestEvent.build(getRequestType(), requestId, requestOwner, null, null,
                                                    RequestState.DENIED, Sets.newHashSet(errorMessage)));
        return true;
    }

    @Override
    public RequestHandledResponse deleteRequests(FeatureRequestsSelectionDTO selection) {
        long nbHandled = 0;
        long total = 0;
        Pageable page = PageRequest.of(0, MAX_ENTITY_PER_PAGE);
        Page<R> requestsPage;
        String message;
        if ((selection.getFilters() != null) && (selection.getFilters().getState() != null)
                && (selection.getFilters().getState() != RequestState.ERROR)) {
            message = String.format("Requests in state %s are not deletable", selection.getFilters().getState());
        } else {
            int cpt = 0;
            boolean stop = false;
            // Delete only error requests
            selection.getFilters().setState(RequestState.ERROR);
            do {
                requestsPage = findRequests(selection, page);
                if (total == 0) {
                    total = requestsPage.getTotalElements();
                }
                getRequestsRepository().deleteAll(requestsPage);
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
        }
        return RequestHandledResponse.build(total, nbHandled, message);
    }

    @Override
    public RequestHandledResponse retryRequests(FeatureRequestsSelectionDTO selection) {
        long nbHandled = 0;
        long total = 0;
        String message;
        Pageable page = PageRequest.of(0, MAX_ENTITY_PER_PAGE);
        Page<R> requestsPage;
        boolean stop = false;
        if ((selection.getFilters() != null) && (selection.getFilters().getState() != null)
                && (selection.getFilters().getState() != RequestState.ERROR)) {
            message = String.format("Requests in state %s are not retryable", selection.getFilters().getState());
        } else {
            // Retry only error requests
            selection.getFilters().setState(RequestState.ERROR);
            do {
                requestsPage = findRequests(selection, page);
                if (total == 0) {
                    total = requestsPage.getTotalElements();
                }
                List<R> toUpdate = requestsPage.map(this::globalUpdateForRetry).toList();
                toUpdate = getRequestsRepository().saveAll(toUpdate);
                nbHandled += toUpdate.size();
                if ((requestsPage.getNumber() < MAX_PAGE_TO_RETRY) && requestsPage.hasNext()) {
                    page = requestsPage.nextPageable();
                } else {
                    stop = true;
                }
            } while (!stop);
            if (nbHandled < total) {
                message = String.format("All requests has not been handled. Limit of retryable requests (%d) exceeded",
                                        MAX_PAGE_TO_RETRY * MAX_ENTITY_PER_PAGE);
            } else {
                message = "All retryable requested handled";
            }
        }
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

    protected abstract Page<R> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);

    protected abstract IAbstractFeatureRequestRepository<R> getRequestsRepository();

    protected abstract FeatureRequestType getRequestType();

    protected abstract void logRequestDenied(String requestOwner, String requestId, Set<String> errors);

    protected abstract R updateForRetry(R request);
}
