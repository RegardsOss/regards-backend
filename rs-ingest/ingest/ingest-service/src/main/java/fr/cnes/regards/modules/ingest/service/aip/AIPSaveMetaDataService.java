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

import com.google.common.collect.Lists;
import com.netflix.discovery.converters.Auto;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.AIPSaveMetaDataRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manage {@link AIPSaveMetaDataRequest} entities
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AIPSaveMetaDataService implements IAIPSaveMetaDataService {

    @Autowired
    private AIPSaveMetaDataRepository aipSaveMetaDataRepository;

    @Autowired
    private IAIPStorageService aipStorageService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IStorageClient storageClient;

    @Override
    public void commitJob(List<AIPSaveMetaDataRequest> requests, List<AIPEntity> aipsToStore,
            List<AIPEntity> aipsToUpdate, List<FileDeletionRequestDTO> filesToDelete) {
        String requestId = null;
        try {
            // Store AIPs meta data
            requestId = aipStorageService.storeAIPs(aipsToStore);
        } catch (ModuleException e) {
            e.printStackTrace();
        }

        // Link the request sent to storage to the
        for (AIPSaveMetaDataRequest request : requests) {
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
        List<AIPSaveMetaDataRequest> requests = new ArrayList<>();
        for (AIPEntity aip : aips) {
            requests.add(AIPSaveMetaDataRequest.build(aip, removeCurrentMetaData, computeChecksum));
            sessionNotifier.notifyAIPMetaDataStoring(aip);
        }
        aipSaveMetaDataRepository.saveAll(requests);
    }

    @Override
    public void handleManifestSaved(AIPSaveMetaDataRequest request, Set<RequestInfo> requestInfos) {
        // TODO
        sessionNotifier.notifyAIPMetaDataStored(request.getAip());
        aipSaveMetaDataRepository.delete(request);
    }

    @Override
    public void handleManifestSaveError(Set<RequestInfo> requestInfos) {
        sessionNotifier.notifyAIPMetaDataStoreError(null);
        // TODO
        List<AIPSaveMetaDataRequest> requests = new ArrayList<>();
        for (AIPSaveMetaDataRequest request : requests) {
            request.setErrors(null);
            request.setState(InternalRequestStep.ERROR);
        }
        aipSaveMetaDataRepository.saveAll(requests);
    }

    @Override
    public List<AIPSaveMetaDataRequest> findAllById(List<Long> ids) {
        return aipSaveMetaDataRepository.findAllById(ids);
    }

    @Override
    public List<AIPSaveMetaDataRequest> saveAll(List<AIPSaveMetaDataRequest> requests) {
        return aipSaveMetaDataRepository.saveAll(requests);
    }
}
