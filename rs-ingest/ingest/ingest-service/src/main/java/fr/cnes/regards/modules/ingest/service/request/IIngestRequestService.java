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

import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestProcessingChainView;

/**
 * @author Marc SORDI
 *
 */
public interface IIngestRequestService {

    /**
     * Schedule ingest processing jobs
     */
    void scheduleIngestProcessingJob();

    void scheduleIngestProcessingJobByChain(IngestProcessingChainView chainView);

    /**
     * Load a collection of requests
     */
    List<IngestRequest> getIngestRequests(Set<Long> ids);

    /**
     * Update a request
     */
    IngestRequest updateIngestRequest(IngestRequest request);

    /**
     * Delete successful request
     * @param request
     */
    void deleteIngestRequest(IngestRequest request);

}