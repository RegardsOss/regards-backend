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

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
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
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent.ErrorFile;

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
    private INotificationClient notificationClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IStorageClient storageClient;

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
        jobInfoService.createAsQueued(jobInfo);

        // Attach job
        requests.forEach(r -> r.setJobInfo(jobInfo));
    }

    @Override
    public void handleJobError(JobEvent jobEvent) {

        JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
        if (IngestProcessingJob.class.getName().equals(jobInfo.getClassName())) {

            // Load ingest requests
            try {
                Type type = new TypeToken<Set<Long>>() {
                }.getType();
                Set<Long> ids;
                ids = IJob.getValue(jobInfo.getParametersAsMap(), IngestProcessingJob.IDS_PARAMETER, type);
                List<IngestRequest> requests = loadByIds(ids);
                requests.forEach(r -> handleRequestError(r, null));
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
        request.setState(RequestState.GRANTED);

        // Keep track of the request
        ingestRequestRepository.save(request);

        // Publish
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   request.getState(), request.getErrors()));
    }

    @Override
    public void handleRequestDenied(IngestRequest request) {
        request.setState(RequestState.DENIED);

        // Do not keep track of the request
        // Publish DENIED request
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   request.getState(), request.getErrors()));
    }

    @Override
    public void handleRequestError(IngestRequest request, SIPEntity entity) {

        // TODO Unlock the job when request is removed!
        // Lock job
        jobInfoService.lock(request.getJobInfo());

        // Keep track of the error
        saveAndPublishErrorRequest(request, null);

        // Publish failing SIP in current session
        if (entity != null) {
            sessionNotifier.notifySIPCreationFailed(entity);
        }
    }

    @Override
    public void handleRequestSuccess(IngestRequest request, SIPEntity sipEntity, List<AIP> aips) {

        // Save SIP entity
        sipEntity = sipService.saveSIPEntity(sipEntity);

        // Build AIP entities and save them
        List<AIPEntity> aipEntities = aipService.createAndSave(sipEntity, aips);
        // Attach generated AIPs to the current request
        request.setAips(aipEntities);

        // Launch next remote step
        request.setStep(IngestRequestStep.REMOTE_STORAGE_REQUESTED, confProperties.getRemoteRequestTimeout());

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        for (AIPEntity aipEntity : aipEntities) {

            AIP aip = aipEntity.getAip();
            List<StorageMetadata> storages = aipEntity.getIngestMetadata().getStorages();

            for (ContentInformation ci : aip.getProperties().getContentInformations()) {

                OAISDataObject dataObject = ci.getDataObject();

                // Find origin(s) URL (location without storage)
                List<OAISDataObjectLocation> origins = dataObject.getLocations().stream()
                        .filter(l -> l.getStorage() == null).collect(Collectors.toList());
                if (origins.isEmpty()) {
                    // TODO : request error! this case can be validated directly on submission!
                    // Keep track of the error
                    saveAndPublishErrorRequest(request,
                                               String.format("Origin URL not found in AIP (location without storage)"));
                    return;
                }
                URL originUrl = origins.get(0).getUrl();

                // Create a request for each storage
                for (StorageMetadata storage : storages) {
                    files.add(FileStorageRequestDTO
                            .build(dataObject.getFilename(), dataObject.getChecksum(), dataObject.getAlgorithm(),
                                   ci.getRepresentationInformation().getSyntax().getMimeType().toString(),
                                   aip.getId().toString(), originUrl, storage.getStorage(),
                                   Optional.ofNullable(storage.getStorePath())));
                }
            }
        }

        // Request storage for all AIPs of the ingest request
        RequestInfo info = storageClient.store(files);
        // Register request info to identify storage callback events
        request.setRemoteStepGroupId(info.getGroupId());

        // Keep track of the request
        ingestRequestRepository.save(request);
    }

    /**
     * Do not use at the moment, just log.
     */
    @Override
    public void handleRemoteRequestGranted(RequestInfo requestInfo) {
        // Do not track at the moment : the ongoing request could send a success too quickly
        // and could cause unnecessary concurrent access to the database!
        LOGGER.debug("Storage request granted with id \"{}\"", requestInfo.getGroupId());

        // IngestRequest request = ingestRequestRepository.findAllByRemoteStepGroupId(info.getGroupId());
        // request.setStep(IngestRequestStep.REMOTE_STORAGE_GRANTED, confProperties.getRemoteRequestTimeout());
        // ingestRequestRepository.save(request);
    }

    @Override
    public void handleRemoteRequestDenied(RequestInfo requestInfo) {

        // Retrieve request
        IngestRequest request = ingestRequestRepository.findByRemoteStepGroupId(requestInfo.getGroupId());
        request.setState(RequestState.ERROR);

        switch (request.getStep()) {
            case REMOTE_STORAGE_REQUESTED:
                // Request for FILEs storage
                request.setStep(IngestRequestStep.REMOTE_STORAGE_DENIED);
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Remote file storage request denied"));
                break;
            case REMOTE_AIP_STORAGE_REQUESTED:
                // Request for AIPs storage
                request.setStep(IngestRequestStep.REMOTE_AIP_STORAGE_DENIED);
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Remote AIP storage request denied"));
                break;
            default:
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
                break;
        }
    }

    @Override
    public void handleRemoteStoreSuccess(RequestInfo requestInfo) {

        // Retrieve request and related entities SIP & AIPs
        IngestRequest request = ingestRequestRepository.findAllByRemoteStepGroupId(requestInfo.getGroupId());

        switch (request.getStep()) {
            case REMOTE_STORAGE_REQUESTED:
                // Request for FILEs storage successfully completed, now requests AIPs storage
                storeAips(request);
                break;
            case REMOTE_AIP_STORAGE_REQUESTED:
                // Request for AIPs storage successfully completed, now finalize sucessful request
                finalizeSuccessfulRequest(request);
                break;
            default:
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
                break;
        }
    }

    private void storeAips(IngestRequest request) {

        for (@SuppressWarnings("unused")
        AIPEntity aipEntity : request.getAips()) {
            // FIXME update AIP file reference!
        }

        // Launch next remote step
        request.setStep(IngestRequestStep.REMOTE_AIP_STORAGE_REQUESTED, confProperties.getRemoteRequestTimeout());

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        for (AIPEntity aipEntity : request.getAips()) {
            // Create a request for each storage
            // TODO préciser le répertoire de sauvegarde des AIPs
            try {
                files.addAll(aipService.buildAIPStorageRequest(aipEntity.getAip(),
                                                               aipEntity.getIngestMetadata().getStorages()));
            } catch (ModuleException e) {
                // Keep track of the error
                saveAndPublishErrorRequest(request,
                                           String.format("Cannot store AIP file for AIP %s", aipEntity.getAipId()));
                break;
            }
        }

        // Request storage for all AIPs of the ingest request
        RequestInfo info = storageClient.store(files);
        // TODO remove old AIP
        // Register request info to identify storage callback events
        request.setRemoteStepGroupId(info.getGroupId());

        // Keep track of the request
        ingestRequestRepository.save(request);
    }

    private void finalizeSuccessfulRequest(IngestRequest request) {
        request.setState(RequestState.SUCCESS);

        // Clean
        ingestRequestRepository.delete(request);

        // Change AIP state
        for (AIPEntity aipEntity : request.getAips()) {
            aipEntity.setState(AIPState.STORED);
        }

        // Update SIP state
        SIPEntity sipEntity = request.getAips().get(0).getSip();
        sipEntity.setState(SIPState.STORED);

        // Publish SUCCESSFUL request
        publisher.publish(IngestRequestEvent.build(request.getRequestId(), request.getSip().getId(),
                                                   sipEntity.getSipId(), request.getState(), request.getErrors()));

        // Publish new SIP in current session
        sessionNotifier.notifySIPCreated(request.getAips().get(0).getSip());
    }

    @Override
    public void handleRemoteStoreError(RequestInfo requestInfo, Collection<ErrorFile> errors) {

        // Retrieve request
        IngestRequest request = ingestRequestRepository.findByRemoteStepGroupId(requestInfo.getGroupId());

        // Propagate errors
        if (errors != null) {
            errors.forEach(e -> request.addError(e.getErrorCause()));
        }

        switch (request.getStep()) {
            case REMOTE_STORAGE_REQUESTED:
                // Request for FILEs storage
                request.setStep(IngestRequestStep.REMOTE_STORAGE_ERROR);
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Remote file storage request error"));
                break;
            case REMOTE_AIP_STORAGE_REQUESTED:
                // Request for AIPs storage
                request.setStep(IngestRequestStep.REMOTE_AIP_STORAGE_ERROR);
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Remote AIP storage request error"));
                break;
            default:
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format("Unexpected step \"%s\"", request.getStep()));
                break;
        }

    }

    private void saveAndPublishErrorRequest(IngestRequest request, @Nullable String message) {
        // Mutate request
        request.setState(RequestState.ERROR);
        request.addError(String.format("Storage request error with id \"%s\" and SIP provider id \"%s\"",
                                       request.getRequestId(), request.getSip().getId()));
        if (message != null) {
            request.addError(message);
        }

        // Keep track of the error
        ingestRequestRepository.save(request);

        // Publish
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null, null,
                                                   request.getState(), request.getErrors()));
    }
}
