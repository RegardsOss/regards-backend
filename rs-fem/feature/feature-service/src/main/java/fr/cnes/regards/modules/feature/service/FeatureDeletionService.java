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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.feature.dao.FeatureDeletionRequestSpecificationBuilder;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.domain.AbstractFeatureEntity;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
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
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureDeletionService extends AbstractFeatureService<FeatureDeletionRequest>
    implements IFeatureDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDeletionService.class);

    @Autowired
    private IFeatureDeletionRequestRepository featureDeletionRequestRepository;

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepository;

    @Autowired
    private Validator validator;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private FeatureSessionNotifier featureSessionNotifier;

    @Autowired
    private FeatureService featureService;

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureDeletionRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();
        List<FeatureDeletionRequest> grantedRequests = new ArrayList<>();
        RequestInfo<FeatureUniformResourceName> requestInfo = new RequestInfo<>();
        Set<String> existingRequestIds = this.featureDeletionRequestRepository.findRequestId();

        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(events.stream()
                                                                                                          .map(
                                                                                                              FeatureDeletionRequestEvent::getUrn)
                                                                                                          .collect(
                                                                                                              Collectors.toSet()));

        events.forEach(item -> prepareFeatureDeletionRequest(item,
                                                             sessionInfoByUrn.get(item.getUrn()),
                                                             grantedRequests,
                                                             requestInfo,
                                                             existingRequestIds));
        LOGGER.trace("------------->>> {} deletion requests prepared in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        featureDeletionRequestRepository.saveAll(grantedRequests);
        LOGGER.debug("------------->>> {} deletion requests registered in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);
        return requestInfo;
    }

    private void prepareFeatureDeletionRequest(FeatureDeletionRequestEvent item,
                                               ILightFeatureEntity sessionInfo,
                                               List<FeatureDeletionRequest> grantedRequests,
                                               RequestInfo<FeatureUniformResourceName> requestInfo,
                                               Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId()) || grantedRequests.stream()
                                                                               .anyMatch(request -> request.getRequestId()
                                                                                                           .equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            LOGGER.debug("Error during founded FeatureDeletionRequest validation {}", errors);
            requestInfo.addDeniedRequest(item.getUrn(), ErrorTranslator.getErrors(errors));
            // Monitoring log
            FeatureLogger.deletionDenied(item.getRequestOwner(),
                                         item.getRequestId(),
                                         item.getUrn(),
                                         ErrorTranslator.getErrors(errors));
            // Publish DENIED request (do not persist it in DB)
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                        item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        null,
                                                        item.getUrn(),
                                                        RequestState.DENIED,
                                                        ErrorTranslator.getErrors(errors)));
            // Update session properties
            featureSessionNotifier.incrementCount(sessionInfo, FeatureSessionProperty.DENIED_DELETE_REQUESTS);
        } else {

            FeatureDeletionRequest request = FeatureDeletionRequest.build(item.getRequestId(),
                                                                          item.getRequestOwner(),
                                                                          item.getRequestDate(),
                                                                          RequestState.GRANTED,
                                                                          null,
                                                                          FeatureRequestStep.LOCAL_DELAYED,
                                                                          item.getPriority(),
                                                                          item.getUrn());
            // Monitoring log
            FeatureLogger.deletionGranted(item.getRequestOwner(), item.getRequestId(), item.getUrn());
            // Publish GRANTED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                        item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        null,
                                                        item.getUrn(),
                                                        RequestState.GRANTED,
                                                        null));

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

        Collection<FeatureDeletionRequest> deletionRequestsToSchedule = featureDeletionRequestRepository.findRequestsToSchedule(
            0,
            properties.getMaxBulkSize());
        // Check blocked ones
        deletionRequestsToSchedule = updateBlockedRequestAndReturnNotBlockedRequests(deletionRequestsToSchedule);

        if (!deletionRequestsToSchedule.isEmpty()) {
            Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(
                deletionRequestsToSchedule.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toSet()));

            Optional<PriorityLevel> highestPriorityLevel = deletionRequestsToSchedule.stream()
                                                                                     .max((p1, p2) -> Math.max(p1.getPriority()
                                                                                                                 .getPriorityLevel(),
                                                                                                               p2.getPriority()
                                                                                                                 .getPriorityLevel()))
                                                                                     .map(AbstractRequest::getPriority);

            for (FeatureDeletionRequest request : deletionRequestsToSchedule) {
                requestsToSchedule.add(request);
                requestIds.add(request.getId());
                // Update session properties
                featureSessionNotifier.incrementCount(sessionInfoByUrn.get(request.getUrn()),
                                                      FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
            featureDeletionRequestRepository.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureDeletionJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the highest priority of the requests to schedule
            JobInfo jobInfo = new JobInfo(false,
                                          highestPriorityLevel.orElse(PriorityLevel.NORMAL).getPriorityLevel(),
                                          jobParameters,
                                          authResolver.getUser(),
                                          FeatureDeletionJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("------------->>> {} deletion requests scheduled in {} ms",
                         requestsToSchedule.size(),
                         System.currentTimeMillis() - scheduleStart);
            return requestIds.size();
        }
        return 0;
    }

    /**
     * Check given requests if they are blocked by a product blocking dissemination.
     *
     * @return requests not blocked.
     */
    private Collection<FeatureDeletionRequest> updateBlockedRequestAndReturnNotBlockedRequests(Collection<FeatureDeletionRequest> deleteRequests) {
        // Log all forced deletion requests
        deleteRequests.stream()
                      .filter(FeatureDeletionRequest::isForceDeletion)
                      .forEach(request -> FeatureLogger.deletionForced(request.getRequestOwner(),
                                                                       request.getRequestId(),
                                                                       request.getUrn()));
        // Retrieve products urn for request deletion without force deletion.
        // Requests with force deletion can be processed without checking dissemination status
        Set<FeatureUniformResourceName> urnsToDelete = deleteRequests.stream()
                                                                     .filter(request -> !request.isForceDeletion())
                                                                     .map(AbstractFeatureRequest::getUrn)
                                                                     .collect(Collectors.toSet());
        Collection<FeatureUniformResourceName> blocked = featureEntityRepository.findFeatureUrnWaitingBlockingDissemination(
            urnsToDelete);
        featureDeletionRequestRepository.updateStepByUrn(FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION, blocked);
        return deleteRequests.stream().filter(r -> !blocked.contains(r.getUrn())).toList();
    }

    @Override
    public void processRequests(List<FeatureDeletionRequest> requests, FeatureDeletionJob featureDeletionJob) {
        // Retrieve features
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureEntityRepository.findCompleteByUrnIn(
                                                                              requests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toList()))
                                                                                                  .stream()
                                                                                                  .collect(Collectors.toMap(
                                                                                                      FeatureEntity::getUrn,
                                                                                                      Function.identity()));
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

    protected void postRequestDeleted(Collection<FeatureDeletionRequest> deletedRequests) {
        // Nothing to do
    }

    private void manageRequestsAlreadyDeleted(Set<FeatureDeletionRequest> requestsAlreadyDeleted,
                                              boolean isToNotify,
                                              FeatureDeletionJob featureDeletionJob) {
        if (requestsAlreadyDeleted.isEmpty()) {
            return;
        }
        // PROPAGATE to NOTIFIER if required
        if (isToNotify) {
            String unknown = "unknown";
            for (FeatureDeletionRequest fdr : requestsAlreadyDeleted) {
                // Build fake incomplete feature
                Feature fakeFeature = Feature.build(unknown,
                                                    unknown,
                                                    fdr.getUrn(),
                                                    IGeometry.unlocated(),
                                                    EntityType.DATA,
                                                    unknown);
                fdr.setToNotify(fakeFeature, null, null);
                fdr.setAlreadyDeleted(true);
                fdr.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);

                featureDeletionJob.advanceCompletion();
            }
            featureDeletionRequestRepository.saveAll(requestsAlreadyDeleted);
        } else {
            this.featureDeletionRequestRepository.deleteAllInBatch(requestsAlreadyDeleted);
        }

        // PROPAGATE to CATALOG
        requestsAlreadyDeleted.forEach(r -> publisher.publish(FeatureEvent.buildFeatureDeleted(r.getUrn().toString())));

        // Feedbacks for already deleted features
        Set<String> errors = Sets.newHashSet("Feature already deleted. Skipping silently!");
        for (FeatureDeletionRequest fdr : requestsAlreadyDeleted) {
            // Monitoring log
            FeatureLogger.deletionSuccess(fdr.getRequestOwner(), fdr.getRequestId(), fdr.getUrn());
            // Send feedback
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                        fdr.getRequestId(),
                                                        fdr.getRequestOwner(),
                                                        null,
                                                        fdr.getUrn(),
                                                        RequestState.SUCCESS,
                                                        errors));
        }
    }

    private void manageRequestsWithFiles(Map<FeatureDeletionRequest, FeatureEntity> requestsWithFiles,
                                         FeatureDeletionJob featureDeletionJob) {
        for (Entry<FeatureDeletionRequest, FeatureEntity> entry : requestsWithFiles.entrySet()) {
            FeatureDeletionRequest featureDeletionrequest = entry.getKey();
            // If request is not set a force deletion, check if a dissemination is pending on the feature to delete.
            // If so, the deletion request must be blocked until dissemination is recieved.
            if (!entry.getKey().isForceDeletion()) {
                entry.getValue().getDisseminationsInfo().forEach(disseminationInfo -> {
                    if (disseminationInfo.isBlocking() && disseminationInfo.getAckDate() == null) {
                        // Monitoring log
                        FeatureLogger.deletionBlocked(disseminationInfo.getLabel(),
                                                      featureDeletionrequest.getRequestOwner(),
                                                      featureDeletionrequest.getRequestId(),
                                                      featureDeletionrequest.getUrn());
                        featureDeletionrequest.setStep(FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION);
                    }
                });
            }
            // Request file deletion
            if (featureDeletionrequest.getStep() != FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION) {
                publishFiles(featureDeletionrequest, entry.getValue());
            }
            featureDeletionJob.advanceCompletion();
        }
        // Save all request with files waiting for file deletion or new status waiting for dissemination
        this.featureDeletionRequestRepository.saveAll(requestsWithFiles.keySet());
        // No feedback at the moment
    }

    private void manageRequestsWithoutFile(Map<FeatureDeletionRequest, FeatureEntity> requestsWithoutFiles,
                                           boolean isToNotify,
                                           FeatureDeletionJob featureDeletionJob) {
        sendFeedbacksAndClean(requestsWithoutFiles, isToNotify, featureDeletionJob);
    }

    private void sendFeedbacksAndClean(Map<FeatureDeletionRequest, FeatureEntity> mapDeletionRequestsAndFeatures,
                                       boolean isToNotify,
                                       @Nullable FeatureDeletionJob featureDeletionJob) {

        if (mapDeletionRequestsAndFeatures.isEmpty()) {
            return;
        }
        Set<FeatureDeletionRequest> deletionRequests = mapDeletionRequestsAndFeatures.keySet();

        // Remove possible null values for requests associated to feature already deleted.
        List<FeatureEntity> associatedFeatureEntities = mapDeletionRequestsAndFeatures.values()
                                                                                      .stream()
                                                                                      .filter(Objects::nonNull)
                                                                                      .toList();

        List<Long> featureEntitiesNotToDelete = new ArrayList<>();
        List<Long> featureDeletionRequestsNotToDelete = new ArrayList<>();
        // Update request and send feedback (notification and success events)
        for (Map.Entry<FeatureDeletionRequest, FeatureEntity> entry : mapDeletionRequestsAndFeatures.entrySet()) {
            updateRequest(isToNotify,
                          featureDeletionJob,
                          entry,
                          featureEntitiesNotToDelete,
                          featureDeletionRequestsNotToDelete);
        }
        // Filter feature to delete (not WAITING_BLOCKING_DISSEMINATION)
        Set<FeatureEntity> featureEntitiesToDelete = associatedFeatureEntities.stream()
                                                                              .filter(featureEntity -> !featureEntitiesNotToDelete.contains(
                                                                                  featureEntity.getId()))
                                                                              .collect(Collectors.toSet());
        // Propagate feature to delete to RS-CATALOG
        featureEntitiesToDelete.forEach(featureEntity -> publisher.publish(FeatureEvent.buildFeatureDeleted(
            featureEntity.getUrn().toString())));

        doOnSuccess(deletionRequests);

        // Prepare propagation to RS-NOTIFIER if required
        if (isToNotify) {
            // If notification is required, requests are not over and should be saved.
            featureDeletionRequestRepository.saveAll(deletionRequests);
        } else {
            // If no notification required, requests are over and can be deleted
            doOnTerminated(deletionRequests);
            featureDeletionRequestRepository.deleteAllInBatch(deletionRequests.stream()
                                                                              .filter(featureDeletionRequest -> !featureDeletionRequestsNotToDelete.contains(
                                                                                  featureDeletionRequest.getId()))
                                                                              .toList());
        }
        // Delete features, related requests will be deleted once we know notifier has successfully sent the notification about it
        featureCreationRequestRepository.deleteByFeatureEntityIn(associatedFeatureEntities);

        LOGGER.info("Deleting {} features in database ...", featureEntitiesToDelete.size());
        long registrationStart = System.currentTimeMillis();
        featureEntityRepository.deleteByIdIn(featureEntitiesToDelete.stream()
                                                                    .map(AbstractFeatureEntity::getId)
                                                                    .toList());
        LOGGER.info("Deletion done in {}ms", System.currentTimeMillis() - registrationStart);
    }

    /**
     * If request is blocked by a pending dissemination :
     * - Update given request to WAITING_BLOCKING_DISSEMINATION step
     * - Add entity to featureEntitiesNotToDelete
     * - Add request to featureDeletionRequestsNotToDelete
     * If deletion request can be done :
     * - Publish success amqp event response
     * If deletion request needs to be notifier :
     * - Update given request to LOCAL_TO_BE_NOTIFIED step
     */
    private void updateRequest(boolean isToNotify,
                               FeatureDeletionJob featureDeletionJob,
                               Entry<FeatureDeletionRequest, FeatureEntity> featureDeletionRequestAndFeatureEntity,
                               List<Long> featureEntitiesNotToDelete,
                               List<Long> featureDeletionRequestsNotToDelete) {
        FeatureEntity featureEntity = featureDeletionRequestAndFeatureEntity.getValue();
        if (featureEntity != null) {
            FeatureDeletionRequest fdr = featureDeletionRequestAndFeatureEntity.getKey();
            fdr.setToNotify(featureEntity.getFeature(), featureEntity.getSessionOwner(), featureEntity.getSession());

            // If request is not set a force deletion, check if a dissemination is pending on the feature to delete.
            // If so, the deletion request must be blocked until dissemination is received.
            if (!featureDeletionRequestAndFeatureEntity.getKey().isForceDeletion()) {
                featureEntity.getDisseminationsInfo().forEach(disseminationInfo -> {
                    if (disseminationInfo.isBlocking() && disseminationInfo.getAckDate() == null) {
                        // Monitoring log
                        FeatureLogger.deletionBlocked(disseminationInfo.getLabel(),
                                                      fdr.getRequestOwner(),
                                                      fdr.getRequestId(),
                                                      fdr.getUrn());
                        fdr.setStep(FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION);
                        featureEntitiesNotToDelete.add(featureEntity.getId());
                        featureDeletionRequestsNotToDelete.add(fdr.getId());
                    }
                });
            }

            if (featureDeletionJob != null) {
                // featureDeletionJob can be null in case this method is called outside the context of a job
                featureDeletionJob.advanceCompletion();
            }
            if (fdr.getStep() != FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION) {
                // Monitoring log
                FeatureLogger.deletionSuccess(fdr.getRequestOwner(), fdr.getRequestId(), fdr.getUrn());
                // Publish successful request
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                            fdr.getRequestId(),
                                                            fdr.getRequestOwner(),
                                                            featureEntity.getProviderId(),
                                                            fdr.getUrn(),
                                                            RequestState.SUCCESS));
                // Update requests in case of notification
                if (isToNotify) {
                    fdr.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                    fdr.setAlreadyDeleted(false);
                }
            }
        }
    }

    private boolean haveFiles(FeatureDeletionRequest fdr, FeatureEntity feature) {
        // if non-existing urn we will skip this check
        if (feature == null) {
            LOGGER.warn("Trying to delete a non existing feature with urn {}", fdr.getUrn());
            return true;
        }
        return (feature.getFeature().getFiles() != null) && !feature.getFeature().getFiles().isEmpty();
    }

    /**
     * Publish command to delete all contained files inside the {@link FeatureDeletionRequest} to storage
     */
    private void publishFiles(FeatureDeletionRequest fdr, FeatureEntity feature) {
        fdr.setStep(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED);
        List<FileDeletionDto> storageRequests = new ArrayList<>();
        for (FeatureFile file : feature.getFeature().getFiles()) {
            FeatureFileAttributes attribute = file.getAttributes();
            for (FeatureFileLocation location : file.getLocations()) {
                // Create a storage request for each location of the file
                if (location.getStorage() != null) {
                    storageRequests.add(FileDeletionDto.build(attribute.getChecksum(),
                                                              location.getStorage(),
                                                              feature.getFeature().getUrn().toString(),
                                                              feature.getSessionOwner(),
                                                              feature.getSession(),
                                                              false));
                } else {
                    LOGGER.warn("Location is null for file url=[{}], which means this feature file location has never "
                                + "been saved", location.getUrl());

                }
            }
        }
        // If multiple group is returned we only save the first one.
        // Multiple groups can be returned if the number of requests is over the limit in one group
        Collection<fr.cnes.regards.modules.filecatalog.client.RequestInfo> responseInfos = this.storageClient.delete(
            storageRequests);
        if (responseInfos.size() > 1) {
            LOGGER.warn(
                "Multiple storage request group created for a single FeatureDeletionRequest. Only the first one will be monitored by deletion process");
        }
        responseInfos.stream().findFirst().ifPresent(responseInfo -> fdr.setGroupId(responseInfo.getGroupId()));
    }

    @Override
    public void processStorageRequests(Set<String> groupIds) {
        Set<FeatureDeletionRequest> requests = this.featureDeletionRequestRepository.findByGroupIdIn(groupIds);

        // Retrieve features
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureEntityRepository.findCompleteByUrnIn(
                                                                              requests.stream().map(FeatureDeletionRequest::getUrn).toList())
                                                                                                  .stream()
                                                                                                  .collect(Collectors.toMap(
                                                                                                      FeatureEntity::getUrn,
                                                                                                      Function.identity()));

        Map<FeatureDeletionRequest, FeatureEntity> successfulRequests = new HashMap<>();
        for (FeatureDeletionRequest fdr : requests) {
            successfulRequests.put(fdr, featureByUrn.get(fdr.getUrn()));
        }
        sendFeedbacksAndClean(successfulRequests, notificationSettingsService.isActiveNotification(), null);
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
    public Page<FeatureDeletionRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page) {
        return featureDeletionRequestRepository.findAll(new FeatureDeletionRequestSpecificationBuilder().withParameters(
            filters).build(), page);
    }

    @Override
    public RequestsInfo getInfo(SearchFeatureRequestParameters filters) {
        if (filters.getStates() != null && filters.getStates().getValues() != null && !filters.getStates()
                                                                                              .getValues()
                                                                                              .contains(RequestState.ERROR)) {
            return RequestsInfo.build(0L);
        } else {
            filters.withStatesIncluded(List.of(RequestState.ERROR));
            return RequestsInfo.build(featureDeletionRequestRepository.count(new FeatureDeletionRequestSpecificationBuilder().withParameters(
                filters).build()));
        }
    }

    @Override
    protected IAbstractFeatureRequestRepository<FeatureDeletionRequest> getRequestsRepository() {
        return featureDeletionRequestRepository;
    }

    @Override
    protected FeatureDeletionRequest updateForRetry(FeatureDeletionRequest request) {
        // Nothing to do
        return request;
    }

    @Override
    protected void sessionInfoUpdateForRetry(Collection<FeatureDeletionRequest> requests) {
        Map<FeatureUniformResourceName, FeatureRequestStep> errorStepByUrn = requests.stream()
                                                                                     .collect(Collectors.toMap(
                                                                                         FeatureDeletionRequest::getUrn,
                                                                                         FeatureDeletionRequest::getLastExecErrorStep));

        requests.stream().filter(r -> r.getSourceToNotify() != null && r.getSessionToNotify() != null).forEach(r -> {
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
            if (FeatureRequestStep.REMOTE_NOTIFICATION_ERROR.equals(errorStepByUrn.get(r.getUrn()))) {
                featureSessionNotifier.incrementCount(r.getSourceToNotify(),
                                                      r.getSessionToNotify(),
                                                      FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
            }
        });
    }

    @Override
    protected void sessionInfoUpdateForDelete(Collection<FeatureDeletionRequest> requests) {
        requests.stream().filter(r -> r.getSourceToNotify() != null && r.getSessionToNotify() != null).forEach(r -> {
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.DELETE_REQUESTS);
        });
    }

    @Override
    public void doOnSuccess(Collection<FeatureDeletionRequest> requests) {
        requests.stream().filter(r -> r.getSourceToNotify() != null && r.getSessionToNotify() != null).forEach(r -> {
            featureSessionNotifier.incrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.DELETED_PRODUCTS);
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.REFERENCED_PRODUCTS);
        });
    }

    @Override
    public void doOnTerminated(Collection<FeatureDeletionRequest> requests) {
        requests.stream().filter(r -> r.getSourceToNotify() != null && r.getSessionToNotify() != null).forEach(r -> {
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
        });
    }

    @Override
    public void doOnError(Collection<FeatureDeletionRequest> requests) {
        requests.stream().filter(r -> r.getSourceToNotify() != null && r.getSessionToNotify() != null).forEach(r -> {
            featureSessionNotifier.incrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.IN_ERROR_DELETE_REQUESTS);
            featureSessionNotifier.decrementCount(r.getSourceToNotify(),
                                                  r.getSessionToNotify(),
                                                  FeatureSessionProperty.RUNNING_DELETE_REQUESTS);
        });
    }

    @MultitenantTransactional(readOnly = true)
    public List<FeatureDeletionRequest> findAllByIds(Iterable ids) {
        return this.featureDeletionRequestRepository.findAllById(ids);
    }

    @Override
    public void forceDeletion(Set<Long> ids) {
        featureDeletionRequestRepository.forceDeletionById(ids);
    }
}
