/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;

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
    void handleJobError(JobEvent jobEvent);

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
     * Handle request error during job processing
     */
    void handleRequestError(IngestRequest request, SIPEntity entity);

    /**
     * Handle request success at the end of the job processing and launch remote storage request
     * All LOCAL {@link IngestRequestStep} successfully done.
     */
    void handleRequestSuccess(IngestRequest request, SIPEntity sipEntity, List<AIP> aips);

    /**
     * Handle request granted from storage service
     */
    void handleRemoteRequestGranted(RequestInfo requestInfo);

    /**
     * Handle request denied from storage service
     */
    void handleRemoteRequestDenied(RequestInfo requestInfo);

    /**
     * Handle remote storage success
     */
    void handleRemoteStoreSuccess(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success);

    /**
     * Handle remote storage error
     */
    void handleRemoteStoreError(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success,
            Collection<RequestResultInfoDTO> errors);

    /**
     * Handle remote reference success
     */
    void handleRemoteReferenceSuccess(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success);

    /**
     * Handle remote reference error
     */
    void handleRemoteReferenceError(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success,
            Collection<RequestResultInfoDTO> errors);
}