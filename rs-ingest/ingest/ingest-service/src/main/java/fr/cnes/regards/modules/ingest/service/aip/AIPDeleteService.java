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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.dto.ContentInformationDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ILastAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AbstractAIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.LastAIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.IStorageClient;
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
    public void cancelStorageRequests(Collection<IngestRequest> requests) {
        List<String> storageGroupIds = requests.stream()
                                               .filter(r -> r.getRemoteStepGroupIds() != null
                                                            && !r.getRemoteStepGroupIds().isEmpty())
                                               .flatMap(r -> r.getRemoteStepGroupIds().stream())
                                               .toList();
        storageClient.cancelRequests(storageGroupIds);
    }

    @Override
    public void deleteAll(Set<AIPEntity> aipEntities) {
        aipRepository.deleteAllByIdInBatch(aipEntities.stream().map(AIPEntity::getId).toList());
    }

    @Override
    public void scheduleLinkedFilesDeletion(OAISDeletionRequest request) {
        String sipId = request.getAip().getSip().getSipId();

        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aips = aipRepository.findBySipSipId(sipId);

        // Send deletion requests to storage
        Collection<RequestInfo> deleteRequestInfos = sendLinkedFilesDeletionRequest(aips);

        // Add deletion requests info to current request
        request.setRemoteStepGroupIds(deleteRequestInfos.stream()
                                                        .map(RequestInfo::getGroupId)
                                                        .collect(Collectors.toList()));
        // Put the request as un-schedule.
        // The answering event from storage will put again the request to be executed
        request.setState(InternalRequestState.TO_SCHEDULE);
        oaisDeletionRequestRepository.save(request);
    }

    @Override
    public Collection<RequestInfo> sendLinkedFilesDeletionRequest(Collection<AIPEntity> aips) {
        List<FileDeletionDto> filesToDelete = new ArrayList<>();
        for (AIPEntity aipEntity : aips) {
            String aipId = aipEntity.getAipId();
            // Retrieve all linked files
            for (ContentInformationDto ci : aipEntity.getAip().getProperties().getContentInformations()) {
                OAISDataObjectDto dataObject = ci.getDataObject();
                filesToDelete.addAll(buildFileDeletions(aipId,
                                                        aipEntity.getSessionOwner(),
                                                        aipEntity.getSession(),
                                                        dataObject.getChecksum(),
                                                        dataObject.getLocations()));
            }
        }
        // Delete AIP files in storage by publishing events
        return storageClient.delete(filesToDelete);
    }

    private List<FileDeletionDto> buildFileDeletions(String owner,
                                                     String sessionOwner,
                                                     String session,
                                                     String fileChecksum,
                                                     Set<OAISDataObjectLocationDto> locations) {
        List<FileDeletionDto> fileDeletions = new ArrayList<>();
        for (OAISDataObjectLocationDto location : locations) {
            // Ignore if the file is not yet stored
            if (location.getStorage() != null) {
                // Create the storage delete event
                fileDeletions.add(FileDeletionDto.build(fileChecksum,
                                                        location.getStorage(),
                                                        owner,
                                                        sessionOwner,
                                                        session,
                                                        false));
            }
        }
        return fileDeletions;
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
            requestService.deleteAllByAip(aipsRelatedToSip);
            if (deleteIrrevocably) {
                // Delete them
                aipRepository.deleteAll(aipsRelatedToSip);
            } else {
                // Mark the AIP as deleted
                aipRepository.saveAll(aipsRelatedToSip);
            }
            manageLastFlag(aipsRelatedToSip);
            // Send notification to data mangement for feature deleted
            aipsRelatedToSip.forEach(aip -> publisher.publish(FeatureEvent.buildFeatureDeleted(aip.getAipId())));
        }
    }

    /**
     * Manage the last flag of deleted AIPs.
     * This method gets the previous version of these AIPs (if exists), then:
     * <li>set their last flag to true</li>
     * <li>modify the lastAipEntity table to update entry with the new last aip (or not if not exists)</li>
     */
    private void manageLastFlag(Set<AIPEntity> deletedAips) {
        // Remove last aip entries
        lastAipRepository.deleteAllByAipIdIn(deletedAips.stream()
                                                        .map(AbstractAIPEntity::getId)
                                                        .collect(Collectors.toSet()));
        // find last version of deleted provider_id
        Set<String> providerIds = deletedAips.stream()
                                             .map(AbstractOAISEntity::getProviderId)
                                             .collect(Collectors.toSet());
        Set<AIPEntity> lastVersionOfSameProviderIds = aipRepository.findAllByProviderIdWithVersionMax(providerIds);
        for (AIPEntity lastVersionOfSameProviderId : lastVersionOfSameProviderIds) {
            if (!lastVersionOfSameProviderId.isLast()) {
                // mark last version as last
                aipRepository.updateLast(lastVersionOfSameProviderId.getId(), true);
                // create last aip entry
                lastAipRepository.save(new LastAIPEntity(lastVersionOfSameProviderId.getId(),
                                                         lastVersionOfSameProviderId.getProviderId()));
            }
        }
    }

    @Override
    public void removeLastFlag(AIPEntity aip) {
        lastAipRepository.deleteByAipId(aip.getId());
    }

}
