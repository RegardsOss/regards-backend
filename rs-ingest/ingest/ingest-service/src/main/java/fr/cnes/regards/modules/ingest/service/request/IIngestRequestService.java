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

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ingest request service
 *
 * @author Marc SORDI
 */
public interface IIngestRequestService {

    /**
     * Schedule a job with following passed requests.
     * <b>Ingest requests must be linked to the chain. No additional control is done!</b>
     *
     * @param chainName related processing chain
     * @param requests  requests to handle
     */
    void scheduleIngestProcessingJobByChain(String chainName, Collection<IngestRequest> requests);

    /**
     * Handle job error
     * All requests from that job failed
     *
     * @return true when the job type is managed by this service
     */
    boolean handleJobCrash(JobInfo jobInfo);

    /**
     * Load a collection of requests
     */
    List<IngestRequest> findByIds(Set<Long> ids);

    /**
     * Load all requests with the given providerId
     */
    List<IngestRequest> findByProviderId(String providerId);

    /**
     * Load all requests with {@link InternalRequestState#TO_SCHEDULE} status
     */
    Page<IngestRequest> findToSchedule(Pageable pageable);

    /**
     * Load all requests that could block the creation of a request with a given providerId
     */
    List<IngestRequest> findPotentiallyBlockingRequests(List<String> providerIds);

    /**
     * Handle request granted during request handling
     */
    void handleRequestGranted(IngestRequest request);

    /**
     * Handle request denied during request handling
     */
    void handleRequestDenied(IngestRequest request);

    /**
     * Handle unknown chain while loading job parameters
     */
    void handleUnknownChain(List<IngestRequest> requests);

    /**
     * Handle ingest job start for specified request
     */
    void handleIngestJobStart(IngestRequest request);

    /**
     * Handle request error during job processing
     */
    void handleIngestJobFailed(IngestRequest request, SIPEntity entity, String errorMessage);

    /**
     * Handle request success at the end of the job processing and launch remote storage request
     * All LOCAL {@link IngestRequestStep} successfully done.
     */
    List<AIPEntity> handleIngestJobSucceed(IngestRequest request, SIPEntity sipEntity, List<AIP> aips);

    void requestRemoteStorage(IngestRequest request);

    /**
     * Handle request denied from storage service
     */
    void handleRemoteRequestDenied(Set<RequestInfo> requests);

    /**
     * Handle remote storage success
     */
    void handleRemoteStoreSuccess(Map<RequestInfo, Set<IngestRequest>> requests);

    /**
     * Handle remote storage error
     */
    void handleRemoteStoreError(IngestRequest request, RequestInfo requestInfo);

    /**
     * Handle remote reference success
     */
    void handleRemoteReferenceSuccess(Set<RequestInfo> requests);

    /**
     * Handle remote reference error
     */
    void handleRemoteReferenceError(Set<RequestInfo> requests);

    void ignore(IngestRequest request);

    void waitVersioningMode(IngestRequest request);

    void scheduleRequestWithVersioningMode(ChooseVersioningRequestParameters filters);

    void fromWaitingTo(Collection<AbstractRequest> requests, VersioningMode versioningMode);
}