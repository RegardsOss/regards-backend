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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPStorageService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manage {@link AIPStoreMetaDataRequest} entities
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AIPSaveMetaDataRequestService implements IAIPSaveMetaDataRequestService {

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
    public void commitJob(List<AIPStoreMetaDataRequest> requests, List<AIPEntity> aipsToStore,
            List<AIPEntity> aipsToUpdate, List<FileDeletionRequestDTO> filesToDelete) {
        String requestId = null;
        try {
            // Store AIPs meta data
            requestId = aipStorageService.storeAIPs(aipsToStore);
        } catch (ModuleException e) {
            e.printStackTrace();
        }

        // Link the request sent to storage to the
        for (AIPStoreMetaDataRequest request : requests) {
            if (request.getState() != InternalRequestStep.ERROR) {
                // Register request info to identify storage callback events
                request.setRemoteStepGroupIds(Lists.newArrayList(requestId));
            }
        }

        // Save request status
        saveAll(requests);
        // Save AIPs
        aipService.saveAll(aipsToUpdate);

        // Publish event to delete legacy AIPs manifest
        if (!filesToDelete.isEmpty()) {
            storageClient.delete(filesToDelete);
        }
    }
    @Override
    public void scheduleSaveMetaData(List<AIPEntity> aips, boolean removeCurrentMetaData, boolean computeChecksum) {
        List<AIPStoreMetaDataRequest> requests = new ArrayList<>();
        for (AIPEntity aip : aips) {
            requests.add(AIPStoreMetaDataRequest.build(aip, removeCurrentMetaData, computeChecksum));
            sessionNotifier.notifyAIPMetaDataStoring(aip);
        }
        aipStoreMetaDataRepository.saveAll(requests);
    }

    @Override
    public void handleManifestSaved(AIPStoreMetaDataRequest request, Set<RequestInfo> requestInfos) {
        // TODO
        sessionNotifier.notifyAIPMetaDataStored(request.getAip());
        aipStoreMetaDataRepository.delete(request);
    }

    @Override
    public void handleManifestSaveError(Set<RequestInfo> requestInfos) {
        sessionNotifier.notifyAIPMetaDataStoreError(null);
        // TODO
        List<AIPStoreMetaDataRequest> requests = new ArrayList<>();
        for (AIPStoreMetaDataRequest request : requests) {
            request.setErrors(null);
            request.setState(InternalRequestStep.ERROR);
        }
        aipStoreMetaDataRepository.saveAll(requests);
    }

    @Override
    public List<AIPStoreMetaDataRequest> findAllById(List<Long> ids) {
        return aipStoreMetaDataRepository.findAllById(ids);
    }

    @Override
    public List<AIPStoreMetaDataRequest> saveAll(List<AIPStoreMetaDataRequest> requests) {
        return aipStoreMetaDataRepository.saveAll(requests);
    }
}
