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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.StoreLocation;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPStorageService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

/**
 * Manage {@link AIPStoreMetaDataRequest} entities
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AIPStoreMetaDataRequestService implements IAIPStoreMetaDataRequestService {

    @Autowired
    private IAIPStoreMetaDataRepository aipStoreMetaDataRepository;

    @Autowired
    private IAIPStorageService aipStorageService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IRequestService requestService;

    @Override
    public void handle(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToUpdate,
            List<FileDeletionRequestDTO> filesToDelete) {
        List<String> requestIds = new ArrayList<>();
        try {
            // Store AIPs meta data requests
            requestIds.addAll(aipStorageService.storeAIPs(requests));
        } catch (ModuleException e) {
            e.printStackTrace();
        }

        // Link to requests the group id of the request sent to storage
        for (AIPStoreMetaDataRequest request : requests) {
            if (request.getState() != InternalRequestState.ERROR) {
                // Register request info to identify storage callback events
                request.setRemoteStepGroupIds(requestIds);
            }
        }

        // Save request status
        aipStoreMetaDataRepository.saveAll(requests);
        // Save AIPs
        aipService.saveAll(aipsToUpdate);

        // Publish event to delete legacy AIPs manifest
        if (!filesToDelete.isEmpty()) {
            storageClient.delete(filesToDelete);
        }
    }

    @Override
    public void schedule(List<AIPEntity> aips, Set<StorageMetadata> storages, boolean removeCurrentMetaData,
            boolean computeChecksum) {
        Set<StoreLocation> storeLocations = aipStorageService.getManifestStoreLocationsByStorageMetadata(storages);
        for (AIPEntity aip : aips) {
            scheduleRequest(aip, storeLocations, removeCurrentMetaData, computeChecksum);
        }
    }

    @Override
    public void schedule(AIPEntity aip, Set<OAISDataObjectLocation> storages, boolean removeCurrentMetaData,
            boolean computeChecksum) {
        Set<StoreLocation> manifestStorages = aipStorageService.getManifestStoreLocationsByLocation(storages);
        scheduleRequest(aip, manifestStorages, removeCurrentMetaData, computeChecksum);
    }

    private void scheduleRequest(AIPEntity aip, Set<StoreLocation> storages, boolean removeCurrentMetaData,
            boolean computeChecksum) {
        requestService
                .scheduleRequest(AIPStoreMetaDataRequest.build(aip, storages, removeCurrentMetaData, computeChecksum));
    }

    @Override
    public void handleSuccess(AIPStoreMetaDataRequest request, RequestInfo requestInfo) {
        // Update the manifest, save manifest location and update storages list
        aipStorageService.updateAIPsContentInfosAndLocations(Lists.newArrayList(request.getAip()),
                                                             requestInfo.getSuccessRequests());
        // Save the AIP
        aipService.save(request.getAip());
        sessionNotifier.productMetaStoredSuccess(request.getAip());

        // Delete the request
        aipStoreMetaDataRepository.delete(request);
    }

    @Override
    public void handleError(AIPStoreMetaDataRequest request, RequestInfo requestInfo) {
        request.setErrors(requestInfo.getErrorRequests().stream().map(r -> r.getErrorCause())
                .collect(Collectors.toSet()));
        request.setState(InternalRequestState.ERROR);
        aipStoreMetaDataRepository.save(request);
        sessionNotifier.productMetaStoredError(request.getAip());
    }

    @Override
    public List<AIPStoreMetaDataRequest> search(List<Long> ids) {
        return aipStoreMetaDataRepository.findAllById(ids);
    }
}
