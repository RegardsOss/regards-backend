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
package fr.cnes.regards.modules.ingest.service.flow;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateFileLocationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.AIPService;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;
import fr.cnes.regards.modules.ingest.service.request.IDeleteRequestService;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.storage.client.IStorageRequestListener;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * This class offers callbacks from storage events
 *
 * @author Marc SORDI
 */
@Component
public class StorageResponseFlowHandler implements IStorageRequestListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageResponseFlowHandler.class);

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IDeleteRequestService deleteRequestService;

    @Autowired
    private AIPUpdateRequestService aipUpdateRequestService;

    @Autowired
    private AIPService aipService;

    @Override
    public void onCopySuccess(Set<RequestInfo> requests) {
        // When a AIP is successfully copied to a new location, we have to update local AIP to add the new location.
        // Dispatch each success copy request by AIP to update
        Multimap<String, AbstractAIPUpdateTask> newFileLocations = ArrayListMultimap.create();
        for (RequestInfo r : requests) {
            // For each copy group request in success, check unitary file copy requests succeeded
            for (RequestResultInfoDTO sr : r.getSuccessRequests()) {
                // For each file copy success, check if at least one of the owners of the file is an AIP.
                for (String fileOwner : sr.getResultFile().getOwners()) {
                    if (UniformResourceName.isValidUrn(fileOwner)) {
                        // If so, associate the AIPUpdateFileLocationTask to the aip.
                        newFileLocations.put(fileOwner,
                                             AIPUpdateFileLocationTask.buildAddLocationTask(Lists.newArrayList(sr)));
                    }
                }
            }
        }
        // To improve performance, retrieve all requested AIPs in one request
        Collection<AIPEntity> aips = aipService.getAips(newFileLocations.keySet());
        // Then dispatch each update task by AIPentity
        Multimap<AIPEntity, AbstractAIPUpdateTask> newFileLocationsWithAIP = ArrayListMultimap.create();
        newFileLocations.asMap().forEach((aipId, tasks) -> {
            Optional<AIPEntity> aip = aips.stream().filter(a -> a.getAipId().equals(aipId)).findFirst();
            if (aip.isPresent()) {
                newFileLocationsWithAIP.putAll(aip.get(), tasks);
            }
        });
        newFileLocations.clear();
        // Finally, creates the AIPUpdateLocationRequests
        aipUpdateRequestService.create(newFileLocationsWithAIP);
        newFileLocationsWithAIP.clear();
    }

    @Override
    public void onCopyError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onAvailable(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onAvailabilityError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onDeletionSuccess(Set<RequestInfo> requests) {
        deleteRequestService.handleRemoteDeleteSuccess(requests);
    }

    @Override
    public void onDeletionError(Set<RequestInfo> requests) {
        deleteRequestService.handleRemoteDeleteError(requests);
    }

    @Override
    public void onReferenceSuccess(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteReferenceSuccess(requests);
    }

    @Override
    public void onReferenceError(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteReferenceError(requests);
    }

    @Override
    public void onStoreSuccess(Set<RequestInfo> requests) {
        requestService.handleRemoteStoreSuccess(requests);
    }

    @Override
    public void onStoreError(Set<RequestInfo> requests) {
        requestService.handleRemoteStoreError(requests);
    }

    @Override
    public void onRequestGranted(Set<RequestInfo> requests) {
        requestService.handleRemoteRequestGranted(requests);
    }

    @Override
    public void onRequestDenied(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteRequestDenied(requests);
    }
}
