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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

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
    void handleGrantedRequest(IngestRequest request);

    /**
     * Handle request denied during request handling
     */
    void handleDeniedRequest(IngestRequest request);

    /**
     * Handle request error during job processing
     */
    void handleRequestError(IngestRequest request, SIPEntity entity);

    /**
     * Handle request success at the end of the job processing
     */
    void handleRequestSuccess(IngestRequest request, SIPEntity sipEntity, List<AIP> aips);

}