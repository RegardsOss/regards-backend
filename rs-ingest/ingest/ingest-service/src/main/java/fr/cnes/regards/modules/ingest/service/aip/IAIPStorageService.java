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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.StoreLocation;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Manage AIP storage
 * @author Léo Mieulet
 */
public interface IAIPStorageService {

    /**
     * Store AIPs Files
     * @param aips
     * @param metadata
     * @return file storage event group_id list
     * @throws ModuleException
     */
    List<String> storeAIPFiles(List<AIPEntity> aips, IngestMetadata metadata) throws ModuleException;

    /**
     * Store AIPs
     * @param requests
     * @return group id
     * @throws ModuleException
     */
    List<String> storeAIPs(List<AIPStoreMetaDataRequest> requests) throws ModuleException;

    /**
     * Update provided {@link AIPEntity} aips content info with files metadata
     * @param aips to update
     * @param storeRequestInfos storage events
     */
    void updateAIPsContentInfosAndLocations(List<AIPEntity> aips, Collection<RequestResultInfoDTO> storeRequestInfos);

    /**
     * Update provided {@link AIPEntity} aip with a list of new file storage locations
     * @param aip to update
     * @param storeRequestInfos storage events
     * @return true when aip have been impacted by these events
     */
    AIPUpdateResult addAIPLocations(AIPEntity aip, Collection<RequestResultInfoDTO> storeRequestInfos);

    /**
     * Update provided {@link AIPEntity} aip with a list of removed file storage locations
     * @param aip to update
     * @param storeRequestInfos storage events
     * @return true when aip have been impacted by these events
     */
    AIPUpdateResult removeAIPLocations(AIPEntity aip, Collection<RequestResultInfoDTO> storeRequestInfos);

    /**
     * Remove a list of storage id from the AIP and retrieve the list of events to send
     * @param aip
     * @param removedStorages list of storage metadata that will be removed from the AIP
     * @return the list of events to sent to storage, empty if nothing have been done
     */
    Collection<FileDeletionRequestDTO> removeStorages(AIPEntity aip, List<String> removedStorages);

    /**
     * Extract from a storage metadata list (during ingestion) the list of storage we'll use to save metadata
     * @param storages
     * @return
     */
    Set<StoreLocation> getManifestStoreLocationsByStorageMetadata(Set<StorageMetadata> storages);

    /**
     * Extract from the AIP OAISDataObjectLocations the list of storage already in use to (re)save AIP
     * @param manifestLocations
     * @return
     */
    Set<StoreLocation> getManifestStoreLocationsByLocation(Set<OAISDataObjectLocation> manifestLocations);
}
