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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.mapper.IOAISDeletionPayloadMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationService;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationSettingsService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Service to handle {@link OAISDeletionCreatorRequest}s
 * And the {@link fr.cnes.regards.modules.ingest.service.job.OAISDeletionJob} algoritm
 *
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class OAISDeletionService implements IOAISDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAISDeletionService.class);

    @Autowired
    private IOAISDeletionCreatorRepository creatorRepository;

    @Autowired
    private IOAISDeletionRequestRepository requestRepository;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IOAISDeletionPayloadMapper deletionRequestMapper;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IOAISDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private IAIPNotificationService aipNotificationService;

    @Autowired
    private IAIPNotificationSettingsService aipNotificationSettingsService;

    @Override
    public Optional<OAISDeletionCreatorRequest> searchCreator(Long requestId) {
        return creatorRepository.findById(requestId);
    }

    @Override
    public List<OAISDeletionRequest> searchRequests(List<Long> deleteRequestIds) {
        return requestRepository.findAllById(deleteRequestIds);
    }

    @Override
    public void update(OAISDeletionRequest request) {
        requestRepository.save(request);
    }

    @Override
    public void registerOAISDeletionCreator(OAISDeletionPayloadDto request) {
        OAISDeletionCreatorPayload deletionPayload = deletionRequestMapper.dtoToEntity(request);
        OAISDeletionCreatorRequest deletionRequest = OAISDeletionCreatorRequest.build(deletionPayload);
        deletionRequest = (OAISDeletionCreatorRequest) requestService.scheduleRequest(deletionRequest);
        if (deletionRequest.getState() != InternalRequestState.BLOCKED) {
            requestService.scheduleJob(deletionRequest);
        }
    }

    @Override
    public void handleRemoteDeleteError(Set<RequestInfo> requestInfos) {
        // Do not handle storage deletion errors in ingest process. Storage errors will be handled in storage process.
        handleRemoteDeleteSuccess(requestInfos);
    }

    @Override
    public void handleRemoteDeleteSuccess(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = requestService.getRequests(requestInfos);
        List<AbstractRequest> requestsToSchedule = Lists.newArrayList();
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    // Storage knows files are deleted
                    // Put back request as CREATED
                    OAISDeletionRequest deletionRequest = (OAISDeletionRequest) request;
                    deletionRequest.setRequestFilesDeleted();
                    deletionRequest.clearRemoteStepGroupIds();
                    requestsToSchedule.add(deletionRequest);
                }
            }
        }
        requestService.scheduleRequests(requestsToSchedule);
    }

    @Override
    public void runDeletion(Collection<OAISDeletionRequest> requests) {
        Iterator<OAISDeletionRequest> requestIter = requests.iterator();
        boolean interrupted = Thread.currentThread().isInterrupted();
        Set<OAISDeletionRequest> errors = new HashSet<>();
        Set<OAISDeletionRequest> success = new HashSet<>();

        // See if notifications are required
        AIPNotificationSettings notificationSettings = aipNotificationSettingsService.retrieve();
        boolean isToNotify = notificationSettings.isActiveNotification();

        // Handle deletion requests
        while (requestIter.hasNext() && !interrupted) {
            OAISDeletionRequest request = requestIter.next();
            if (!requestService.shouldDelayRequest(request)) {
                AIPEntity aipToDelete = request.getAip();
                SIPEntity sipToDelete = aipToDelete.getSip();
                try {
                    if (request.isDeleteFiles() && !request.isRequestFilesDeleted()) {
                        aipService.scheduleLinkedFilesDeletion(request);
                    } else {
                        requestService.deleteRequest(request);
                        aipService.processDeletion(sipToDelete.getSipId(),
                                                   request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                        sipService.processDeletion(sipToDelete.getSipId(),
                                                   request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                        // if notifications are required
                        if(isToNotify) {
                            // break the link between request and aip (because the deleted aip is a foreign key in the request)
                            request.setAip(null);
                            // add aip content to payload (in case of notification error, the aip does not exist anymore
                            // but its content is required to notify, so it is added in the request payload)
                            request.setAipToNotify(aipToDelete.getAip());
                            // add request to list of requests successfully processed
                            success.add(request);
                        }
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Deletion request %s of AIP %s could not be executed",
                                                    request.getId(), request.getAip().getAipId());
                    LOGGER.error(errorMsg, e);
                    request.setState(InternalRequestState.ERROR);
                    request.addError(errorMsg);
                    errors.add(request);
                }
            } else {
                String errorMsg = String
                        .format("Deletion request %s of AIP %s could not be executed cause other requests reference same AIPs. Request is blocked.",
                                request.getId(), request.getAip().getAipId());
                LOGGER.warn(errorMsg);
                request.setState(InternalRequestState.BLOCKED);
                request.addError(errorMsg);
                errors.add(request);
            }
            interrupted = Thread.currentThread().isInterrupted();
        }
        // abort requests that could not be handled
        ArrayList<OAISDeletionRequest> aborted = new ArrayList<>();
        while (requestIter.hasNext()) {
            OAISDeletionRequest request = requestIter.next();
            request.setState(InternalRequestState.ABORTED);
            aborted.add(request);
        }
        interrupted = Thread.interrupted();
        deletionRequestRepository.saveAll(errors);
        deletionRequestRepository.saveAll(aborted);

        // If notifications are active, send them to notifier
        if(isToNotify && !success.isEmpty()) {
            aipNotificationService.sendRequestsToNotifier(Sets.newHashSet(success));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

}
