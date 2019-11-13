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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import java.util.List;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public interface IAIPSaveMetaDataService {

    void commitJob(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToStore,
            List<AIPEntity> aipsToUpdate, List<FileDeletionRequestDTO> filesToDelete);

    /**
     * Create some tasks to save the manifest on storage
     * @param aips list of aips
     * @param removeCurrentMetaData true when a legacy metadata exists and should be removed
     * @param computeChecksum true when the aip does not contains a reliable checksum and should be recomputed
     */
    void scheduleSaveMetaData(List<AIPEntity> aips, boolean removeCurrentMetaData, boolean computeChecksum);

    /**
     * @param ids a list of request id
     * @return the list of entities
     */
    List<AIPStoreMetaDataRequest> findAllById(List<Long> ids);

    /**
     * Save new requests states
     * @param requests
     * @return
     */
    List<AIPStoreMetaDataRequest> saveAll(List<AIPStoreMetaDataRequest> requests);


    void handleManifestSaved(AIPStoreMetaDataRequest request, Set<RequestInfo> requestInfos);

    void handleManifestSaveError(Set<RequestInfo> requestInfos);
}
