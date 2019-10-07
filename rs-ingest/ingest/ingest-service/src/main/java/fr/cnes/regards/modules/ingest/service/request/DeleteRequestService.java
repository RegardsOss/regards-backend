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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.StorageDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Delete request service
 *
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class DeleteRequestService implements IDeleteRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteRequestService.class);

    @Autowired
    private IStorageDeletionRequestRepository storageDeletionRequestRepo;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Override
    public void handleRemoteDeleteError(RequestInfo request, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        Optional<StorageDeletionRequest> storageRequestOptional = storageDeletionRequestRepo.findOneByRemoteStepGroupId(request.getGroupId());
        if (storageRequestOptional.isPresent()) {
            StorageDeletionRequest deletionRequest = storageRequestOptional.get();
            deletionRequest.setState(InternalRequestStep.ERROR);
            Set<String> errorList = errors.stream()
                    .map(RequestResultInfoDTO::getErrorCause)
                    .collect(Collectors.toSet());
            deletionRequest.setErrors(errorList);

            // Append to the message the origin of the issue
            Set<String> oaisEntityError = errorList.stream()
                    .map(message -> "Error while removing stored file: " + message)
                    .collect(Collectors.toSet());
            try {
                // Update SIP with error
                SIPEntity sipEntity = sipService.getEntity(deletionRequest.getSipId());
                sipEntity.setState(SIPState.ERROR);
                sipEntity.setErrors(oaisEntityError);
                sessionNotifier.notifySIPDeletionFailed(sipEntity);
                sipService.save(sipEntity);
                // Update AIP with error
                Set<AIPEntity> aipEntities = aipService.getAips(deletionRequest.getSipId());
                for (AIPEntity aipEntity : aipEntities) {
                    aipEntity.setErrors(oaisEntityError);
                    aipEntity.setState(AIPState.ERROR);
                    aipService.save(aipEntity);
                }
                sessionNotifier.notifyAIPDeletionFailed(aipEntities);
            } catch (EntityNotFoundException e) {
                LOGGER.debug("Can't mark SIPEntity with sidId[{}] with error: {}", deletionRequest.getSipId(), e.getMessage());
            }
            storageDeletionRequestRepo.save(deletionRequest);
        }
    }

    @Override
    public void handleRemoteDeleteSuccess(RequestInfo request, Collection<RequestResultInfoDTO> success) {
        Optional<StorageDeletionRequest> storageRequestOptional = storageDeletionRequestRepo.findOneByRemoteStepGroupId(request.getGroupId());
        if (storageRequestOptional.isPresent()) {
            StorageDeletionRequest deletionRequest = storageRequestOptional.get();
            boolean deleteIrrevocably = deletionRequest.getDeletionMode() == SessionDeletionMode.IRREVOCABLY;
            aipService.processDeletion(deletionRequest.getSipId(), deleteIrrevocably);
            sipService.processDeletion(deletionRequest.getSipId(), deleteIrrevocably);
            storageDeletionRequestRepo.delete(deletionRequest);
        }
    }
}
