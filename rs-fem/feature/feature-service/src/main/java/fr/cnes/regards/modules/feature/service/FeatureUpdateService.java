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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.AbstractFeatureEntity;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.DisseminationAckEvent;
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
import fr.cnes.regards.modules.feature.service.request.FeatureUpdateDisseminationService;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FeatureUpdateService extends AbstractFeatureService<FeatureUpdateRequest>
    implements IFeatureUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUpdateService.class);

    @Autowired
    private IFeatureValidationService validationService;

    @Autowired
    private IFeatureUpdateRequestRepository featureUpdateRequestRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IFeatureDeletionRequestRepository featureDeletionRequestRepository;

    @Autowired
    private IFeatureDisseminationInfoRepository featureDisseminationInfoRepository;

    @Autowired
    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    @Autowired
    private FeatureMetrics metrics;

    @Autowired
    private Validator validator;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private FeatureSessionNotifier featureSessionNotifier;

    @Autowired
    private FeatureFilesService featureFilesService;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository;

    @Autowired
    private ISessionAgentClient sessionNotificationClient;

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureUpdateRequestEvent> featureUpdateRequestEvts) {
        long registrationStart = System.currentTimeMillis();

        List<FeatureUpdateRequest> grantedRequests = new ArrayList<>();
        RequestInfo<FeatureUniformResourceName> requestInfo = new RequestInfo<>();

        Set<String> existingRequestIds = featureUpdateRequestRepository.findRequestId();

        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(
            featureUpdateRequestEvts.stream().map(event -> event.getFeature().getUrn()).collect(Collectors.toSet()));

        featureUpdateRequestEvts.forEach(event -> prepareFeatureUpdateRequest(event,
                                                                              sessionInfoByUrn.get(event.getFeature()
                                                                                                        .getUrn()),
                                                                              grantedRequests,
                                                                              requestInfo,
                                                                              existingRequestIds));

        // Batch save in database
        featureUpdateRequestRepository.saveAll(grantedRequests);

        LOGGER.trace("------------->>> {} update requests registered in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        return requestInfo;
    }

    @Override
    public RequestInfo<FeatureUniformResourceName> registerRequests(FeatureUpdateCollection toHandle) {
        // Build events to reuse event registration code
        List<FeatureUpdateRequestEvent> toTreat = new ArrayList<>();
        for (Feature feature : toHandle.getFeatures()) {
            toTreat.add(FeatureUpdateRequestEvent.build(toHandle.getRequestOwner(),
                                                        toHandle.getMetadata(),
                                                        feature,
                                                        OffsetDateTime.now().minusSeconds(1)));
        }
        return registerRequests(toTreat);
    }

    /**
     * Validate, save and publish a new request
     */
    private void prepareFeatureUpdateRequest(FeatureUpdateRequestEvent featureUpdateRequestEvt,
                                             ILightFeatureEntity sessionInfo,
                                             List<FeatureUpdateRequest> grantedRequests,
                                             RequestInfo<FeatureUniformResourceName> requestInfo,
                                             Set<String> existingRequestIds) {

        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        String featureId = featureUpdateRequestEvt.getFeature() != null ?
            featureUpdateRequestEvt.getFeature().getId() :
            null;
        FeatureUniformResourceName urn = featureUpdateRequestEvt.getFeature() != null ?
            featureUpdateRequestEvt.getFeature().getUrn() :
            null;
        validator.validate(featureUpdateRequestEvt, errors);
        validateRequest(featureUpdateRequestEvt, errors);

        if (existingRequestIds.contains(featureUpdateRequestEvt.getRequestId()) || grantedRequests.stream()
                                                                                                  .anyMatch(request -> request.getRequestId()
                                                                                                                              .equals(
                                                                                                                                  featureUpdateRequestEvt.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        // Validate feature according to the data model
        errors.addAllErrors(validationService.validate(featureUpdateRequestEvt.getFeature(), ValidationMode.PATCH));

        if (errors.hasErrors()) {
            denyRequest(featureUpdateRequestEvt, requestInfo, sessionInfo, featureId, urn, errors);
        } else {
            // Manage granted request
            FeatureUpdateRequest request = createFeatureUpdateRequest(featureUpdateRequestEvt);
            
            // Handle optional file mode
            request.setFileUpdateMode(FeatureFileUpdateMode.parse(featureUpdateRequestEvt.getFileUpdateMode()));

            // Monitoring log
            FeatureLogger.updateGranted(request.getRequestOwner(),
                                        request.getRequestId(),
                                        request.getProviderId(),
                                        request.getUrn());
            // Publish GRANTED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH,
                                                        featureUpdateRequestEvt.getRequestId(),
                                                        featureUpdateRequestEvt.getRequestOwner(),
                                                        featureId,
                                                        urn,
                                                        RequestState.GRANTED,
                                                        null));
            // Add to granted request collection
            metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.UPDATE_REQUEST_GRANTED);
            grantedRequests.add(request);
            requestInfo.addGrantedRequest(request.getUrn(), request.getRequestId());
            // Update session properties
            featureSessionNotifier.incrementCount(sessionInfo, FeatureSessionProperty.UPDATE_REQUESTS);
        }
    }

    /**
     * Create entity of feature update request from its event with :
     * <ul>
     *     <li>state: GRANTED</li>
     *     <li>step: LOCAL_DELAYED</li>
     * </ul>
     */
    private FeatureUpdateRequest createFeatureUpdateRequest(FeatureUpdateRequestEvent featureUpdateRequestEvt) {
        return FeatureUpdateRequest.buildGranted(featureUpdateRequestEvt.getRequestId(),
                                                 featureUpdateRequestEvt.getRequestOwner(),
                                                 featureUpdateRequestEvt.getRequestDate(),
                                                 featureUpdateRequestEvt.getFeature(),
                                                 featureUpdateRequestEvt.getMetadata().getPriority(),
                                                 featureUpdateRequestEvt.getMetadata().getStorages(),
                                                 FeatureRequestStep.LOCAL_DELAYED,
                                                 featureUpdateRequestEvt.getMetadata().getAcknowledgedRecipient());
    }

    private void denyRequest(FeatureUpdateRequestEvent request,
                             RequestInfo<FeatureUniformResourceName> requestInfo,
                             ILightFeatureEntity sessionInfo,
                             String featureId,
                             FeatureUniformResourceName urn,
                             Errors errors) {
        // Monitoring log
        FeatureLogger.updateDenied(request.getRequestOwner(),
                                   request.getRequestId(),
                                   featureId,
                                   urn,
                                   ErrorTranslator.getErrors(errors));
        // Publish DENIED request
        publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH,
                                                    request.getRequestId(),
                                                    request.getRequestOwner(),
                                                    featureId,
                                                    urn,
                                                    RequestState.DENIED,
                                                    ErrorTranslator.getErrors(errors)));
        if (request.getFeature() == null) {
            requestInfo.getMessages()
                       .add(String.format("Request %s without feature has been rejected", request.getRequestId()));
        } else {
            requestInfo.addDeniedRequest(request.getFeature().getUrn(), ErrorTranslator.getErrors(errors));
        }
        metrics.count(featureId, null, FeatureUpdateState.UPDATE_REQUEST_DENIED);
        // Update session properties
        featureSessionNotifier.incrementCount(sessionInfo, FeatureSessionProperty.DENIED_UPDATE_REQUESTS);
    }

    @Override
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();
        List<ILightFeatureUpdateRequest> requestsToSchedule = this.featureUpdateRequestRepository.findRequestsToSchedule(
            this.properties.getDelayBeforeProcessing(),
            this.properties.getMaxBulkSize());

        if (!requestsToSchedule.isEmpty()) {

            Optional<PriorityLevel> highestPriorityLevel = requestsToSchedule.stream()
                                                                             .max((p1, p2) -> Math.max(p1.getPriority()
                                                                                                         .getPriorityLevel(),
                                                                                                       p2.getPriority()
                                                                                                         .getPriorityLevel()))
                                                                             .map(IAbstractRequest::getPriority);

            requestsToSchedule = filterUrnInDeletion(requestsToSchedule);
            if (!requestsToSchedule.isEmpty()) {

                Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(
                    requestsToSchedule.stream().map(ILightFeatureUpdateRequest::getUrn).collect(Collectors.toSet()));

                // Compute request ids
                Set<Long> requestIds = new HashSet<>();
                requestsToSchedule.forEach(r -> {
                    requestIds.add(r.getId());
                    metrics.count(r.getProviderId(), r.getUrn(), FeatureUpdateState.UPDATE_REQUEST_SCHEDULED);
                    // Update session properties
                    featureSessionNotifier.incrementCount(sessionInfoByUrn.get(r.getUrn()),
                                                          FeatureSessionProperty.RUNNING_UPDATE_REQUESTS);
                });

                // Switch to next step
                featureUpdateRequestRepository.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

                // Schedule job
                Set<JobParameter> jobParameters = Sets.newHashSet();
                jobParameters.add(new JobParameter(FeatureUpdateJob.IDS_PARAMETER, requestIds));

                // the job priority will be set according the priority of the first request to schedule
                JobInfo jobInfo = new JobInfo(false,
                                              highestPriorityLevel.orElse(PriorityLevel.NORMAL).getPriorityLevel(),
                                              jobParameters,
                                              authResolver.getUser(),
                                              FeatureUpdateJob.class.getName());
                jobInfoService.createAsQueued(jobInfo);

                LOGGER.trace("------------->>> {} update requests scheduled in {} ms",
                             requestsToSchedule.size(),
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
     *
     * @param requestsToSchedule list to filter
     * @return filtered list
     */
    private List<ILightFeatureUpdateRequest> filterUrnInDeletion(List<ILightFeatureUpdateRequest> requestsToSchedule) {
        // request from db are stored into an unmodifiable collection so we need to create a new list to remove errors
        List<ILightFeatureUpdateRequest> toSchedule = new ArrayList<>(requestsToSchedule);
        List<FeatureRequestStep> deletionSteps = Lists.newArrayList();
        deletionSteps.add(FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED);
        deletionSteps.add(FeatureRequestStep.LOCAL_SCHEDULED);
        Set<FeatureUniformResourceName> deletionUrnScheduled = this.featureDeletionRequestRepository.findByStepIn(
            deletionSteps,
            OffsetDateTime.now()).stream().map(FeatureDeletionRequest::getUrn).collect(Collectors.toSet());
        Set<ILightFeatureUpdateRequest> errors = requestsToSchedule.stream()
                                                                   .filter(request -> deletionUrnScheduled.contains(
                                                                       request.getUrn()))
                                                                   .collect(Collectors.toSet());
        Set<Long> errorIds = errors.stream().map(IAbstractFeatureRequest::getId).collect(Collectors.toSet());
        if (!errorIds.isEmpty()) {
            errors.forEach(r -> LOGGER.error(
                "Update request {} on {} not scheduled cause a deletion request is processing on the same feature",
                r.getId(),
                r.getUrn()));
            this.featureUpdateRequestRepository.updateStateAndStep(RequestState.ERROR,
                                                                   FeatureRequestStep.LOCAL_ERROR,
                                                                   errorIds);
        }
        toSchedule.removeAll(errors);
        return toSchedule;
    }

    @Override
    public Set<FeatureEntity> processRequests(List<FeatureUpdateRequest> requests, FeatureUpdateJob featureUpdateJob) {
        long processStart = System.currentTimeMillis();

        Set<FeatureEntity> featureEntities = new HashSet<>();
        Set<FeatureUpdateRequest> successfulRequest = new HashSet<>();
        Set<FeatureUpdateRequest> storagePendingRequests = new HashSet<>();
        List<FeatureUpdateRequest> errorRequests = new ArrayList<>();
        List<DisseminationAckEvent> disseminationAckEvents = new ArrayList<>();

        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = featureEntityRepository.findCompleteByUrnIn(
                                                                                                 requests.stream().map(FeatureUpdateRequest::getUrn).collect(Collectors.toList()))
                                                                                             .stream()
                                                                                             .collect(Collectors.toMap(
                                                                                                 FeatureEntity::getUrn,
                                                                                                 Function.identity()));
        Map<FeatureEntity, Set<FeatureDisseminationInfo>> disseminationInfosByFeature = computeDisseminationInfoIfNeeded(
            featureByUrn,
            requests);
        // Update feature update request
        for (FeatureUpdateRequest request : requests) {
            Feature patch = request.getFeature();
            // Retrieve feature from db
            // Note : entity is attached to transaction manager so all changes will be reflected in the db!
            FeatureEntity featureEntity = featureByUrn.get(patch.getUrn());
            // Check a feature exists related to this request
            if (featureEntity == null) {
                request.addError(FeatureRequestStep.LOCAL_ERROR,
                                 String.format("No feature referenced in database with following URN = %s ProviderId"
                                               + " = %s", request.getUrn(), request.getProviderId()));
                errorRequests.add(request);
                // Monitoring log
                FeatureLogger.updateError(request.getRequestOwner(),
                                          request.getRequestId(),
                                          request.getProviderId(),
                                          request.getUrn(),
                                          request.getErrors());
                // Register
                metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.UPDATE_REQUEST_ERROR);
            } else {
                featureEntity.setLastUpdate(OffsetDateTime.now());
                if (featureEntity.getFeature().getHistory() != null) {
                    featureEntity.getFeature().getHistory().setUpdatedBy(request.getRequestOwner());
                } else {
                    featureEntity.getFeature()
                                 .setHistory(FeatureHistory.build(request.getRequestOwner(),
                                                                  request.getRequestOwner()));
                }

                // Merge properties handling null property values to unset properties
                IProperty.mergeProperties(featureEntity.getFeature().getProperties(),
                                          patch.getProperties(),
                                          patch.getUrn().toString(),
                                          request.getRequestOwner());
                // Geometry cannot be unset but can be mutated
                if (!GeoJsonType.UNLOCATED.equals(patch.getGeometry().getType())) {
                    featureEntity.getFeature().setGeometry(patch.getGeometry());
                }
                // Monitoring log
                FeatureLogger.updateSuccess(request.getRequestOwner(),
                                            request.getRequestId(),
                                            request.getProviderId(),
                                            request.getFeature().getUrn());

                // Update source/session for request notification
                request.setSourceToNotify(featureEntity.getSessionOwner());
                request.setSessionToNotify(featureEntity.getSession());

                // Check files update
                try {
                    featureFilesService.handleFeatureUpdateFiles(request, featureEntity);
                    if (request.getStep() != FeatureRequestStep.REMOTE_STORAGE_REQUESTED) {
                        // Publish request success
                        publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH,
                                                                    request.getRequestId(),
                                                                    request.getRequestOwner(),
                                                                    featureEntity.getProviderId(),
                                                                    featureEntity.getUrn(),
                                                                    RequestState.SUCCESS));

                        // add entity to request (toNotify)
                        request.setToNotify(featureEntity.getFeature());
                        successfulRequest.add(request);
                    } else {
                        storagePendingRequests.add(request);
                    }
                    // Manage acknowledges
                    Set<FeatureDisseminationInfo> disseminationInfos = disseminationInfosByFeature.getOrDefault(
                        featureEntity,
                        Sets.newHashSet());
                    // Add ack event to list of ack events to create if needed
                    handleAcknowledgedRecipient(request, featureEntity, disseminationInfos).ifPresent(
                        disseminationAckEvents::add);
                    // Register
                    metrics.count(request.getProviderId(), request.getUrn(), FeatureUpdateState.FEATURE_MERGED);
                    // Add updated feature
                    featureEntities.add(featureEntity);
                } catch (ModuleException e) {
                    LOGGER.error(e.getMessage(), e);
                    request.addError(FeatureRequestStep.LOCAL_ERROR, e.getMessage());
                    errorRequests.add(request);
                }
            }
            if (featureUpdateJob != null) {
                featureUpdateJob.advanceCompletion();
            }
        }

        featureUpdateDisseminationService.saveAckRequests(disseminationAckEvents);
        featureEntityRepository.saveAll(featureEntities);
        featureUpdateRequestRepository.saveAll(errorRequests);
        featureUpdateRequestRepository.saveAll(storagePendingRequests);

        doOnError(errorRequests);
        doOnSuccess(successfulRequest);

        LOGGER.trace("------------->>> {} update requests processed with {} entities updated in {}ms",
                     requests.size(),
                     featureEntities.size(),
                     System.currentTimeMillis() - processStart);
        return featureEntities;
    }

    private Map<FeatureEntity, Set<FeatureDisseminationInfo>> computeDisseminationInfoIfNeeded(Map<FeatureUniformResourceName, FeatureEntity> featureByUrn,
                                                                                               List<FeatureUpdateRequest> requests) {
        List<FeatureEntity> requestNeedingDisseminationInfo = requests.stream()
                                                                      .filter(request -> StringUtils.isNotBlank(request.getAcknowledgedRecipient()))
                                                                      .map(request -> featureByUrn.get(request.getUrn()))
                                                                      .filter(Objects::nonNull)
                                                                      .toList();

        Map<Long, FeatureEntity> idToFeature = requestNeedingDisseminationInfo.stream()
                                                                              .collect(Collectors.toMap(
                                                                                  AbstractFeatureEntity::getId,
                                                                                  Function.identity()));
        if (!requestNeedingDisseminationInfo.isEmpty()) {
            Set<FeatureDisseminationInfo> featureDisseminationInfos = featureDisseminationInfoRepository.findByFeatureIdIn(
                requestNeedingDisseminationInfo.stream().map(AbstractFeatureEntity::getId).toList());
            return featureDisseminationInfos.stream()
                                            .collect(Collectors.groupingBy(d -> idToFeature.get(d.getFeatureId()),
                                                                           Collectors.toSet()));
        }
        return new HashMap<>();
    }

    private Optional<DisseminationAckEvent> handleAcknowledgedRecipient(FeatureUpdateRequest request,
                                                                        FeatureEntity featureEntity,
                                                                        Set<FeatureDisseminationInfo> disseminationInfoCache) {
        String acknowledgedRecipient = request.getAcknowledgedRecipient();
        if (!StringUtils.isBlank(acknowledgedRecipient)) {
            LOGGER.debug("acknowledged recipient: {}", acknowledgedRecipient);
            return Optional.of(new DisseminationAckEvent(featureEntity.getUrn().toString(), acknowledgedRecipient));
        }
        return Optional.empty();
    }

    @Override
    protected void postRequestDeleted(Collection<FeatureUpdateRequest> deletedRequests) {
        // Nothing to do
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
    @MultitenantTransactional(readOnly = true)
    public Page<FeatureUpdateRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page) {
        return featureUpdateRequestRepository.findAll(new FeatureUpdateRequestSpecificationBuilder().withParameters(
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
            return RequestsInfo.build(featureUpdateRequestRepository.count(new FeatureUpdateRequestSpecificationBuilder().withParameters(
                filters).build()));
        }
    }

    @Override
    public void handleStorageError(Collection<RequestResultInfoDto> errorRequests) {
        Map<String, String> errorByGroupId = Maps.newHashMap();
        errorRequests.forEach(e -> errorByGroupId.put(e.getGroupId(), e.getErrorCause()));

        Set<FeatureUpdateRequest> requests = featureUpdateRequestRepository.findByGroupIdIn(errorByGroupId.keySet());

        if (!requests.isEmpty()) {
            for (FeatureUpdateRequest request : requests) {
                String errorCause = Optional.ofNullable(errorByGroupId.get(request.getGroupId()))
                                            .orElse("unknown error.");
                String errorMessage = String.format("Error received from storage for request id %s and provider id %s. "
                                                    + "Cause : %s",
                                                    request.getRequestId(),
                                                    request.getProviderId(),
                                                    errorCause);
                LOGGER.error(errorMessage);
                addRemoteStorageError(request, errorCause);

                // publish error notification for all request id
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.PATCH,
                                                            request.getRequestId(),
                                                            request.getRequestOwner(),
                                                            request.getProviderId(),
                                                            request.getUrn(),
                                                            RequestState.ERROR,
                                                            Sets.newHashSet(errorMessage)));
            }
            doOnError(requests);
            featureUpdateRequestRepository.saveAll(requests);
        }
    }

    @Override
    protected IAbstractFeatureRequestRepository<FeatureUpdateRequest> getRequestsRepository() {
        return featureUpdateRequestRepository;
    }

    @Override
    protected FeatureUpdateRequest updateForRetry(FeatureUpdateRequest request) {
        // Nothing to do
        return request;
    }

    @Override
    protected void sessionInfoUpdateForRetry(Collection<FeatureUpdateRequest> requests) {
        // Retrieve LightEntity associated to each update request to retry
        Map<FeatureUniformResourceName, ILightFeatureEntity> entitiesByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                         .map(request -> request.getFeature()
                                                                                                                                .getUrn())
                                                                                                         .collect(
                                                                                                             Collectors.toSet()));
        // Groups requests by entity urn
        ArrayListMultimap<FeatureUniformResourceName, FeatureUpdateRequest> requestsByFeatureUrn = requests.stream()
                                                                                                           .collect(
                                                                                                               Multimaps.toMultimap(
                                                                                                                   FeatureUpdateRequest::getUrn,
                                                                                                                   f -> f,
                                                                                                                   ArrayListMultimap::create));
        // For each request, check if request state is a retryable state, is so decrement session error count and
        // increment session update request running.
        requestsByFeatureUrn.forEach((urn, request) -> {
            if (request.getLastExecErrorStep().isRetryableErrorStep()) {
                ILightFeatureEntity requestEntity = entitiesByUrn.get(urn);
                featureSessionNotifier.decrementCount(requestEntity, FeatureSessionProperty.IN_ERROR_UPDATE_REQUESTS);
                featureSessionNotifier.incrementCount(requestEntity, FeatureSessionProperty.RUNNING_UPDATE_REQUESTS);
            }
        });
    }

    @Override
    protected void sessionInfoUpdateForDelete(Collection<FeatureUpdateRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                            .map(request -> request.getFeature()
                                                                                                                                   .getUrn())
                                                                                                            .collect(
                                                                                                                Collectors.toSet()));
        sessionInfoByUrn.forEach((urn, entity) -> {
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.IN_ERROR_UPDATE_REQUESTS);
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.UPDATE_REQUESTS);
        });
    }

    @Override
    public void doOnSuccess(Collection<FeatureUpdateRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                            .map(request -> request.getFeature()
                                                                                                                                   .getUrn())
                                                                                                            .collect(
                                                                                                                Collectors.toSet()));
        sessionInfoByUrn.forEach((urn, entity) -> {
            featureSessionNotifier.incrementCount(entity, FeatureSessionProperty.UPDATED_PRODUCTS);
        });

        // if notifications are required
        if (notificationSettingsService.isActiveNotification()) {
            requests.forEach(r -> r.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED));
            featureUpdateRequestRepository.saveAll(requests);
        } else {
            doOnTerminated(requests);
            featureUpdateRequestRepository.deleteAllInBatch(requests);
        }
    }

    private StepProperty getStepProperty(String source,
                                         String session,
                                         FeatureSessionProperty property,
                                         String recipientLabel,
                                         int nbProducts) {
        return new StepProperty("fem_dissemination",
                                source,
                                session,
                                new StepPropertyInfo(StepTypeEnum.DISSEMINATION,
                                                     property.getState(),
                                                     String.format(property.getName(), recipientLabel),
                                                     String.valueOf(nbProducts),
                                                     property.isInputRelated(),
                                                     property.isOutputRelated()));
    }

    @Override
    public void doOnTerminated(Collection<FeatureUpdateRequest> requests) {
        requests.forEach(request -> {
            // For each terminated update request, notify session for request ends.
            // To do so, retrieve session and source from request if present.
            // If not retrieve it from the associated feature.
            // If feature does not exist anymore, log error. (should never happen).
            if (request.getSourceToNotify() != null && request.getSessionToNotify() != null) {
                featureSessionNotifier.decrementCount(request.getSourceToNotify(),
                                                      request.getSessionToNotify(),
                                                      FeatureSessionProperty.RUNNING_UPDATE_REQUESTS);
            } else {
                List<ILightFeatureEntity> features = featureEntityRepository.findLightByUrnIn(List.of(request.getUrn()));
                if (!features.isEmpty()) {
                    featureSessionNotifier.decrementCount(features.get(0),
                                                          FeatureSessionProperty.RUNNING_UPDATE_REQUESTS);
                } else {
                    LOGGER.error(
                        "Unable to decrement update request count for update request not associated to an existing feature");
                }
            }
        });
    }

    @Override
    public void doOnError(Collection<FeatureUpdateRequest> requests) {
        if (requests != null && !requests.isEmpty()) {
            Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                                .map(
                                                                                                                    request -> request.getFeature()
                                                                                                                                      .getUrn())
                                                                                                                .collect(
                                                                                                                    Collectors.toSet()));
            sessionInfoByUrn.forEach((urn, entity) -> {
                featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.RUNNING_UPDATE_REQUESTS);
                featureSessionNotifier.incrementCount(entity, FeatureSessionProperty.IN_ERROR_UPDATE_REQUESTS);
            });
            // Publish request failure
            List<FeatureRequestEvent> errorsEvents = requests.stream()
                                                             .map(request -> FeatureRequestEvent.build(
                                                                 FeatureRequestType.PATCH,
                                                                 request.getRequestId(),
                                                                 request.getRequestOwner(),
                                                                 request.getProviderId(),
                                                                 request.getUrn(),
                                                                 request.getState(),
                                                                 request.getErrors()))
                                                             .collect(Collectors.toList());
            publisher.publish(errorsEvents);
        }
    }

    @Override
    @MultitenantTransactional(readOnly = true)
    public List<FeatureUpdateRequest> findAllByOrderIdsByRequestDateAsc(Set<Long> ids) {
        return featureUpdateRequestRepository.findAllByIdInOrderByRequestDateAsc(ids);
    }
}
