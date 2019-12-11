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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * OAIS Deletion process service.
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 *
 */
public interface IOAISDeletionService {

    /**
     * Search a {@link OAISDeletionCreatorRequest} by is id.
     * @param requestId
     * @return {@link OAISDeletionCreatorRequest}
     */
    public Optional<OAISDeletionCreatorRequest> searchCreator(Long requestId);

    /**
     * Search {@link OAISDeletionCreatorRequest}s by ids.
     * @param deleteRequestIds
     * @return {@link OAISDeletionCreatorRequest}
     */
    public List<OAISDeletionRequest> searchRequests(List<Long> deleteRequestIds);

    /**
     * Delete given request from repository
     * @param request
     */
    public void deleteRequest(OAISDeletionRequest request);

    /**
     * Delete given requests from repository
     * @param request
     */
    public void deleteRequests(Collection<OAISDeletionRequest> requests);

    /**
     * Update given request
     * @param request
     */
    public void update(OAISDeletionRequest request);

    /**
     * Register deletion request from flow item
     * @param request to register as deletion request
     */
    public void registerOAISDeletionCreator(OAISDeletionPayloadDto request);

    /**
     * Try to schedule the deletion job
     * @param deletionRequest
     */
    public void scheduleOAISDeletionCreatorJob(OAISDeletionCreatorRequest deletionRequest);

    /**
     * Handle file deletion error
     */
    void handleRemoteDeleteError(Set<RequestInfo> requestInfos);

    /**
     * Handle file deletion success
     */
    void handleRemoteDeleteSuccess(Set<RequestInfo> requestInfos);
}
