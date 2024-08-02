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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.mapper.IOAISDeletionPayloadMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPDeleteService;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionJob;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionsCreatorJob;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationService;
import fr.cnes.regards.modules.ingest.service.settings.IIngestSettingsService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Service to handle {@link OAISDeletionCreatorRequest}s
 * And the {@link fr.cnes.regards.modules.ingest.service.job.OAISDeletionJob} algoritm
 *
 * @author Sébastien Binda
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
    private IAIPDeleteService aipDeleteService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IOAISDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private IAIPNotificationService aipNotificationService;

    @Autowired
    private IIngestSettingsService ingestSettingsService;

    @Override
    public Optional<OAISDeletionCreatorRequest> searchCreator(Long requestId) {
        return creatorRepository.findById(requestId);
    }

    @Override
    public List<OAISDeletionRequest> searchRequests(List<Long> deleteRequestIds) {
        return requestRepository.findAllById(deleteRequestIds);
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
    public boolean handleJobCrash(JobInfo jobInfo) {
        if (OAISDeletionsCreatorJob.class.getName().equals(jobInfo.getClassName())) {
            try {
                Type type = new TypeToken<Long>() {

                }.getType();
                Long requestId = IJob.getValue(jobInfo.getParametersAsMap(), OAISDeletionsCreatorJob.REQUEST_ID, type);
                Optional<OAISDeletionCreatorRequest> request = creatorRepository.findById(requestId);
                request.ifPresent(r -> {
                    r.setState(InternalRequestState.ERROR);
                    r.setErrors(IngestErrorType.DELETE, Set.of(jobInfo.getStatus().getStackTrace()));
                    LOGGER.warn("OAIS Deletion creator request with id {} is now in {} state at {}",
                                r.getId(),
                                r.getState(),
                                r.getLastUpdate());
                    creatorRepository.save(r);
                });
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                LOGGER.error(String.format("OAISDeletionsCreatorJob request job with id \"%s\" fails with status \"%s\"",
                                           jobInfo.getId(),
                                           jobInfo.getStatus().getStatus()), e);
            }
            return true;
        } else if (OAISDeletionJob.class.getName().equals(jobInfo.getClassName())) {
            try {
                Type type = new TypeToken<List<Long>>() {

                }.getType();
                List<Long> requestIds = IJob.getValue(jobInfo.getParametersAsMap(),
                                                      OAISDeletionJob.OAIS_DELETION_REQUEST_IDS,
                                                      type);

                if (requestIds != null && !requestIds.isEmpty()) {
                    List<OAISDeletionRequest> requests = requestRepository.findAllById(requestIds);
                    requests.forEach(r -> {
                        r.setState(InternalRequestState.ERROR);
                        r.setErrors(IngestErrorType.DELETE, Set.of(jobInfo.getStatus().getStackTrace()));
                        LOGGER.warn("OAIS Deletion request with id {} is now in {} state at {}",
                                    r.getId(),
                                    r.getState(),
                                    r.getLastUpdate());
                    });
                    requestRepository.saveAll(requests);
                }
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                LOGGER.error(String.format("OAISDeletionJob request job with id \"%s\" fails with status \"%s\"",
                                           jobInfo.getId(),
                                           jobInfo.getStatus().getStatus()), e);
            }
            return true;
        }
        return false;
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
    public void runDeletion(Collection<OAISDeletionRequest> requests, OAISDeletionJob oaisDeletionJob) {
        Iterator<OAISDeletionRequest> requestIter = requests.iterator();
        boolean interrupted = Thread.currentThread().isInterrupted();
        Set<OAISDeletionRequest> errors = new HashSet<>();
        Set<OAISDeletionRequest> success = new HashSet<>();

        // See if notifications are required
        boolean isToNotify = ingestSettingsService.isActiveNotification();

        // Handle deletion requests
        while (requestIter.hasNext() && !interrupted) {
            OAISDeletionRequest request = requestIter.next();
            AIPEntity aipToDelete = request.getAip();
            SIPEntity sipToDelete = aipToDelete.getSip();
            if (request.isDeleteFiles() && !request.isRequestFilesDeleted()) {
                aipDeleteService.scheduleLinkedFilesDeletion(request);
            } else {
                // delete first the request so the aip can be deleted (the aip is a foreign key in the request)
                requestService.deleteRequest(request);
                aipDeleteService.processDeletion(sipToDelete.getSipId(),
                                                 request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                sipService.processDeletion(sipToDelete.getSipId(),
                                           request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                // if notifications are required
                if (isToNotify) {
                    // break the link between request and aip (the aip does not exist anymore)
                    request.setAip(null);
                    // add aip content to the payload (the aip does not exist anymore but its content is still
                    // required to notify its deletion, so it is added in the request payload)
                    request.setAipToNotify(aipToDelete);
                    // save again the request and add it to the list of requests successfully processed
                    OAISDeletionRequest registeredRequest = deletionRequestRepository.save(request);
                    success.add(registeredRequest);
                }
            }
            oaisDeletionJob.advanceCompletion();
            interrupted = Thread.currentThread().isInterrupted();
        }
        // abort requests that could not be handled
        ArrayList<OAISDeletionRequest> aborted = new ArrayList<>();
        while (requestIter.hasNext()) {
            OAISDeletionRequest request = requestIter.next();
            request.setState(InternalRequestState.ABORTED);
            aborted.add(request);
            oaisDeletionJob.advanceCompletion();
        }
        interrupted = Thread.interrupted();
        deletionRequestRepository.saveAll(errors);
        deletionRequestRepository.saveAll(aborted);

        // If notifications are active, send them to notifier
        if (isToNotify && !success.isEmpty()) {
            aipNotificationService.sendRequestsToNotifier(Sets.newHashSet(success));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
