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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dao.FeatureUpdateRequestSpecification;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.IAbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureHistory;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.FeatureUpdateCollection;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.FeatureMetrics.FeatureUpdateState;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureUpdateJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;

/**
 * @author Marc SORDI
 */
@Service
@MultitenantTransactional
public class FeatureUpdateService extends AbstractFeatureService implements IFeatureUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUpdateService.class);

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IFeatureValidationService validationService;

    @Autowired
    private IFeatureUpdateRequestRepository updateRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IFeatureUpdateRequestRepository featureUpdateRequestRepo;

    @Autowired
    private IFeatureDeletionRequestRepository featureDeletionRepo;

    @Autowired
    private FeatureMetrics metrics;

    @Autowired
    private Validator validator;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureUpdateRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();

        List<FeatureUpdateRequest> grantedRequests = new ArrayList<>();
        RequestInfo<FeatureUniformResourceName> requestInfo = new RequestInfo<>();
        Set<String> existingRequestIds = this.featureUpdateRequestRepo.findRequestId();

        events.forEach(item -> prepareFeatureUpdateRequest(item, grantedRequests, requestInfo, existingRequestIds));

        // Batch save
        updateRepo.saveAll(grantedRequests);

        LOGGER.trace("------------->>> {} update requests registered in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        return requestInfo;
    }

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(FeatureUpdateCollection toHandle) {
        // Build events to reuse event registration code
        List<FeatureUpdateRequestEvent> toTreat = new ArrayList<>();
        for (Feature feature : toHandle.getFeatures()) {
            toTreat.add(FeatureUpdateRequestEvent.build(toHandle.getRequestOwner(), toHandle.getMetadata(), feature,
                                                        OffsetDateTime.now().minusSeconds(1)));
        }
        return registerRequests(toTreat);
    }

    /**
     * Validate, save and publish a new request
     */
    private void prepareFeatureUpdateRequest(FeatureUpdateRequestEvent item, List<FeatureUpdateRequest> grantedRequests,
            RequestInfo<FeatureUniformResourceName> requestInfo, Set<String> existingRequestIds) {

        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId())
                || grantedRequests.stream().anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        // Validate feature according to the data model
        errors.addAllErrors(validationService.validate(item.getFeature(), ValidationMode.PATCH));

        if (errors.hasErrors()) {
            // Monitoring log
            FeatureLogger.updateDenied(item.getRequestOwner(), item.getRequestId(),
                                       item.getFeature() != null ? item.getFeature().getId() : null,
                                       item.getFeature() != null ? item.getFeature().getUrn() : null,
                                       ErrorTranslator.getErrors(errors));
            // Publish DENIED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH, item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        item.getFeature() != null ? item.getFeature().getId() : null,
                                                        item.getFeature() != null ? item.getFeature().getUrn() : null,
                                                        RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            if (item.getFeature() == null) {
                requestInfo.getMessages()
                        .add(String.format("Request {} without feature has been rejected", item.getRequestId()));
            } else {
                requestInfo.addDeniedRequest(item.getFeature().getUrn(), ErrorTranslator.getErrors(errors));
            }
            metrics.count(item.getFeature() != null ? item.getFeature().getId() : null, null,
                          FeatureUpdateState.UPDATE_REQUEST_DENIED);
            return;
        }

        // Manage granted request
        FeatureUpdateRequest request = FeatureUpdateRequest
                .build(item.getRequestId(), item.getRequestOwner(), item.getRequestDate(), RequestState.GRANTED, null,
                       item.getFeature(), item.getMetadata().getPriority(), FeatureRequestStep.LOCAL_DELAYED);

        // Monitoring log
        FeatureLogger.updateGranted(request.getRequestOwner(), request.getRequestId(), request.getProviderId(),
                                    request.getUrn());
        // Publish GRANTED request
        publisher.publish(FeatureRequestEvent
                .build(FeatureRequestType.PATCH, item.getRequestId(), item.getRequestOwner(),
                       item.getFeature() != null ? item.getFeature().getId() : null, null, RequestState.GRANTED, null));
        // Add to granted request collection
        metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.UPDATE_REQUEST_GRANTED);
        grantedRequests.add(request);
        requestInfo.addGrantedRequest(request.getUrn(), request.getRequestId());
    }

    @Override
    public int scheduleRequests() {

        long scheduleStart = System.currentTimeMillis();
        List<ILightFeatureUpdateRequest> requestsToSchedule = this.featureUpdateRequestRepo
                .findRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(),
                                        PageRequest.of(0, this.properties.getMaxBulkSize()),
                                        OffsetDateTime.now().minusSeconds(this.properties.getDelayBeforeProcessing()))
                .getContent();

        if (!requestsToSchedule.isEmpty()) {

            requestsToSchedule = filterUrnInDeletion(requestsToSchedule);
            if (!requestsToSchedule.isEmpty()) {

                // Compute request ids
                Set<Long> requestIds = new HashSet<>();
                requestsToSchedule.forEach(r -> {
                    requestIds.add(r.getId());
                    metrics.count(r.getProviderId(), r.getUrn(), FeatureUpdateState.UPDATE_REQUEST_SCHEDULED);
                });

                // Switch to next step
                featureUpdateRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

                // Schedule job
                Set<JobParameter> jobParameters = Sets.newHashSet();
                jobParameters.add(new JobParameter(FeatureUpdateJob.IDS_PARAMETER, requestIds));

                // the job priority will be set according the priority of the first request to schedule
                JobInfo jobInfo = new JobInfo(false, requestsToSchedule.get(0).getPriority().getPriorityLevel(),
                        jobParameters, authResolver.getUser(), FeatureUpdateJob.class.getName());
                jobInfoService.createAsQueued(jobInfo);

                LOGGER.trace("------------->>> {} update requests scheduled in {} ms", requestsToSchedule.size(),
                             System.currentTimeMillis() - scheduleStart);
                return requestIds.size();
            }
        }
        return 0;
    }

    /**
     * From a list of {@link ILightFeatureUpdateRequest} to schedule remove those it have their urn
     * in a {@link FeatureDeletionRequest} at the step REMOTE_STORAGE_DELETION_REQUESTED
     * For those {@link ILightFeatureUpdateRequest} We will set their status to error and save them
     * @param requestsToSchedule list to filter
     * @return filtered list
     */
    private List<ILightFeatureUpdateRequest> filterUrnInDeletion(List<ILightFeatureUpdateRequest> requestsToSchedule) {
        // request from db are stored into an unmodifiable collection so we need to create a new list to remove errors
        List<ILightFeatureUpdateRequest> toSchedule = new ArrayList<>(requestsToSchedule);
        Set<FeatureUniformResourceName> deletionUrnScheduled = this.featureDeletionRepo
                .findByStep(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED, OffsetDateTime.now()).stream()
                .map(FeatureDeletionRequest::getUrn).collect(Collectors.toSet());
        Set<ILightFeatureUpdateRequest> errors = requestsToSchedule.stream()
                .filter(request -> deletionUrnScheduled.contains(request.getUrn())).collect(Collectors.toSet());
        Set<Long> errorIds = errors.stream().map(IAbstractFeatureRequest::getId).collect(Collectors.toSet());
        if (!errorIds.isEmpty()) {
            this.featureUpdateRequestRepo.updateState(RequestState.ERROR, errorIds);
        }

        toSchedule.removeAll(errors);
        return toSchedule;
    }

    @Override
    public Set<FeatureEntity> processRequests(List<FeatureUpdateRequest> requests, FeatureUpdateJob featureUpdateJob) {

        long processStart = System.currentTimeMillis();
        Set<FeatureEntity> entities = new HashSet<>();
        Set<FeatureUpdateRequest> successfulRequest = new HashSet<>();
        List<FeatureUpdateRequest> errorRequests = new ArrayList<>();

        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = this.featureRepo
                .findByUrnIn(requests.stream().map(FeatureUpdateRequest::getUrn).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(FeatureEntity::getUrn, Function.identity()));
        // Update feature
        for (FeatureUpdateRequest request : requests) {

            Feature patch = request.getFeature();

            // Retrieve feature from db
            // Note : entity is attached to transaction manager so all changes will be reflected in the db!
            FeatureEntity entity = featureByUrn.get(patch.getUrn());

            if (entity == null) {

                // Unknown URN : request error
                request.setState(RequestState.ERROR);
                request.setStep(FeatureRequestStep.LOCAL_ERROR);
                request.addError(String.format("No feature referenced in database with following URN : %s",
                                               request.getUrn()));
                errorRequests.add(request);

                // Monitoring log
                FeatureLogger.updateError(request.getRequestOwner(), request.getRequestId(), request.getProviderId(),
                                          request.getUrn(), request.getErrors());
                // Publish request failure
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH, request.getRequestId(),
                                                            request.getRequestOwner(), request.getProviderId(),
                                                            request.getUrn(), request.getState(), request.getErrors()));

                metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.UPDATE_REQUEST_ERROR);
            } else {

                entity.setLastUpdate(OffsetDateTime.now());
                if (entity.getFeature().getHistory() != null) {
                    entity.getFeature().getHistory().setUpdatedBy(request.getRequestOwner());
                } else {
                    entity.getFeature()
                            .setHistory(FeatureHistory.build(request.getRequestOwner(), request.getRequestOwner()));
                }

                // Merge properties handling null property values to unset properties
                IProperty.mergeProperties(entity.getFeature().getProperties(), patch.getProperties(),
                                          patch.getUrn().toString(), request.getRequestOwner());

                // Geometry cannot be unset but can be mutated
                if (!GeoJsonType.UNLOCATED.equals(patch.getGeometry().getType())) {
                    entity.getFeature().setGeometry(patch.getGeometry());
                }

                // Monitoring log
                FeatureLogger.updateSuccess(request.getRequestOwner(), request.getRequestId(), request.getProviderId(),
                                            request.getFeature().getUrn());
                // Publish request success
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH, request.getRequestId(),
                                                            request.getRequestOwner(), entity.getProviderId(),
                                                            entity.getUrn(), RequestState.SUCCESS));

                // Register
                metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.FEATURE_MERGED);
                entities.add(entity);
                successfulRequest.add(request);
            }
            featureUpdateJob.advanceCompletion();
        }

        featureRepo.saveAll(entities);
        featureUpdateRequestRepo.saveAll(errorRequests);

        // if notifications are required
        if (notificationSettingsService.retrieve().isActiveNotification()) {
            featureUpdateRequestRepo.updateStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, successfulRequest.stream()
                    .map(AbstractFeatureRequest::getId).collect(Collectors.toSet()));
        } else {
            featureUpdateRequestRepo.deleteInBatch(successfulRequest);
        }

        LOGGER.trace("------------->>> {} update requests processed with {} entities updated in {} ms", requests.size(),
                     entities.size(), System.currentTimeMillis() - processStart);

        return entities;
    }

    @Override
    public FeatureRequestType getRequestType() {
        return FeatureRequestType.PATCH;
    }

    @Override
    protected void logRequestDenied(String requestOwner, String requestId, Set<String> errors) {
        FeatureLogger.updateDenied(requestOwner, requestId, null, null, errors);
    }

    @Override
    public Page<FeatureUpdateRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page) {
        return updateRepo.findAll(FeatureUpdateRequestSpecification.searchAllByFilters(selection, page), page);
    }

    @Override
    public RequestsInfo getInfo(FeatureRequestsSelectionDTO selection) {
        if ((selection.getFilters() != null) && ((selection.getFilters().getState() != null)
                && (selection.getFilters().getState() != RequestState.ERROR))) {
            return RequestsInfo.build(0L);
        } else {
            selection.getFilters().withState(RequestState.ERROR);
            return RequestsInfo.build(updateRepo
                    .count(FeatureUpdateRequestSpecification.searchAllByFilters(selection, PageRequest.of(0, 1))));
        }
    }

    @Override
    public void deleteRequests(FeatureRequestsSelectionDTO selection) {
        Pageable page = PageRequest.of(0, 500);
        Page<FeatureUpdateRequest> requestsPage;
        boolean stop = false;
        do {
            requestsPage = findRequests(selection, page);
            updateRepo.deleteAll(requestsPage.filter(r -> r.isDeletable()));
            if ((requestsPage.getNumber() < MAX_PAGE_TO_DELETE) && requestsPage.hasNext()) {
                page = requestsPage.nextPageable();
            } else {
                stop = true;
            }
        } while (!stop);
    }

}
