/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageRequestListener;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateFileLocationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.AIPService;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * This class offers callbacks from storage events
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Component
@MultitenantTransactional
public class StorageResponseFlowHandler implements IStorageRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageResponseFlowHandler.class);

    private static final String HANDLER_NAME = "[STORAGE RESPONSE HANDLER] ";

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IOAISDeletionService deleteRequestService;

    @Autowired
    private AIPUpdateRequestService aipUpdateRequestService;

    @Autowired
    private AIPService aipService;

    @Override
    public void onCopySuccess(Set<RequestInfo> requests) {
        long start = System.currentTimeMillis();
        LOGGER.debug("[COPY RESPONSE HANDLER] Handling {} copy group requests", requests.size());
        // When a AIP is successfully copied to a new location, we have to update local AIP to add the new location.
        // Dispatch each success copy request by AIP to update
        Multimap<String, AbstractAIPUpdateTask> updateTasksByAIPId = createAIPUpdateTasksByAIP(requests);
        // To improve performance, retrieve all requested AIPs in one request
        Collection<AIPEntity> aips = aipService.findByAipIds(updateTasksByAIPId.keySet());
        // Then dispatch each update task by AIPentity
        Multimap<AIPEntity, AbstractAIPUpdateTask> updateTasksByAIP = ArrayListMultimap.create();
        updateTasksByAIPId.asMap().forEach((aipId, tasks) -> {
            Optional<AIPEntity> aip = aips.stream().filter(a -> a.getAipId().equals(aipId)).findFirst();
            if (aip.isPresent()) {
                updateTasksByAIP.putAll(aip.get(), tasks);
            }
        });
        // Finally, creates the AIPUpdateLocationRequests
        int nbScheduled = aipUpdateRequestService.create(updateTasksByAIP);
        LOGGER.debug("[COPY RESPONSE HANDLER] {} update requests scheduled in {}ms",
                     nbScheduled,
                     System.currentTimeMillis() - start);
    }

    @Override
    public void onCopyError(Set<RequestInfo> requests) {
        LOGGER.debug("[COPY RESPONSE HANDLER] Handling {} copy error group requests", requests.size());
        // handle success requests if any
        onCopySuccess(requests);
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
        LOGGER.debug("[DELETION RESPONSE HANDLER] Handling {} deletion group requests", requests.size());
        deleteRequestService.handleRemoteDeleteSuccess(requests);
    }

    @Override
    public void onDeletionError(Set<RequestInfo> requests) {
        LOGGER.debug("[DELETION RESPONSE HANDLER] Handling {} deletion error group requests", requests.size());
        deleteRequestService.handleRemoteDeleteError(requests);
    }

    @Override
    public void onReferenceSuccess(Set<RequestInfo> requests) {
        LOGGER.debug("[REFERENCE RESPONSE HANDLER] Handling {} reference group requests", requests.size());
        ingestRequestService.handleRemoteReferenceSuccess(requests);
    }

    @Override
    public void onReferenceError(Set<RequestInfo> requests) {
        LOGGER.debug("[REFERENCE RESPONSE HANDLER] Handling {} reference error group requests", requests.size());
        ingestRequestService.handleRemoteReferenceError(requests);
    }

    @Override
    public void onStoreSuccess(Set<RequestInfo> requestInfos) {

        long globalstart = System.currentTimeMillis();
        LOGGER.debug(HANDLER_NAME + "Handling {} storage group requests", requestInfos.size());

        // Detect INGEST requests to handle
        Map<RequestInfo, Set<IngestRequest>> toHandle = new HashMap<>();

        List<AbstractRequest> requests = requestService.getRequests(requestInfos);
        for (RequestInfo ri : requestInfos) {
            LOGGER.trace(HANDLER_NAME + "handling success storage request {} with {} success / {} errors",
                         ri.getGroupId(),
                         ri.getSuccessRequests().size(),
                         ri.getErrorRequests().size());
            boolean found = false;
            Set<IngestRequest> toHandleRemote = Sets.newHashSet();

            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    found = true;
                    if (request instanceof IngestRequest) {
                        LOGGER.trace(HANDLER_NAME + "Ingest request {} found associated to group request {}",
                                     request.getId(),
                                     ri.getGroupId());
                        toHandleRemote.add((IngestRequest) request);
                    } else {
                        LOGGER.trace(HANDLER_NAME + "Request type undefined {} for group {}",
                                     request.getId(),
                                     ri.getGroupId());
                        requestService.handleRemoteStoreSuccess(request);
                    }
                }
            }

            toHandle.put(ri, toHandleRemote);
            if (!found) {
                LOGGER.warn(HANDLER_NAME + "No request found associated to group request {}", ri.getGroupId());
            }
        }

        LOGGER.debug(HANDLER_NAME + "Before handling success of {} request infos take {} ms",
                     requestInfos.size(),
                     System.currentTimeMillis() - globalstart);

        // Handle all detected INGEST requests
        ingestRequestService.handleRemoteStoreSuccess(toHandle);

        LOGGER.info(HANDLER_NAME + "Handling of {} request infos take {} ms",
                    requestInfos.size(),
                    System.currentTimeMillis() - globalstart);
    }

    @Override
    public void onStoreError(Set<RequestInfo> requestInfos) {
        LOGGER.debug(HANDLER_NAME + "Handling {} storage error group requests", requestInfos.size());
        List<AbstractRequest> requests = requestService.getRequests(requestInfos);
        for (RequestInfo requestInfo : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(requestInfo.getGroupId())) {
                    if (request instanceof IngestRequest ingestRequest) {
                        ingestRequestService.handleRemoteStoreError(ingestRequest, requestInfo);
                    } else {
                        requestService.handleRemoteStoreError(request);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestGranted(Set<RequestInfo> requests) {
        requestService.handleRemoteRequestGranted(requests);
    }

    @Override
    public void onRequestDenied(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteRequestDenied(requests);
    }

    /**
     * Create a map associates AIP identifier to a list of update tasks associated to the requests info responses from storage
     * client.
     *
     * @param requestsInfo {@link RequestInfo}s from storage client.
     * @return {@link Map} key: AIP identifier. Values: {@link AbstractAIPUpdateTask}s
     */
    private Multimap<String, AbstractAIPUpdateTask> createAIPUpdateTasksByAIP(Collection<RequestInfo> requestsInfo) {
        Multimap<String, AbstractAIPUpdateTask> newFileLocations = ArrayListMultimap.create();
        int count = 0;
        int total = 0;
        for (RequestInfo r : requestsInfo) {
            // For each copy group request in success, check unitary file copy requests succeeded
            for (RequestResultInfoDto sr : r.getSuccessRequests()) {
                total++;
                // For each file successfully copied, check if at least one of the owners of the file is an AIP.
                boolean found = false;
                for (String fileOwner : sr.getResultFile().getOwners()) {
                    if (OaisUniformResourceName.isValidUrn(fileOwner)) {
                        // If so, associate the AIPUpdateFileLocationTask to the aip.
                        newFileLocations.put(fileOwner,
                                             AIPUpdateFileLocationTask.buildAddLocationTask(Lists.newArrayList(sr)));
                        found = true;
                        count++;
                        LOGGER.debug("File {}(checksum={}, type={}) as been copied to {} and is associated to AIP {}",
                                     sr.getResultFile().getMetaInfo().getFileName(),
                                     sr.getResultFile().getMetaInfo().getChecksum(),
                                     sr.getResultFile().getMetaInfo().getType(),
                                     sr.getRequestStorage(),
                                     fileOwner);
                    }
                }
                if (!found) {
                    LOGGER.warn("File {}(checksum={}, type={}) as been copied to {} but is not associated to any AIP",
                                sr.getResultFile().getMetaInfo().getFileName(),
                                sr.getResultFile().getMetaInfo().getChecksum(),
                                sr.getResultFile().getMetaInfo().getType(),
                                sr.getRequestStorage());
                }
            }
        }
        LOGGER.info("{} copied files event received from {} groups. {} associated to existing AIPs",
                    total,
                    requestsInfo.size(),
                    count);
        return newFileLocations;
    }

}
