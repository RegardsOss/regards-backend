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

import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;

/**
 * Service to handle {@link AIPStoreMetaDataRequest}s
 *
 * @author Léo Mieulet
 */
public interface IAIPStoreMetaDataRequestService {

    /**
     * Execute given {@link AIPStoreMetaDataRequest}s
     * @param requests
     * @param aipsToStore
     * @param aipsToUpdate
     * @param filesToDelete
     */
    void handle(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToStore, List<AIPEntity> aipsToUpdate,
            List<FileDeletionRequestDTO> filesToDelete);

    /**
     * Schedule new {@link AIPStoreMetaDataRequest}s associated to given {@link AIPEntity}s
     * @param aips list of aips
     * @param removeCurrentMetaData true when a legacy metadata exists and should be removed
     * @param computeChecksum true when the aip does not contains a reliable checksum and should be recomputed
     */
    void schedule(List<AIPEntity> aips, boolean removeCurrentMetaData, boolean computeChecksum);

    /**
     * @param ids a list of request id
     * @return the list of entities
     */
    List<AIPStoreMetaDataRequest> search(List<Long> ids);

    /**
     * Callback when a {@link AIPStoreMetaDataRequest} is terminated successfully.
     * @param request {@link AIPStoreMetaDataRequest}
     * @param requestInfos {@link RequestInfo}s
     */
    void handleSuccess(AIPStoreMetaDataRequest request, Set<RequestInfo> requestInfos);

    /**
     * Callback when a  {@link AIPStoreMetaDataRequest} is terminated with errors.
     * @param requestInfos {@link RequestInfo}s
     */
    void handleError(Set<RequestInfo> requestInfos);
}
