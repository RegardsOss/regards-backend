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
package fr.cnes.regards.modules.feature.service;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
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
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureDeletionCollection;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureDeletionJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

/**
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureDeletionService extends AbstractFeatureService implements IFeatureDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDeletionService.class);

    private static final String ONLINE_CONF = "ONLINE_CONF";

    @Autowired
    private IFeatureDeletionRequestRepository deletionRepo;

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
    private Gson gson;


    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureDeletionRequestEvent> events) {
        long registrationStart = System.currentTimeMillis();

        List<FeatureDeletionRequest> grantedRequests = new ArrayList<>();
        RequestInfo<FeatureUniformResourceName> requestInfo = new RequestInfo<>();
        // FIXME changer ce fonctionnement!
        Set<String> existingRequestIds = this.deletionRepo.findRequestId();

        events.forEach(item -> prepareFeatureDeletionRequest(item, grantedRequests, requestInfo, existingRequestIds));
        LOGGER.trace("------------->>> {} deletion requests prepared in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        deletionRepo.saveAll(grantedRequests);
        LOGGER.debug("------------->>> {} deletion requests registered in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);
        return requestInfo;
    }

    private void prepareFeatureDeletionRequest(FeatureDeletionRequestEvent item,
            List<FeatureDeletionRequest> grantedRequests, RequestInfo<FeatureUniformResourceName> requestInfo,
            Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId()) || grantedRequests.stream()
                .anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            LOGGER.debug("Error during founded FeatureDeletionRequest validation {}", errors.toString());
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
            return;
        }

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
    }

    @Override
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();

        // Shedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<Long> requestIds = new HashSet<>();
        List<FeatureDeletionRequest> requestsToSchedule = new ArrayList<>();

        Page<FeatureDeletionRequest> dbRequests = this.deletionRepo.findByStep(FeatureRequestStep.LOCAL_DELAYED,
                                                                               OffsetDateTime.now(),
                                                                               PageRequest.of(0,
                                                                                              properties
                                                                                                      .getMaxBulkSize(),
                                                                                              Sort.by(Order.asc(
                                                                                                      "priority"),
                                                                                                      Order.asc(
                                                                                                              "requestDate"))));

        if (!dbRequests.isEmpty()) {
            for (FeatureDeletionRequest request : dbRequests.getContent()) {
                requestsToSchedule.add(request);
                requestIds.add(request.getId());
            }
            deletionRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureDeletionJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false,
                                          requestsToSchedule.get(0).getPriority().getPriorityLevel(),
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

    @Override
    public void processRequests(List<FeatureDeletionRequest> requests) {

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
        boolean isToNotify = notificationSettingsService.retrieve().isActiveNotification();
        manageRequestsAlreadyDeleted(requestsAlreadyDeleted, isToNotify);
        manageRequestsWithFiles(requestsWithFiles);
        manageRequestsWithoutFile(requestsWithoutFiles, isToNotify);
    }

    private void manageRequestsAlreadyDeleted(Set<FeatureDeletionRequest> requestsAlreadyDeleted, boolean isToNotify) {
        if (!requestsAlreadyDeleted.isEmpty()) {
            // PROPAGATE to NOTIFIER if required
            if(isToNotify) {
                String unknown = "unknown";
                for (FeatureDeletionRequest fdr : requestsAlreadyDeleted) {
                    // Build fake incomplete feature
                    Feature fakeFeature = Feature
                            .build(unknown, unknown, fdr.getUrn(), IGeometry.unlocated(), EntityType.DATA, unknown);
                    fdr.setToNotify(fakeFeature);
                    fdr.setAlreadyDeleted(true);
                    fdr.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                }
                deletionRepo.saveAll(requestsAlreadyDeleted);
            } else {
                this.deletionRepo.deleteInBatch(requestsAlreadyDeleted);
            }

            // PROPAGATE to CATALOG
            requestsAlreadyDeleted
                    .forEach(r -> publisher.publish(FeatureEvent.buildFeatureDeleted(r.getUrn().toString())));

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
    }

    private void manageRequestsWithFiles(Map<FeatureDeletionRequest, FeatureEntity> requestsWithFiles) {
        // Request file deletion
        for (Entry<FeatureDeletionRequest, FeatureEntity> entry : requestsWithFiles.entrySet()) {
            publishFiles(entry.getKey(), entry.getValue());
        }
        // Save all request with files waiting for file deletion
        this.deletionRepo.saveAll(requestsWithFiles.keySet());
        // No feedback at the moment
    }

    private void manageRequestsWithoutFile(Map<FeatureDeletionRequest, FeatureEntity> requestsWithoutFiles, boolean isToNotify) {
        sendFeedbacksAndClean(requestsWithoutFiles, isToNotify);
    }

    private void sendFeedbacksAndClean(Map<FeatureDeletionRequest, FeatureEntity> sucessfullRequests, boolean isToNotify) {
        // PREPARE PROPAGATION to NOTIFIER if required
        if(isToNotify) {
            for (Map.Entry<FeatureDeletionRequest, FeatureEntity> entry : sucessfullRequests.entrySet()) {
                entry.getKey().setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                entry.getKey().setAlreadyDeleted(false);
                entry.getKey().setToNotify(entry.getValue().getFeature());
            }
            deletionRepo.saveAll(sucessfullRequests.keySet());
        } else {
            this.deletionRepo.deleteInBatch(sucessfullRequests.keySet());
        }

        // PROPAGATE to CATALOG
        sucessfullRequests.values()
                .forEach(f -> publisher.publish(FeatureEvent.buildFeatureDeleted(f.getUrn().toString())));

        // Feedbacks for deleted features
        Map<FeatureUniformResourceName, FeatureDeletionRequest> requestByUrn = sucessfullRequests.keySet().stream()
                .collect(Collectors.toMap(FeatureDeletionRequest::getUrn, Function.identity()));
        for (FeatureEntity entity : sucessfullRequests.values()) {
            FeatureDeletionRequest fdr = requestByUrn.get(entity.getUrn());
            // Monitoring log
            FeatureLogger.deletionSuccess(fdr.getRequestOwner(), fdr.getRequestId(), fdr.getUrn());
            // Publish successful request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                        fdr.getRequestId(),
                                                        fdr.getRequestOwner(),
                                                        entity.getProviderId(),
                                                        fdr.getUrn(),
                                                        RequestState.SUCCESS));
        }
        // Delete all features without files, related requests will be deleted once we know notifier has successfully sent the notification about it
        this.featureRepo.deleteAll(sucessfullRequests.values());
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
     * @param fdr
     * @return
     */
    private FeatureDeletionRequest publishFiles(FeatureDeletionRequest fdr, FeatureEntity feature) {
        fdr.setStep(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED);
        for (FeatureFile file : feature.getFeature().getFiles()) {
            FeatureFileAttributes attribute = file.getAttributes();
            fdr.setGroupId(this.storageClient.delete(FileDeletionRequestDTO.build(attribute.getChecksum(),
                                                                                  ONLINE_CONF,
                                                                                  feature.getFeature().getUrn()
                                                                                          .toString(),
                                                                                  false)).getGroupId());
        }
        return fdr;
    }

    @Override
    public void processStorageRequests(Set<String> groupIds) {
        Set<FeatureDeletionRequest> requests = this.deletionRepo.findByGroupIdIn(groupIds);

        // Retrieve features
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureRepo
                .findByUrnIn(requests.stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(FeatureEntity::getUrn, Function.identity()));

        Map<FeatureDeletionRequest, FeatureEntity> sucessfullRequests = new HashMap<>();
        for (FeatureDeletionRequest fdr : requests) {
            sucessfullRequests.put(fdr, featureByUrn.get(fdr.getUrn()));
        }
        sendFeedbacksAndClean(sucessfullRequests, notificationSettingsService.retrieve().isActiveNotification());
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
}
