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
package fr.cnes.regards.modules.ingest.service.request;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Ingest request service
 *
 * @author Marc SORDI
 *
 */
public interface IIngestRequestService {

    /**
     * Schedule a job with following passed requests.
     * <b>Ingest requests must be linked to the chain. No additional control is done!</b>
     * @param chainName related processing chain
     * @param requests requests to handle
     */
    void scheduleIngestProcessingJobByChain(String chainName, Collection<IngestRequest> requests);

    /**
     * Handle job error
     */
    void handleJobCrash(JobEvent jobEvent);

    /**
     * Load a collection of requests
     */
    List<IngestRequest> loadByIds(Set<Long> ids);

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

    void fromWaitingTo(Collection<IngestRequest> requests, VersioningMode versioningMode);
}