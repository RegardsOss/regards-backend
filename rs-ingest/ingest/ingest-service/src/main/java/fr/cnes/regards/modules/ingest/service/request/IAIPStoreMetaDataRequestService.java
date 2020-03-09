/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

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
    void handle(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToUpdate,
            List<FileDeletionRequestDTO> filesToDelete);

    /**
     * Schedule new {@link AIPStoreMetaDataRequest}s associated to given {@link AIPEntity}s
     * @param aips list of aips
     * @param storages the list of StorageMetadata during the ingestion
     * @param removeCurrentMetaData true when a legacy metadata exists and should be removed
     * @param computeChecksum true when the aip does not contains a reliable checksum and should be recomputed
     */
    void schedule(List<AIPEntity> aips, Set<StorageMetadata> storages, boolean removeCurrentMetaData,
            boolean computeChecksum);

    /**
     * Schedule new {@link AIPStoreMetaDataRequest}s associated to given {@link AIPEntity}s
     * @param aip
     * @param manifestLocations current location where the AIP is store
     * @param removeCurrentMetaData true when a legacy metadata exists and should be removed
     * @param computeChecksum true when the aip does not contains a reliable checksum and should be recomputed
     */
    void schedule(AIPEntity aip, Set<OAISDataObjectLocation> manifestLocations, boolean removeCurrentMetaData,
            boolean computeChecksum);

    /**
     * @param ids a list of request id
     * @return the list of entities
     */
    List<AIPStoreMetaDataRequest> search(List<Long> ids);

    /**
     * Callback when a {@link AIPStoreMetaDataRequest} is terminated successfully.
     * @param request {@link AIPStoreMetaDataRequest}
     * @param requestInfo {@link RequestInfo}
     */
    void handleSuccess(AIPStoreMetaDataRequest request, RequestInfo requestInfo);

    /**
     * Callback when a  {@link AIPStoreMetaDataRequest} is terminated with errors.
     * @param request {@link AIPStoreMetaDataRequest}
     * @param requestInfo {@link RequestInfo}
     */
    void handleError(AIPStoreMetaDataRequest request, RequestInfo requestInfo);
}
