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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPStorageService;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Manage ingest requests
 *
 * @author Marc SORDI
 *
 */
@Service
@MultitenantTransactional
public class IngestRequestService implements IIngestRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestService.class);

    @Autowired
    private IngestConfigurationProperties confProperties;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IAIPStorageService aipStorageService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IAIPStoreMetaDataRequestService aipSaveMetaDataService;

    @Override
    public void scheduleIngestProcessingJobByChain(String chainName, Collection<IngestRequest> requests) {

        // Schedule jobs
        LOGGER.debug("Scheduling job to handle {} ingest request(s) on chain {}", requests.size(), chainName);

        Set<Long> ids = requests.stream().map(r -> r.getId()).collect(Collectors.toSet());

        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, ids));
        jobParameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, chainName));
        // Lock job info
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.INGEST_PROCESSING_JOB_PRIORITY.getPriority(),
                jobParameters, authResolver.getUser(), IngestProcessingJob.class.getName());
        // Lock job to avoid automatic deletion. The job must be unlock when the link to the request is removed.
        jobInfo.setLocked(true);
        jobInfoService.createAsQueued(jobInfo);

        // Attach job
        requests.forEach(r -> r.setJobInfo(jobInfo));
    }

    @Override
    public void handleJobCrash(JobEvent jobEvent) {

        JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
        if (IngestProcessingJob.class.getName().equals(jobInfo.getClassName())) {

            // Load ingest requests
            try {
                Type type = new TypeToken<Set<Long>>() {
                }.getType();
                Set<Long> ids;
                ids = IJob.getValue(jobInfo.getParametersAsMap(), IngestProcessingJob.IDS_PARAMETER, type);
                List<IngestRequest> requests = loadByIds(ids);
                requests.forEach(r -> handleIngestJobFailed(r, null));
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                String message = String.format("Ingest request job with id \"%s\" fails with status \"%s\"",
                                               jobEvent.getJobId(), jobEvent.getJobEventType());
                LOGGER.error(message, e);
                notificationClient.notify(message, "Ingest job failure", NotificationLevel.ERROR, DefaultRole.ADMIN);
            }
        }
    }

    @Override
    public List<IngestRequest> loadByIds(Set<Long> ids) {
        return ingestRequestRepository.findByIdIn(ids);
    }

    @Override
    public void handleRequestGranted(IngestRequest request) {
        // Keep track of the request
        saveRequest(request);

        // Publish
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   RequestState.GRANTED, request.getErrors()));
    }

    @Override
    public void handleRequestDenied(IngestRequest request) {
        // Do not keep track of the request
        // Publish DENIED request
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   RequestState.DENIED, request.getErrors()));
    }

    @Override
    public void handleIngestJobFailed(IngestRequest request, SIPEntity entity) {
        // Lock job
        jobInfoService.lock(request.getJobInfo());

        // Keep track of the error
        saveAndPublishErrorRequest(request, null);
    }

    @Override
    public List<AIPEntity> handleIngestJobSucceed(IngestRequest request, SIPEntity sipEntity, List<AIP> aips) {

        // Save SIP entity
        sipEntity = sipService.save(sipEntity);

        // Build AIP entities and save them
        List<AIPEntity> aipEntities = aipService.createAndSave(sipEntity, aips);
        // Attach generated AIPs to the current request
        request.setAips(aipEntities);

        // Launch next remote step
        request.setStep(IngestRequestStep.REMOTE_STORAGE_REQUESTED, confProperties.getRemoteRequestTimeout());

        try {
            // Send AIP files storage events, keep these events ids in a list
            List<String> remoteStepGroupIds = aipStorageService.storeAIPFiles(aipEntities, request.getMetadata());

            // Register request info to identify storage callback events
            request.setRemoteStepGroupIds(remoteStepGroupIds);

            // Keep track of the request
            saveRequest(request);
        } catch (ModuleException e) {
            // Keep track of the error
            saveAndPublishErrorRequest(request, String
                    .format("Cannot send events to store AIP files because they are malformed. Cause: %s",
                            e.getMessage()));
        }
        return aipEntities;
    }

    @Override
    public void handleRemoteRequestDenied(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            // Retrieve request
            Optional<IngestRequest> requestOp = ingestRequestRepository.findOne(ri.getGroupId());
            if (requestOp.isPresent()) {
                IngestRequest request = requestOp.get();
                switch (request.getStep()) {
                    case REMOTE_STORAGE_REQUESTED:
                        // Save the request was denied at AIP files storage
                        request.setStep(IngestRequestStep.REMOTE_STORAGE_DENIED);
                        request.setState(InternalRequestStep.ERROR);
                        // Keep track of the error
                        saveAndPublishErrorRequest(request, String.format("Remote file storage request denied"));
                        break;
                    default:
                        // Keep track of the error
                        saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
                        break;
                }
            }
        }
    }

    @Override
    public void handleRemoteStoreSuccess(IngestRequest request, RequestInfo requestInfo) {

        if (request.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
            // Update AIPs with meta returned by storage
            aipStorageService.updateAIPsContentInfosAndLocations(request.getAips(), requestInfo.getSuccessRequests());
            // Check if there is another storage request we're waiting for
            List<String> remoteStepGroupIds = updateRemoteStepGroupId(request, requestInfo);
            if (!remoteStepGroupIds.isEmpty()) {
                saveRequest(request);
                // Another request is still pending
                return;
            }
            // The current request is over
            finalizeSuccessfulRequest(request);
        } else {
            // Keep track of the error
            saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
        }
    }

    private List<String> updateRemoteStepGroupId(IngestRequest request, RequestInfo requestInfo) {
        List<String> remoteStepGroupIds = request.getRemoteStepGroupIds();
        remoteStepGroupIds.remove(requestInfo.getGroupId());
        request.setRemoteStepGroupIds(remoteStepGroupIds);
        return remoteStepGroupIds;
    }

    private void finalizeSuccessfulRequest(IngestRequest request) {
        // Clean
        deleteRequest(request);

        List<AIPEntity> aips = request.getAips();

        // Change AIP state
        for (AIPEntity aipEntity : aips) {
            aipEntity.setState(AIPState.STORED);
            aipService.save(aipEntity);
        }
        sessionNotifier.productStoreSuccess(request.getSessionOwner(), request.getSession(), aips);

        // Schedule manifest archivage
        aipSaveMetaDataService.schedule(aips, request.getMetadata().getStorages(), false, true);
        sessionNotifier.productMetaStorePending(request.getSessionOwner(), request.getSession(), aips);

        // Update SIP state
        SIPEntity sipEntity = aips.get(0).getSip();
        sipEntity.setState(SIPState.STORED);
        sipService.save(sipEntity);
        // Publish SUCCESSFUL request
        publisher.publish(IngestRequestEvent.build(request.getRequestId(), request.getSip().getId(),
                                                   sipEntity.getSipId(), RequestState.SUCCESS, request.getErrors()));
    }

    @Override
    public void handleRemoteStoreError(IngestRequest request, RequestInfo requestInfo) {

        // Propagate errors
        requestInfo.getErrorRequests().forEach(e -> request.addError(e.getErrorCause()));

        if (request.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
            // Update AIP and SIP with current error
            updateOAISEntitiesWithErrors(request, requestInfo.getErrorRequests(),
                                         "Error occurred while storing AIP files");
            // Update AIPs with success response returned by storage
            aipStorageService.updateAIPsContentInfosAndLocations(request.getAips(), requestInfo.getSuccessRequests());
            // Save error in request status
            request.setStep(IngestRequestStep.REMOTE_STORAGE_ERROR);
            // Keep track of the error
            saveAndPublishErrorRequest(request, String.format("Remote file storage request error"));
        } else {
            // Keep track of the error
            saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
        }
    }

    @Override
    public void handleRemoteReferenceSuccess(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            // Retrieve request and related SIP & AIPs entities
            Optional<IngestRequest> requestOp = ingestRequestRepository.findOneWithAIPs(ri.getGroupId());
            if (requestOp.isPresent()) {
                IngestRequest request = requestOp.get();
                if (request.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {// Check if there is another storage request we're waiting for
                    List<String> remoteStepGroupIds = updateRemoteStepGroupId(request, ri);
                    if (!remoteStepGroupIds.isEmpty()) {
                        saveRequest(request);
                        // Another request is still pending
                        return;
                    }
                    // The current request is over
                    finalizeSuccessfulRequest(request);
                }
            }
        }
    }

    @Override
    public void handleRemoteReferenceError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            // Retrieve request
            Optional<IngestRequest> requestOp = ingestRequestRepository.findOneWithAIPs(ri.getGroupId());

            if (requestOp.isPresent()) {
                IngestRequest request = requestOp.get();
                // Propagate errors
                if (ri.getErrorRequests() != null) {
                    ri.getErrorRequests().forEach(e -> request.addError(e.getErrorCause()));
                }
                updateOAISEntitiesWithErrors(request, ri.getErrorRequests(),
                                             "Error occurred while storing AIP references");
            }
        }
    }

    private void saveAndPublishErrorRequest(IngestRequest request, @Nullable String message) {
        // Mutate request
        request.addError(String.format("Storage request error with id \"%s\" and SIP provider id \"%s\"",
                                       request.getRequestId(), request.getSip().getId()));
        if (message != null) {
            request.addError(message);
        }

        // Keep track of the error
        saveRequest(request);

        // Publish
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   RequestState.ERROR, request.getErrors()));
    }

    /**
     * Creates or update the given {@link IngestRequest} and lock associated jobs if any.
     * @param request
     * @return saved {@link IngestRequest}
     */
    public IngestRequest saveRequest(IngestRequest request) {
        // Before saving entity check the state of the associated job if any
        if ((request.getJobInfo() != null) && !request.getJobInfo().isLocked()) {
            // Lock the job info before saving entity in order to avoid deletion of this job by an other process
            JobInfo jobInfo = request.getJobInfo();
            jobInfo.setLocked(true);
            jobInfoService.save(jobInfo);
            request.setJobInfo(jobInfo);
        }
        return ingestRequestRepository.save(request);
    }

    /**
     * Delete the given {@link IngestRequest} and unlock associated jobs.
     * @param request
     */
    public void deleteRequest(IngestRequest request) {
        if ((request.getJobInfo() != null) && !request.getJobInfo().isLocked()) {
            JobInfo jobInfoToUnlock = request.getJobInfo();
            jobInfoToUnlock.setLocked(false);
            jobInfoService.save(jobInfoToUnlock);
        }
        ingestRequestRepository.delete(request);
    }

    private void updateOAISEntitiesWithErrors(IngestRequest request, Collection<RequestResultInfoDTO> errors,
            String errorCause) {
        List<AIPEntity> aips = request.getAips();
        // Iterate overs AIPs and errors
        for (AIPEntity aipEntity : aips) {
            for (RequestResultInfoDTO error : errors) {
                // Check using owner property if the AIP contains the file that was not properly saved
                if (error.getResultFile().getOwners().contains(aipEntity.getAipId())) {
                    // Add the cause to this AIP
                    String errorMessage = errorCause + ": " + error.getErrorCause();
                    aipService.save(aipEntity);
                }
            }
        }
        // Save all errors inside the SIP
        Set<String> newErrors = errors.stream().map(e -> errorCause + ": " + e.getErrorCause())
                .collect(Collectors.toSet());
        SIPEntity sip = request.getAips().get(0).getSip();
        sipService.save(sip);
        sessionNotifier.productStoreError(request.getSessionOwner(), request.getSession(), aips);
    }
}
