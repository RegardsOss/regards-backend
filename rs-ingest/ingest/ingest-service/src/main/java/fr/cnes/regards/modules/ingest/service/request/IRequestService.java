/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Léo Mieulet
 */
public interface IRequestService {

    void handleRemoteStoreError(Set<RequestInfo> requests);

    void handleRemoteStoreSuccess(Set<RequestInfo> requests);

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
     * @param filters
     * @param pageable
     * @return a page of entities
     */
    Page<RequestDto> searchRequests(SearchRequestsParameters filters, Pageable pageable) throws ModuleException;

    /**
     * Delete all requests linked to provided aips
     * @param aipsRelatedToSip
     */
    void deleteAllByAip(Set<AIPEntity> aipsRelatedToSip);


    /**
     * Save provided requests into the repository
     * If requests cannot be run right now, their status will change to pending
     * @param requests of the same type. Can concern several sessions
     */
    void scheduleRequests(List<AbstractRequest> requests);

    /**
     * Save provided request into the repository
     * If the request cannot be run right now, the request status will change to pending
     * @param request the request to save
     * @return
     */
    AbstractRequest scheduleRequest(AbstractRequest request);
}
