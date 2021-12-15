/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureDeletionJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureDeletionService extends AbstractFeatureService<FeatureDeletionRequest> implements IFeatureDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDeletionService.class);

    private static final String ONLINE_CONF = "ONLINE_CONF";

    @Autowired
    private IFeatureDeletionRequestRepository deletionRepo;

    @Autowired
    private IFeatureCreationRequestRepository creationRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Validator validator;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private FeatureSessionNotifier featureSessionNotifier;

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureDeletionRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();
        List<FeatureDeletionRequest> grantedRequests = new ArrayList<>();
        RequestInfo<FeatureUniformResourceName> requestInfo = new RequestInfo<>();
        // FIXME changer ce fonctionnement!
        Set<String> existingRequestIds = this.deletionRepo.findRequestId();

        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn =
                getSessionInfoByUrn(events.stream().map(FeatureDeletionRequestEvent::getUrn).collect(Collectors.toSet()));

        events.forEach(item -> prepareFeatureDeletionRequest(item, sessionInfoByUrn.get(item.getUrn()), grantedRequests, requestInfo, existingRequestIds));
        LOGGER.trace("------------->>> {} deletion requests prepared in {} ms", grantedRequests.size(), System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        deletionRepo.saveAll(grantedRequests);
        LOGGER.debug("------------->>> {} deletion requests registered in {} ms", grantedRequests.size(), System.currentTimeMillis() - registrationStart);
        return requestInfo;
    }

    private void prepareFeatureDeletionRequest(FeatureDeletionRequestEvent item, ILightFeatureEntity sessionInfo, List<FeatureDeletionRequest> grantedRequests,
                                               RequestInfo<FeatureUniformResourceName> requestInfo, Set<String> existingRequestIds
    ) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId()) || grantedRequests.stream().anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            LOGGER.debug("Error during founded FeatureDeletionRequest validation {}", errors);
            requestInfo.addDeniedRequest(item.getUrn(), ErrorTranslator.getErrors(errors));
            // Monitoring log
            FeatureLogger.deletionDenied(item.getRequestOwner(), item.getRequestId(), item.getUrn(), ErrorTranslator.getErrors(errors));
            // Publish DENIED request (do not persist it in DB)
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION, item.getRequestId(), item.getRequestOwner(), null, item.getUrn(),
                                                        RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            // Update session properties
            featureSessionNotifier.incrementCount(sessionInfo, FeatureSessionProperty.DENIED_DELETE_REQUESTS);
        } else {

            FeatureDeletionRequest request = FeatureDeletionRequest.build(item.getRequestId(), item.getRequestOwner(), item.getRequestDate(), RequestState.GRANTED, null,
                                                                          FeatureRequestStep.LOCAL_DELAYED, item.getPriority(), item.getUrn());
            // Monitoring log
            FeatureLogger.deletionGranted(item.getRequestOwner(), item.getRequestId(), item.getUrn());
            // Publish GRANTED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION, item.getRequestId(), item.getRequestOwner(), null, item.getUrn(),
                                                        RequestState.GRANTED, null));

            // Add to granted request collection
            grantedRequests.add(request);
            requestInfo.addGrantedRequest(item.getUrn(), request.getRequestId());
            // Update session properties
            featureSessionNotifier.incrementCount(sessionInfo, FeatureSessionProperty.DELETE_REQUESTS);
        }
    }

    @Override
    public int scheduleRequests() {

        long scheduleStart = System.currentTimeMillis();
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<Long> requestIds = new HashSet<>();
        List<FeatureDeletionRequest> requestsToSchedule = new ArrayList<>();

        Page<FeatureDeletionRequest> dbRequests = deletionRepo.findRequestsToSchedule(
                FeatureRequestStep.LOCAL_DELAYED,
                OffsetDateTime.now(),
                PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));

        if (!dbRequests.isEmpty()) {

            Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn =
                    getSessionInfoByUrn(dbRequests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toSet()));

            for (FeatureDeletionRequest request : dbRequests.getContent()) {
                requestsToSchedule.add(request);
                requestIds.add(request.getId());
                // Update session properties
                featureSessionNotifier.incrementCount(sessionInfoByUrn.get(request.getUrn()), FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
            deletionRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureDeletionJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false, requestsToSchedule.get(0).getPriority().getPriorityLevel(),
                                          jobParameters, authResolver.getUser(), FeatureDeletionJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("------------->>> {} deletion requests scheduled in {} ms", requestsToSchedule.size(), System.currentTimeMillis() - scheduleStart);
            return requestIds.size();
        }
        return 0;
    }

    @Override
    public void processRequests(List<FeatureDeletionRequest> requests, FeatureDeletionJob featureDeletionJob) {

        // Retrieve features
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureRepo
                .findByUrnIn(requests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(FeatureEntity::getUrn, Function.identity()));

        // Dispatch requests
        Set<FeatureDeletionRequest> requestsAlreadyDeleted = new HashSet<>();
        Map<FeatureDeletionRequest, FeatureEntity> requestsWithFiles = new HashMap<>();
        Map<FeatureDeletionRequest, FeatureEntity> requestsWithoutFiles = new HashMap<>();

        for (FeatureDeletionRequest fdr : requests) {
            FeatureEntity entity = featureByUrn.get(fdr.getUrn());
            if (entity == null) {
                requestsAlreadyDeleted.add(fdr);
            } else if (haveFiles(fdr, entity)) {
                requestsWithFiles.put(fdr, entity);
            } else {
                if ((entity.getFeature() != null) && (entity.getFeature().getHistory() != null)) {
                    entity.getFeature().getHistory().setDeletedBy(fdr.getRequestOwner());
                }
                requestsWithoutFiles.put(fdr, entity);
            }
        }

        // Manage dispatched requests
        boolean isToNotify = notificationSettingsService.isActiveNotification();
        manageRequestsAlreadyDeleted(requestsAlreadyDeleted, isToNotify, featureDeletionJob);
        manageRequestsWithFiles(requestsWithFiles, featureDeletionJob);
        manageRequestsWithoutFile(requestsWithoutFiles, isToNotify, featureDeletionJob);
    }

    private void manageRequestsAlreadyDeleted(Set<FeatureDeletionRequest> requestsAlreadyDeleted, boolean isToNotify, FeatureDeletionJob featureDeletionJob) {
        if (!requestsAlreadyDeleted.isEmpty()) {
            // PROPAGATE to NOTIFIER if required
            if (isToNotify) {
                String unknown = "unknown";
                for (FeatureDeletionRequest fdr : requestsAlreadyDeleted) {
                    // Build fake incomplete feature
                    Feature fakeFeature = Feature.build(unknown, unknown, fdr.getUrn(), IGeometry.unlocated(), EntityType.DATA, unknown);
                    fdr.setToNotify(fakeFeature, null, null);
                    fdr.setAlreadyDeleted(true);
                    fdr.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                    featureDeletionJob.advanceCompletion();
                }
                deletionRepo.saveAll(requestsAlreadyDeleted);
            } else {
                this.deletionRepo.deleteInBatch(requestsAlreadyDeleted);
            }

            // PROPAGATE to CATALOG
            requestsAlreadyDeleted.forEach(r -> publisher.publish(FeatureEvent.buildFeatureDeleted(r.getUrn().toString())));

            // Feedbacks for already deleted features
            Set<String> errors = Sets.newHashSet("Feature already deleted. Skipping silently!");
            for (FeatureDeletionRequest fdr : requestsAlreadyDeleted) {
                // Monitoring log
                FeatureLogger.deletionSuccess(fdr.getRequestOwner(), fdr.getRequestId(), fdr.getUrn());
                // Send feedback
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION, fdr.getRequestId(), fdr.getRequestOwner(), null, fdr.getUrn(),
                                                            RequestState.SUCCESS, errors));
            }
        }
    }

    private void manageRequestsWithFiles(Map<FeatureDeletionRequest, FeatureEntity> requestsWithFiles,
            FeatureDeletionJob featureDeletionJob) {
        // Request file deletion
        for (Entry<FeatureDeletionRequest, FeatureEntity> entry : requestsWithFiles.entrySet()) {
            publishFiles(entry.getKey(), entry.getValue());
            featureDeletionJob.advanceCompletion();
        }
        // Save all request with files waiting for file deletion
        this.deletionRepo.saveAll(requestsWithFiles.keySet());
        // No feedback at the moment
    }

    private void manageRequestsWithoutFile(Map<FeatureDeletionRequest, FeatureEntity> requestsWithoutFiles, boolean isToNotify, FeatureDeletionJob featureDeletionJob) {
        sendFeedbacksAndClean(requestsWithoutFiles, isToNotify, featureDeletionJob);
    }

    private void sendFeedbacksAndClean(Map<FeatureDeletionRequest, FeatureEntity> successfulRequests, boolean isToNotify, FeatureDeletionJob featureDeletionJob) {

        // PROPAGATE to CATALOG
        successfulRequests.values().forEach(f -> publisher.publish(FeatureEvent.buildFeatureDeleted(f.getUrn().toString())));

        // Feedbacks for deleted features
        for (Map.Entry<FeatureDeletionRequest, FeatureEntity> entry : successfulRequests.entrySet()) {
            FeatureEntity entity = entry.getValue();
            FeatureDeletionRequest fdr = entry.getKey();
            fdr.setToNotify(entity.getFeature(), entity.getSessionOwner(), entity.getSession());
            // Monitoring log
            FeatureLogger.deletionSuccess(fdr.getRequestOwner(), fdr.getRequestId(), fdr.getUrn());
            if (featureDeletionJob != null) {
                // featureDeletionJob can be null in case this method is called outside the context of a job
                featureDeletionJob.advanceCompletion();
            }
            // Publish successful request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION, fdr.getRequestId(), fdr.getRequestOwner(), entity.getProviderId(), fdr.getUrn(),
                                                        RequestState.SUCCESS));
            // Update requests in case of notification
            if (isToNotify) {
                fdr.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                fdr.setAlreadyDeleted(false);
            }
        }
        doOnSuccess(successfulRequests.keySet());

        // PREPARE PROPAGATION to NOTIFIER if required
        if (isToNotify) {
            // If notification is required, requests are not over and should be saved.
            deletionRepo.saveAll(successfulRequests.keySet());
        } else {
            // If no notification required, requests are over and can be deleted
            doOnTerminated(successfulRequests.keySet());
            this.deletionRepo.deleteInBatch(successfulRequests.keySet());
        }

        // Delete features, related requests will be deleted once we know notifier has successfully sent the notification about it
        this.creationRepo.deleteByFeatureEntityIn(successfulRequests.values());
        this.featureRepo.deleteAll(successfulRequests.values());
    }

    private boolean haveFiles(FeatureDeletionRequest fdr, FeatureEntity feature) {
        // if non existing urn we will skip this check
        if (feature == null) {
            LOGGER.warn(String.format("Trying to delete a non existing feature with urn %s", fdr.getUrn().toString()));
            return true;
        }
        return (feature.getFeature().getFiles() != null) && !feature.getFeature().getFiles().isEmpty();
    }

    /**
     * Publish command to delete all contained files inside the {@link FeatureDeletionRequest} to
     * storage
     *
     */
    private FeatureDeletionRequest publishFiles(FeatureDeletionRequest fdr, FeatureEntity feature) {
        fdr.setStep(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED);
        for (FeatureFile file : feature.getFeature().getFiles()) {
            FeatureFileAttributes attribute = file.getAttributes();
            fdr.setGroupId(this.storageClient.delete(FileDeletionRequestDTO.build(attribute.getChecksum(), ONLINE_CONF,
                                                                                  feature.getFeature().getUrn()
                                                                                          .toString(),
                                                                                  feature.getSessionOwner(),
                                                                                  feature.getSession(), false))
                                   .getGroupId());
        }
        return fdr;
    }

    @Override
    public void processStorageRequests(Set<String> groupIds) {
        Set<FeatureDeletionRequest> requests = this.deletionRepo.findByGroupIdIn(groupIds);

        // Retrieve features
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureRepo
                .findByUrnIn(requests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(FeatureEntity::getUrn, Function.identity()));

        Map<FeatureDeletionRequest, FeatureEntity> sucessfullRequests = new HashMap<>();
        for (FeatureDeletionRequest fdr : requests) {
            sucessfullRequests.put(fdr, featureByUrn.get(fdr.getUrn()));
        }
        sendFeedbacksAndClean(sucessfullRequests, notificationSettingsService.isActiveNotification(), null);
    }

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(@Valid FeatureDeletionCollection collection) {
        // Build events to reuse event registration code
        List<FeatureDeletionRequestEvent> toTreat = new ArrayList<>();
        for (FeatureUniformResourceName urn : collection.getFeaturesUrns()) {
            toTreat.add(FeatureDeletionRequestEvent.build(authResolver.getUser(), urn, collection.getPriority()));
        }
        return registerRequests(toTreat);
    }

    @Override
    public FeatureRequestType getRequestType() {
        return FeatureRequestType.DELETION;
    }

    @Override
    protected void logRequestDenied(String requestOwner, String requestId, Set<String> errors) {
        FeatureLogger.deletionDenied(requestOwner, requestId, null, errors);
    }

    @Override
    public Page<FeatureDeletionRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page) {
        return deletionRepo.findAll(FeatureDeletionRequestSpecification.searchAllByFilters(selection, page), page);
    }

    @Override
    public RequestsInfo getInfo(FeatureRequestsSelectionDTO selection) {
        if ((selection.getFilters() != null) && ((selection.getFilters().getState() != null)
                && (selection.getFilters().getState() != RequestState.ERROR))) {
            return RequestsInfo.build(0L);
        } else {
            selection.getFilters().withState(RequestState.ERROR);
            return RequestsInfo.build(deletionRepo
                    .count(FeatureDeletionRequestSpecification.searchAllByFilters(selection, PageRequest.of(0, 1))));
        }
    }

    @Override
    protected IAbstractFeatureRequestRepository<FeatureDeletionRequest> getRequestsRepository() {
        return deletionRepo;
    }

    @Override
    protected FeatureDeletionRequest updateForRetry(FeatureDeletionRequest request) {
        // Nothing to do
        return request;
    }

    @Override
    protected void sessionInfoUpdateForRetry(Collection<FeatureDeletionRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn =
                getSessionInfoByUrn(requests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toSet()));
        Map<FeatureUniformResourceName, FeatureRequestStep> errorStepByUrn = requests.stream()
                .collect(Collectors.toMap(FeatureDeletionRequest::getUrn, FeatureDeletionRequest::getLastExecErrorStep));

        requests.forEach(r -> {
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),r.getSessionToNotify(), FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
            if (FeatureRequestStep.REMOTE_NOTIFICATION_ERROR.equals(errorStepByUrn.get(r.getUrn()))) {
                featureSessionNotifier.incrementCount(r.getSourceToNotify(),r.getSessionToNotify(), FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
        });
    }

    @Override
    protected void sessionInfoUpdateForDelete(Collection<FeatureDeletionRequest> requests) {
        requests.forEach((r) -> {
            if (r.getSourceToNotify() != null && r.getSessionToNotify() != null) {
                featureSessionNotifier.decrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
                featureSessionNotifier.decrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.DELETE_REQUESTS);
            }
        });
    }

    @Override
    public void doOnSuccess(Collection<FeatureDeletionRequest> requests) {
        requests.forEach((r) -> {
            if (r.getSourceToNotify() != null && r.getSessionToNotify() != null) {
                featureSessionNotifier.incrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.DELETED_PRODUCTS);
                featureSessionNotifier.decrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.REFERENCED_PRODUCTS);
            }
        });
    }

    @Override
    public void doOnTerminated(Collection<FeatureDeletionRequest> requests) {
        requests.forEach((r) -> {
            if (r.getSourceToNotify() != null && r.getSessionToNotify() != null) {
                featureSessionNotifier.decrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
        });
    }

    @Override
    public void doOnError(Collection<FeatureDeletionRequest> requests) {
        requests.forEach((r) -> {
            if (r.getSourceToNotify() != null && r.getSessionToNotify() != null) {
                featureSessionNotifier.incrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
                featureSessionNotifier.decrementCount(r.getSourceToNotify(), r.getSessionToNotify(),
                                                      FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
        });
    }
}
