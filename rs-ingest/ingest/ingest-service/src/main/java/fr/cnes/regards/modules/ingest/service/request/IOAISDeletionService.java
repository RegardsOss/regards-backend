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

import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionJob;
import fr.cnes.regards.modules.storage.client.RequestInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    Optional<OAISDeletionCreatorRequest> searchCreator(Long requestId);

    /**
     * Search {@link OAISDeletionCreatorRequest}s by ids.
     * @param deleteRequestIds
     * @return {@link OAISDeletionCreatorRequest}
     */
    List<OAISDeletionRequest> searchRequests(List<Long> deleteRequestIds);

    /**
     * Register deletion request from flow item
     * @param request to register as deletion request
     */
    void registerOAISDeletionCreator(OAISDeletionPayloadDto request);

    /**
     * Handle file deletion error
     */
    void handleRemoteDeleteError(Set<RequestInfo> requestInfos);

    /**
     * Handle file deletion success
     */
    void handleRemoteDeleteSuccess(Set<RequestInfo> requestInfos);

    /**
     * Delete all OAIS entities related to these criteria
     */
    void runDeletion(Collection<OAISDeletionRequest> requests, OAISDeletionJob oaisDeletionJob);
}
