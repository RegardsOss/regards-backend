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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestError;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.ingest.StorageType;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.sip.ISipIdAndVersion;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPStorageService;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
import fr.cnes.regards.modules.ingest.service.job.ChooseVersioningJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.notification.IAIPNotificationService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.service.settings.IIngestSettingsService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Manage ingest requests
 *
 * @author Marc SORDI
 */
@Service
@MultitenantTransactional
public class IngestRequestService implements IIngestRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestService.class);

    public static final String UNEXPECTED_STEP_S_TEMPLATE = "Unexpected step \"%s\"";

    @Autowired
    private IngestConfigurationProperties confProperties;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IRequestService requestService;

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
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @Autowired
    private IIngestSettingsService ingestSettingsService;

    @Autowired
    private IAIPNotificationService aipNotificationService;

    @Override
    public void scheduleIngestProcessingJobByChain(String chainName, Collection<IngestRequest> requests) {

        // Schedule jobs
        LOGGER.debug("Scheduling job to handle {} ingest request(s) on chain {}", requests.size(), chainName);

        Set<Long> ids = requests.stream().map(AbstractRequest::getId).collect(Collectors.toSet());

        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, ids));
        jobParameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, chainName));
        // Lock job info
        JobInfo jobInfo = new JobInfo(false,
                                      IngestJobPriority.INGEST_PROCESSING_JOB_PRIORITY,
                                      jobParameters,
                                      authResolver.getUser(),
                                      IngestProcessingJob.class.getName());
        // Lock job to avoid automatic deletion. The job must be unlock when the link to the request is removed.
        jobInfo.setLocked(true);
        jobInfoService.createAsQueued(jobInfo);

        for (IngestRequest request : requests) {
            // Attach job
            request.setJobInfo(jobInfo);
        }
        requests.forEach(r -> r.setJobInfo(jobInfo));
    }

    @Override
    public boolean handleJobCrash(JobInfo jobInfo) {
        boolean isIngestProcessingJob = IngestProcessingJob.class.getName().equals(jobInfo.getClassName());
        if (isIngestProcessingJob) {

            // Load ingest requests
            try {
                Type type = new TypeToken<Set<Long>>() {

                }.getType();
                Set<Long> ids;
                ids = IJob.getValue(jobInfo.getParametersAsMap(), IngestProcessingJob.IDS_PARAMETER, type);
                List<IngestRequest> requests = loadByIds(ids);
                requests.forEach(r -> handleIngestJobFailed(r, null, jobInfo.getStatus().getStackTrace()));
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                String message = String.format("Ingest request job with id \"%s\" fails with status \"%s\"",
                                               jobInfo.getId(),
                                               jobInfo.getStatus().getStatus());
                LOGGER.error(message, e);
                notificationClient.notify(message, "Ingest job failure", NotificationLevel.ERROR, DefaultRole.ADMIN);
            }
        }
        return isIngestProcessingJob;
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
                                                   request.getSip() != null ? request.getSip().getId() : null,
                                                   null,
                                                   RequestState.GRANTED,
                                                   request.getErrors()));
    }

    @Override
    public void handleRequestDenied(IngestRequest request) {
        // Do not keep track of the request
        // Publish DENIED request
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null,
                                                   null,
                                                   RequestState.DENIED,
                                                   request.getErrors()));
    }

    @Override
    public void handleUnknownChain(List<IngestRequest> requests) {
        // Monitoring
        for (IngestRequest request : requests) {
            sessionNotifier.incrementProductGenerationError(request);
        }
    }

    @Override
    public void handleIngestJobStart(IngestRequest request) {
        // Monitoring
        sessionNotifier.incrementProductGenerationPending(request);
    }

    @Override
    public void handleIngestJobFailed(IngestRequest request, SIPEntity entity, String errorMessage) {
        // Lock job
        if (request.getJobInfo() != null) {
            jobInfoService.lock(request.getJobInfo());
        }
        // Keep track of the error
        saveAndPublishErrorRequest(request, errorMessage);

        // Monitoring
        sessionNotifier.decrementProductGenerationPending(request);
        sessionNotifier.incrementProductGenerationError(request);
    }

    @Override
    public List<AIPEntity> handleIngestJobSucceed(IngestRequest request, SIPEntity sipEntity, List<AIP> aips) {
        // first lets find out which SIP is the last
        ISipIdAndVersion latestSip = sipService.getLatestSip(sipEntity.getProviderId());
        if (latestSip == null) {
            LOGGER.debug("No previous sip {}", sipEntity.getProviderId());
            sipService.updateLastFlag(sipEntity, true);
        } else {
            if (latestSip.getVersion() < sipEntity.getVersion()) {
                // Switch last entity
                LOGGER.debug("Previous version of sip {} found", sipEntity.getProviderId());
                sipService.updateLastFlag(latestSip, false);
                sipService.updateLastFlag(sipEntity, true);
            } else {
                LOGGER.debug("No previous version of sip {}", sipEntity.getProviderId());
                sipService.updateLastFlag(sipEntity, false);
            }
        }
        // Save SIP entity
        sipEntity = sipService.save(sipEntity);

        // Build AIP entities and save them
        // decision whether one aip is the latest for its providerId is handled once we know an AIP is stored
        List<AIPEntity> aipEntities = aipService.createAndSave(sipEntity, aips);
        // Attach generated AIPs to the current request
        request.setAips(aipEntities);
        requestRemoteStorage(request);

        // Monitoring
        sessionNotifier.decrementProductGenerationPending(request);

        return aipEntities;
    }

    /**
     * Optimization method for loading {@link IngestProcessingChain}
     */
    private Map<String, Optional<IngestProcessingChain>> preloadChains(Set<IngestRequest> requests,
                                                                       Map<String, Optional<IngestProcessingChain>> chains) {
        if (requests != null) {
            for (IngestRequest request : requests) {
                String chainName = request.getMetadata().getIngestChain();
                if (!chains.containsKey(chainName)) {
                    chains.put(chainName, processingChainRepository.findOneByName(chainName));
                }
            }
        }
        return chains;
    }

    /**
     * Optimization method for loading last versions of {@link AIPEntity}
     */
    private Map<String, AIPEntity> preloadLastVersions(Set<IngestRequest> requests,
                                                       Map<String, AIPEntity> aipEntities) {
        Set<AIPEntity> lastVersions = Sets.newHashSet();
        if (requests != null) {
            lastVersions = aipService.findLastByProviderIds(requests.stream()
                                                                    .map(r -> r.getProviderId())
                                                                    .collect(Collectors.toSet()));
        }
        return lastVersions.stream().collect(Collectors.toMap(AIPEntity::getProviderId, aip -> aip));
    }

    @Override
    public void requestRemoteStorage(IngestRequest request) {
        // Launch next remote step
        request.setStep(IngestRequestStep.REMOTE_STORAGE_REQUESTED, confProperties.getRemoteRequestTimeout());

        try {
            // Send AIP files storage events, keep these events ids in a list
            List<String> remoteStepGroupIds = aipStorageService.storeAIPFiles(request);

            if (!remoteStepGroupIds.isEmpty()) {
                // Register request info to identify storage callback events
                request.setRemoteStepGroupIds(remoteStepGroupIds);
                // Put the request as un-schedule.
                // The answering event from storage will put again the request to be executed
                request.setState(InternalRequestState.TO_SCHEDULE);
                // Keep track of the request
                saveRequest(request);
                // Monitoring
                sessionNotifier.incrementProductStorePending(request);
            } else {
                // No files to store for the request AIPs. We can immediately store the manifest.
                Set<IngestRequest> requests = Sets.newHashSet(request);
                finalizeSuccessfulRequest(requests,
                                          false,
                                          preloadChains(requests,
                                                        new HashMap<String, Optional<IngestProcessingChain>>()),
                                          preloadLastVersions(requests, new HashMap<String, AIPEntity>()));
            }
        } catch (ModuleException e) {
            // Keep track of the error
            String message = String.format("Cannot send events to store AIP files because they are malformed. Cause: %s",
                                           e.getMessage());
            LOGGER.debug(message, e);
            saveAndPublishErrorRequest(request, message);
            // Monitoring
            // Decrement from above
            sessionNotifier.decrementProductStorePending(request);
            sessionNotifier.incrementProductStoreError(request);
        }
    }

    @Override
    public void handleRemoteRequestDenied(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            // Retrieve request
            Optional<IngestRequest> requestOp = ingestRequestRepository.findOneWithAIPs(ri.getGroupId());
            if (requestOp.isPresent()) {
                IngestRequest request = requestOp.get();
                IngestRequestStep step = request.getStep();
                if (step == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
                    // Save the request was denied at AIP files storage
                    request.setStep(IngestRequestStep.REMOTE_STORAGE_DENIED);
                    request.setState(InternalRequestState.ERROR);
                    // Keep track of the error
                    saveAndPublishErrorRequest(request, "Remote file storage request denied");
                } else {
                    // Keep track of the error
                    saveAndPublishErrorRequest(request, String.format(UNEXPECTED_STEP_S_TEMPLATE, step));
                }

                // Monitoring
                // Decrement from #requestRemoteStorage
                sessionNotifier.decrementProductStorePending(request);
                sessionNotifier.incrementProductStoreError(request);
            }
        }
    }

    @Override
    public void handleRemoteStoreSuccess(Map<RequestInfo, Set<IngestRequest>> requests) {

        // Preload last versions
        Set<IngestRequest> merged = new HashSet<>();
        requests.forEach((k, v) -> merged.addAll(v));
        Map<String, AIPEntity> lastVersions = preloadLastVersions(merged, new HashMap<>());

        Map<String, Optional<IngestProcessingChain>> chains = new HashMap<>();
        for (Entry<RequestInfo, Set<IngestRequest>> entry : requests.entrySet()) {
            handleRemoteStoreSuccess(entry.getKey(),
                                     entry.getValue(),
                                     preloadChains(entry.getValue(), chains),
                                     lastVersions);
        }
    }

    private void handleRemoteStoreSuccess(RequestInfo requestInfo,
                                          Set<IngestRequest> requests,
                                          Map<String, Optional<IngestProcessingChain>> chains,
                                          Map<String, AIPEntity> lastVersions) {

        Set<IngestRequest> toFinalize = Sets.newHashSet();

        for (IngestRequest request : requests) {
            if (request.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
                // Update AIPs with meta returned by storage
                aipStorageService.updateAIPsContentInfosAndLocations(request.getAips(),
                                                                     requestInfo.getSuccessRequests());
                // Check if there is another storage request we're waiting for
                List<String> remoteStepGroupIds = updateRemoteStepGroupId(request, requestInfo);
                if (!remoteStepGroupIds.isEmpty()) {
                    saveRequest(request);
                } else {
                    // The current request is over
                    toFinalize.add(request);
                }
            } else {
                // Keep track of the error
                saveAndPublishErrorRequest(request, String.format(UNEXPECTED_STEP_S_TEMPLATE, request.getStep()));
            }
        }
        finalizeSuccessfulRequest(toFinalize, true, chains, lastVersions);
    }

    private List<String> updateRemoteStepGroupId(IngestRequest request, RequestInfo requestInfo) {
        List<String> remoteStepGroupIds = request.getRemoteStepGroupIds();
        remoteStepGroupIds.remove(requestInfo.getGroupId());
        request.setRemoteStepGroupIds(remoteStepGroupIds);
        return remoteStepGroupIds;
    }

    // NOTE : potential error if 2 instances work on the same provider aip at the same time then ...
    // ... 2 "last" aip may occurs and db exception will be thrown.
    private void finalizeSuccessfulRequest(Collection<IngestRequest> requests,
                                           boolean afterStorage,
                                           Map<String, Optional<IngestProcessingChain>> chains,
                                           Map<String, AIPEntity> lastVersions) {
        if (requests.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();
        List<AbstractRequest> toSchedule = Lists.newArrayList();
        Map<IngestProcessingChain, Set<AIPEntity>> postProcessToSchedule = Maps.newHashMap();
        List<IngestRequestEvent> listIngestRequestEvents = new ArrayList<>();

        for (IngestRequest request : requests) {

            List<AIPEntity> aips = request.getAips();

            // Change AIP state
            for (AIPEntity aipEntity : aips) {
                aipEntity.setState(AIPState.STORED);
                // Find if this is the last version and set last flag accordingly
                aipService.handleVersioning(aipEntity, request.getMetadata().getVersioningMode(), lastVersions);
                aipService.save(aipEntity);

                // Manage post processing
                Optional<IngestProcessingChain> chain = chains.get(request.getMetadata().getIngestChain());
                if (chain.isPresent() && chain.get().getPostProcessingPlugin().isPresent()) {
                    if (postProcessToSchedule.get(chain.get()) != null) {
                        postProcessToSchedule.get(chain.get()).add(aipEntity);
                    } else {
                        postProcessToSchedule.put(chain.get(), Sets.newHashSet(aipEntity));
                    }
                }
            }

            // Monitoring
            // Decrement from #requestRemoteStorage
            if (afterStorage) {
                sessionNotifier.decrementProductStorePending(request);
            }
            // Even if no file is present in AIP, we consider the product as stored
            sessionNotifier.incrementProductStoreSuccess(request);

            // Update SIP state
            if (!aips.isEmpty()) {
                SIPEntity sipEntity = aips.get(0).getSip();
                sipEntity.setState(SIPState.STORED);
                sipService.save(sipEntity);

                // add ingest request event to list of ingest request events to publish
                listIngestRequestEvents.add(IngestRequestEvent.build(request.getRequestId(),
                                                                     request.getSip().getId(),
                                                                     sipEntity.getSipId(),
                                                                     RequestState.SUCCESS));
            } else {
                // Should never happen.  A successfully ingest request is always associated to at least one AIP.
                LOGGER.warn("Finalized IngestRequest ({} / {}) is not associated to any AIP",
                            request.getRequestId(),
                            request.getId());
            }
        }

        // NOTIFICATIONS
        // check if notifications are required - if true send to notifier, if false publish events and delete requests
        if (ingestSettingsService.isActiveNotification()) {
            // Change the step of the request
            aipNotificationService.sendRequestsToNotifier(Sets.newHashSet(requests));
        } else {
            publisher.publish(listIngestRequestEvents);
            requestService.deleteRequests(Sets.newHashSet(requests));
        }

        // POSTPROCESS
        for (Entry<IngestProcessingChain, Set<AIPEntity>> es : postProcessToSchedule.entrySet()) {
            for (AIPEntity aip : es.getValue()) {
                LOGGER.info("New post process request to schedule for aip {} / {}",
                            aip.getProviderId(),
                            aip.getAipId());
                AIPPostProcessRequest req = AIPPostProcessRequest.build(aip,
                                                                        es.getKey()
                                                                          .getPostProcessingPlugin()
                                                                          .get()
                                                                          .getBusinessId());
                toSchedule.add(aipPostProcessRequestRepository.save(req));
                sessionNotifier.incrementPostProcessPending(req);
            }
        }
        requestService.scheduleRequests(toSchedule);
        LOGGER.trace("Successful request handled in {} ms", System.currentTimeMillis() - start);
    }

    @Override
    public void handleRemoteStoreError(IngestRequest request, RequestInfo requestInfo) {
        String errorMessage = null;
        // Propagate errors
        requestInfo.getErrorRequests().forEach(e -> request.addError(e.getErrorCause()));
        if (request.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
            // Update AIP and SIP with current error
            updateRequestWithErrors(request,
                                    requestInfo.getErrorRequests(),
                                    "Error occurred while storing AIP files",
                                    StorageType.STORED_FILE);
            // Update AIPs with success response returned by storage
            aipStorageService.updateAIPsContentInfosAndLocations(request.getAips(), requestInfo.getSuccessRequests());
            // Save error in request status
            request.setStep(IngestRequestStep.REMOTE_STORAGE_ERROR);

        } else {
            errorMessage = String.format(UNEXPECTED_STEP_S_TEMPLATE, request.getStep());
        }
        // Keep track of the error
        saveAndPublishErrorRequest(request, errorMessage);
        // Monitoring
        // Decrement from #requestRemoteStorage
        sessionNotifier.decrementProductStorePending(request);
        // Even if no file is present in AIP, we consider the product as stored
        sessionNotifier.incrementProductStoreError(request);
    }

    @Override
    public void handleRemoteReferenceSuccess(Set<RequestInfo> requests) {
        Map<String, Optional<IngestProcessingChain>> chains = new HashMap<>();
        Set<IngestRequest> requestsToFinilized = Sets.newHashSet();
        for (AbstractRequest request : requestService.getRequests(requests)) {
            IngestRequest iReq = (IngestRequest) request;
            // Check if there is another storage request we're waiting for
            if (iReq.getStep() == IngestRequestStep.REMOTE_STORAGE_REQUESTED) {
                for (RequestInfo ri : requests.stream()
                                              .filter(r -> request.getRemoteStepGroupIds().contains(r.getGroupId()))
                                              .collect(Collectors.toSet())) {
                    aipStorageService.updateAIPsContentInfosAndLocations(iReq.getAips(), ri.getSuccessRequests());
                    List<String> remoteStepGroupIds = updateRemoteStepGroupId(iReq, ri);
                    if (!remoteStepGroupIds.isEmpty()) {
                        saveRequest(iReq);
                    } else {
                        // The current request is over
                        requestsToFinilized.add(iReq);
                    }
                }
            }
        }
        finalizeSuccessfulRequest(requestsToFinilized,
                                  true,
                                  preloadChains(requestsToFinilized, chains),
                                  preloadLastVersions(requestsToFinilized, new HashMap<String, AIPEntity>()));
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
                updateRequestWithErrors(request,
                                        ri.getErrorRequests(),
                                        "Error occurred while storing AIP references",
                                        StorageType.REFERENCED_FILE);
                saveAndPublishErrorRequest(request, null);
                // Monitoring
                // Decrement from #requestRemoteStorage
                sessionNotifier.decrementProductStorePending(request);
                sessionNotifier.incrementProductStoreError(request);
            }
        }
    }

    @Override
    public void ignore(IngestRequest request) {
        request.setState(InternalRequestState.IGNORED);
        ingestRequestRepository.save(request);
        sessionNotifier.incrementProductIgnored(request);
    }

    @Override
    public void waitVersioningMode(IngestRequest request) {
        request.setState(InternalRequestState.WAITING_VERSIONING_MODE);
        ingestRequestRepository.save(request);
        sessionNotifier.incrementProductWaitingVersioningMode(request);
    }

    @Override
    public void scheduleRequestWithVersioningMode(ChooseVersioningRequestParameters filters) {
        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(ChooseVersioningJob.CRITERIA_JOB_PARAM_NAME,
                                                                           filters));
        // Schedule request retry job
        JobInfo jobInfo = new JobInfo(false,
                                      IngestJobPriority.CHOOSE_VERSIONING_JOB_PRIORITY,
                                      jobParameters,
                                      authResolver.getUser(),
                                      ChooseVersioningJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", ChooseVersioningJob.class.getName(), jobInfo.getId());
    }

    @Override
    public void fromWaitingTo(Collection<AbstractRequest> requests, VersioningMode versioningMode) {
        MultiValueMap<String, IngestRequest> ingestRequestToSchedulePerChain = new LinkedMultiValueMap<>();
        for (AbstractRequest request : requests) {
            if (request instanceof IngestRequest ingestRequest) {
                sessionNotifier.decrementProductWaitingVersioningMode(ingestRequest);
                ingestRequest.setState(InternalRequestState.CREATED);
                ingestRequest.getMetadata().setVersioningMode(versioningMode);
                handleRequestGranted(ingestRequest);
                ingestRequestToSchedulePerChain.add(ingestRequest.getMetadata().getIngestChain(), ingestRequest);
            }
        }
        ingestRequestToSchedulePerChain.keySet()
                                       .forEach(chain -> scheduleIngestProcessingJobByChain(chain,
                                                                                            ingestRequestToSchedulePerChain.get(
                                                                                                chain)));
    }

    private void saveAndPublishErrorRequest(IngestRequest request, @Nullable String message) {
        // Mutate request
        request.addError(String.format("The ingest request with id \"%s\" and SIP provider id \"%s\" failed",
                                       request.getRequestId(),
                                       request.getSip().getId()));
        request.setState(InternalRequestState.ERROR);
        if (message != null) {
            request.addError(message);
        }

        // Keep track of the error
        saveRequestAndCheck(request);
        // Publish
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null,
                                                   null,
                                                   RequestState.ERROR,
                                                   request.getErrors()));
    }

    /**
     * Creates or update the given {@link IngestRequest} and lock associated jobs if any.
     *
     * @return saved {@link IngestRequest}
     */
    public IngestRequest saveRequest(IngestRequest request) {
        return saveRequest(request, false);
    }

    public IngestRequest saveRequestAndCheck(IngestRequest request) {
        return saveRequest(request, true);
    }

    private IngestRequest saveRequest(IngestRequest request, boolean checkAips) {
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

    private void updateRequestWithErrors(IngestRequest request,
                                         Collection<RequestResultInfoDTO> errors,
                                         String errorCause,
                                         StorageType type) {
        List<AIPEntity> aips = request.getAips();
        // Iterate overs AIPs and errors
        for (AIPEntity aipEntity : aips) {
            for (RequestResultInfoDTO error : errors) {
                // Check using owner property if the AIP contains the file that was not properly saved
                if (error.getRequestOwners().contains(aipEntity.getAipId())) {
                    // Add the cause to this AIP
                    request.addError(errorCause + ": " + error.getErrorCause());
                    // Add the error information (for STORED_FILE or REFERENCED_FILE for retry action by user)
                    request.addErrorInformation(new IngestRequestError(type,
                                                                       error.getRequestChecksum(),
                                                                       error.getRequestStorage()));
                }
            }
        }
    }
}
