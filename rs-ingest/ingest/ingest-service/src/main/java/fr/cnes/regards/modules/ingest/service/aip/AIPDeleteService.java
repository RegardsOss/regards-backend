/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ILastAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AIP service management for deletion
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AIPDeleteService implements IAIPDeleteService {

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private ILastAIPRepository lastAipRepository;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IPublisher publisher;

    @Override
    public boolean deletionAlreadyPending(AIPEntity aip) {
        return oaisDeletionRequestRepository.existsByAipIdAndStateIn(aip.getId(),
                                                                     Sets.newHashSet(InternalRequestState.CREATED,
                                                                                     InternalRequestState.BLOCKED,
                                                                                     InternalRequestState.RUNNING,
                                                                                     InternalRequestState.TO_SCHEDULE));
    }

    @Override
    public void scheduleLinkedFilesDeletion(OAISDeletionRequest request) {
        String sipId = request.getAip().getSip().getSipId();
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();

        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aips = aipRepository.findBySipSipId(sipId);

        for (AIPEntity aipEntity : aips) {
            String aipId = aipEntity.getAipId();
            // Retrieve all linked files
            for (ContentInformation ci : aipEntity.getAip().getProperties().getContentInformations()) {
                OAISDataObject dataObject = ci.getDataObject();
                filesToDelete.addAll(getFileDeletionEvents(aipId,
                                                           aipEntity.getSessionOwner(),
                                                           aipEntity.getSession(),
                                                           dataObject.getChecksum(),
                                                           dataObject.getLocations()));
            }
        }

        // Publish event to delete AIP files and AIPs itself
        Collection<RequestInfo> deleteRequestInfos = storageClient.delete(filesToDelete);

        request.setRemoteStepGroupIds(deleteRequestInfos.stream()
                                                        .map(RequestInfo::getGroupId)
                                                        .collect(Collectors.toList()));
        // Put the request as un-schedule.
        // The answering event from storage will put again the request to be executed
        request.setState(InternalRequestState.TO_SCHEDULE);
        oaisDeletionRequestRepository.save(request);
    }

    private List<FileDeletionRequestDTO> getFileDeletionEvents(String owner,
                                                               String sessionOwner,
                                                               String session,
                                                               String fileChecksum,
                                                               Set<OAISDataObjectLocation> locations) {
        List<FileDeletionRequestDTO> events = new ArrayList<>();
        for (OAISDataObjectLocation location : locations) {
            // Ignore if the file is yet stored
            if (location.getStorage() != null) {
                // Create the storage delete event
                events.add(FileDeletionRequestDTO.build(fileChecksum,
                                                        location.getStorage(),
                                                        owner,
                                                        sessionOwner,
                                                        session,
                                                        false));
            }
        }
        return events;
    }

    @Override
    public void processDeletion(String sipId, boolean deleteIrrevocably) {
        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);
        if (!aipsRelatedToSip.isEmpty()) {
            // we can find any aip from one sip as they are generated at same time so they all have the same session information
            AIPEntity aipForSessionInfo = aipsRelatedToSip.stream().findAny().get();
            sessionNotifier.productDeleted(aipForSessionInfo.getSessionOwner(),
                                           aipForSessionInfo.getSession(),
                                           aipsRelatedToSip);
            aipsRelatedToSip.forEach(entity -> entity.setState(AIPState.DELETED));
            if (deleteIrrevocably) {
                requestService.deleteAllByAip(aipsRelatedToSip);
                // Delete them
                aipRepository.deleteAll(aipsRelatedToSip);
            } else {
                // Mark the AIP as deleted
                aipRepository.saveAll(aipsRelatedToSip);
            }
            // Remove last flag entry
            aipsRelatedToSip.forEach(aip -> removeLastFlag(aip));
            // Send notification to data mangement for feature deleted
            aipsRelatedToSip.forEach(aip -> publisher.publish(FeatureEvent.buildFeatureDeleted(aip.getAipId())));
        }
    }

    private void removeLastFlag(AIPEntity aip) {
        lastAipRepository.deleteByAipId(aip.getId());
    }

}
