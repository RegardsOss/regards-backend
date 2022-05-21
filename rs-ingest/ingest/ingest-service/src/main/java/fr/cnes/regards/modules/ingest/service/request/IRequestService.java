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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Léo Mieulet
 */
public interface IRequestService {

    void handleRemoteStoreError(AbstractRequest requests);

    void handleRemoteStoreSuccess(AbstractRequest requests);

    /**
     * Handle request granted from storage service
     */
    void handleRemoteRequestGranted(Set<RequestInfo> requests);

    /**
     * Retrieve all requests referencing the provided group id
     */
    List<AbstractRequest> findRequestsByGroupIdIn(List<String> groupIds);

    /**
     * Retrieve all requests matching provided criteria
     *
     * @param filters
     * @param pageable
     * @return a page of entities
     */
    Page<AbstractRequest> findRequests(SearchRequestsParameters filters, Pageable pageable);

    /**
     * Retrieve all requests matching provided criteria
     *
     * @param filters
     * @param pageable
     * @return a page of DTO entities
     */
    Page<RequestDto> findRequestDtos(SearchRequestsParameters filters, Pageable pageable);

    /**
     * Delete all requests linked to provided aips
     *
     * @param aipsRelatedToSip
     */
    void deleteAllByAip(Set<AIPEntity> aipsRelatedToSip);

    /**
     * Save provided requests into the repository
     * If requests cannot be run right now, their status will change to pending
     *
     * @param requests of the same type. Can concern several sessions
     * @return number of scheduled requests
     */
    int scheduleRequests(List<AbstractRequest> requests);

    /**
     * Save provided request into the repository
     * If the request cannot be run right now, the request status will change to pending
     *
     * @param request the request to save
     * @return
     * @deprecated Use {@link #scheduleRequests(List)} instead to improve performances.
     */
    @Deprecated
    AbstractRequest scheduleRequest(AbstractRequest request);

    /**
     * Check the given request is runnable or should  be delayed.
     *
     * @param request
     * @return
     */
    public boolean shouldDelayRequest(AbstractRequest request);

    /**
     * Abort every {@link fr.cnes.regards.modules.ingest.domain.request.InternalRequestState#RUNNING}. <br>
     * This is an asynchronous method. So tenant has to be given in order to be able to do database queries.
     *
     * @param tenant
     */
    @Async
    void abortRequests(String tenant);

    /**
     * Allows to abort request page by page and save the process of abortion per page and stop jobs at the end of
     * each page and not at the end of everything
     *
     * @param filters
     * @param pageRequest
     * @param jobIdsAlreadyStopped this parameters should initially be empty and then reused between each page handling
     * @return next page to treat
     */
    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    Page<AbstractRequest> abortCurrentRequestPage(SearchRequestsParameters filters,
                                                  Pageable pageRequest,
                                                  Set<UUID> jobIdsAlreadyStopped);

    /**
     * Fetch a page of requests and try to unblock them
     *
     * @param requestType the type of requests to retrieve and unblock, if possible
     */
    void unblockRequests(RequestTypeEnum requestType);

    /**
     * Associate a job to a {@link AbstractRequest}
     *
     * @param request the request that will start shortly
     *                must be a request type that needs to be run by jobs
     */
    void scheduleJob(AbstractRequest request);

    /**
     * Schedule a job to delete all requests matching provided filters
     *
     * @param filters
     */
    void scheduleRequestDeletionJob(SearchRequestsParameters filters);

    /**
     * Schedule a job to retry all requests matching provided filters from {@link fr.cnes.regards.modules.ingest.domain.request.InternalRequestState} ERROR to CREATED
     *
     * @param filters
     */
    void scheduleRequestRetryJob(SearchRequestsParameters filters);

    void switchRequestState(AbstractRequest request);

    void deleteRequests(Collection<AbstractRequest> requests);

    /**
     * Delete the provided {@link AbstractRequest}, ensure related jobs are unlocked
     *
     * @param request the request to delete
     */
    void deleteRequest(AbstractRequest request);

    boolean isJobRequest(AbstractRequest request);

    /**
     * Retrieve {@link AbstractRequest}s associated to the given storage respones associated by groupId.
     */
    public List<AbstractRequest> getRequests(Set<RequestInfo> requestInfos);
}
