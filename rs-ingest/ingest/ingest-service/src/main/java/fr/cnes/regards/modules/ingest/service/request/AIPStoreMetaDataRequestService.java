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

import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPStoreMetaDataRequestService.class);

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

    @Override
    public void handle(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToUpdate,
            List<FileDeletionRequestDTO> filesToDelete, Table<String, String, Integer> nbManifestRemoved) {
        List<String> requestIds = new ArrayList<>();
        try {
            // Store AIPs meta data requests
            requestIds.addAll(aipStorageService.storeAIPs(requests));
        } catch (ModuleException e) {
            requests.forEach(r -> r.setState(InternalRequestState.ERROR));
            LOGGER.error(e.getMessage(), e);
        }

        // Link to requests the group id of the request sent to storage
        for (AIPStoreMetaDataRequest request : requests) {
            if (request.getState() != InternalRequestState.ERROR) {
                // Register request info to identify storage callback events
                request.setRemoteStepGroupIds(requestIds);
                // Put the request as un-schedule.
                // The answering event from storage will put again the request to be executed
                request.setState(InternalRequestState.TO_SCHEDULE);
            }
        }
        // Monitor all manifest removed
        for (String session : nbManifestRemoved.columnKeySet()) {
            Map<String, Integer> column = nbManifestRemoved.column(session);
            for (String sessionOwner : column.keySet()) {
                Integer nbRemoved = column.get(sessionOwner);
                LOGGER.info("Decrement {} stored meta for session {} - {}", nbRemoved,
                        sessionOwner, session);
                sessionNotifier.decrementMetaStoreSuccess(sessionOwner, session, nbRemoved);
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
    public Collection<AIPStoreMetaDataRequest> createRequests(List<AIPEntity> aips, Set<StorageMetadata> storages,
            boolean removeCurrentMetaData, boolean computeChecksum) {
        Collection<AIPStoreMetaDataRequest> requests = Sets.newHashSet();
        Set<StoreLocation> storeLocations = aipStorageService.getManifestStoreLocationsByStorageMetadata(storages);
        for (AIPEntity aip : aips) {
            requests.add(create(aip, storeLocations, removeCurrentMetaData, computeChecksum));
        }
        return requests;
    }

    @Override
    public AIPStoreMetaDataRequest createRequest(AIPEntity aip, Set<OAISDataObjectLocation> storages,
            boolean removeCurrentMetaData, boolean computeChecksum) {
        // this method being called from a job, it can be interrupted. to enable the action to be done
        // especially the transaction, we use Thread.interrupted() and not Thread.currentThread().isInterrupted().
        boolean interrupted = Thread.interrupted();
        Set<StoreLocation> manifestStorages = aipStorageService.getManifestStoreLocationsByLocation(storages);
        AIPStoreMetaDataRequest req = create(aip, manifestStorages, removeCurrentMetaData, computeChecksum);
        // once the work has been done, we reset the interrupt flag if needed.
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return req;
    }

    private AIPStoreMetaDataRequest create(AIPEntity aip, Set<StoreLocation> storages, boolean removeCurrentMetaData,
            boolean computeChecksum) {
        sessionNotifier.incrementMetaStorePending(aip);
        // FIXME : Handler notification when really scheduled
        return AIPStoreMetaDataRequest.build(aip, storages, removeCurrentMetaData, computeChecksum);
    }

    @Override
    public void handleSuccess(Collection<AIPStoreMetaDataRequest> requests, RequestInfo requestInfo) {
        List<AIPEntity> toSave = Lists.newArrayList();
        for (AIPStoreMetaDataRequest request : requests) {
            // Update the manifest, save manifest location and update storages list
            aipStorageService.updateAIPsContentInfosAndLocations(Lists.newArrayList(request.getAip()),
                                                                 requestInfo.getSuccessRequests());
            // Save the AIP
            toSave.add(request.getAip());
            // Monitoring
            // Decrement from IngestRequestService#finalizeSuccessfulRequest (AIP per AIP)
            sessionNotifier.decrementMetaStorePending(request);
            sessionNotifier.incrementMetaStoreSuccess(request);
        }

        // Save aips
        aipService.saveAll(toSave);
        // Delete the request
        aipStoreMetaDataRepository.deleteAll(requests);
    }

    @Override
    public void handleError(AIPStoreMetaDataRequest request, RequestInfo requestInfo) {
        request.setErrors(requestInfo.getErrorRequests().stream().map(r -> r.getErrorCause())
                .collect(Collectors.toSet()));
        request.setState(InternalRequestState.ERROR);
        aipStoreMetaDataRepository.save(request);
        // Monitoring
        // Decrement from IngestRequestService#finalizeSuccessfulRequest (AIP per AIP)
        sessionNotifier.decrementMetaStorePending(request);
        sessionNotifier.incrementMetaStoreError(request);
    }

    @Override
    public List<AIPStoreMetaDataRequest> search(List<Long> ids) {
        return aipStoreMetaDataRepository.findAllById(ids);
    }
}
