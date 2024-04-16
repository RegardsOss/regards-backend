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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.feature.dao.FeatureCreationRequestSpecificationsBuilder;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.FeatureMetrics.FeatureCreationState;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureCreationJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature service management
 *
 * @author Kevin Marchois
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FeatureCreationService extends AbstractFeatureService<FeatureCreationRequest>
    implements IFeatureCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCreationService.class);

    @Autowired
    private IFeatureUpdateService updateService;

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IFeatureValidationService validationService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private FeatureMetrics metrics;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private Validator validator;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private FeatureSessionNotifier featureSessionNotifier;

    @Autowired
    private FeatureFilesService featureFilesService;

    @Autowired
    private FeatureDeletionService featureDeletionService;

    @PersistenceContext
    private EntityManager em;

    @Override
    public RequestInfo<String> registerRequests(List<FeatureCreationRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();

        List<FeatureCreationRequest> grantedRequests = new ArrayList<>();
        List<FeatureUpdateRequestEvent> newUpdateRequests = new ArrayList<>();
        RequestInfo<String> requestInfo = new RequestInfo<>();

        // Only retrieve from database requestIds matching the events to check if requests already exists.
        Set<String> existingRequestIds = featureCreationRequestRepo.findRequestIdByRequestIdIn(events.stream()
                                                                                                     .map(
                                                                                                         FeatureCreationRequestEvent::getRequestId)
                                                                                                     .collect(Collectors.toList()));

        Set<FeatureUniformResourceName> eventsUrn = events.stream()
                                                          .map(event -> event.getFeature().getUrn())
                                                          .collect(Collectors.toSet());
        Set<FeatureUniformResourceName> existingRequestUrns = featureCreationRequestRepo.findUrnByUrnIn(eventsUrn);
        Set<FeatureUniformResourceName> existingEntityUrns = featureEntityRepository.findLightByUrnIn(eventsUrn)
                                                                                    .stream()
                                                                                    .map(ILightFeatureEntity::getUrn)
                                                                                    .collect(Collectors.toSet());

        events.forEach(item -> prepareFeatureCreationRequest(item,
                                                             grantedRequests,
                                                             requestInfo,
                                                             existingRequestIds,
                                                             existingRequestUrns,
                                                             existingEntityUrns,
                                                             newUpdateRequests));
        LOGGER.trace("------------->>> {} creation requests prepared in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureCreationRequest from a list of FeatureCreationRequestEvent
        featureCreationRequestRepo.saveAll(grantedRequests);
        LOGGER.trace("------------->>> {} creation requests registered in {} ms",
                     grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        if (!newUpdateRequests.isEmpty()) {
            RequestInfo<FeatureUniformResourceName> updateInfo = updateService.registerRequests(newUpdateRequests);
            updateInfo.getGranted()
                      .forEach((urn, requestId) -> requestInfo.addGrantedRequest(urn.toString(), requestId));
            updateInfo.getDenied().forEach((urn, requestId) -> requestInfo.addDeniedRequest(urn.toString(), requestId));
        }

        return requestInfo;
    }

    @Override
    public RequestInfo<String> registerRequests(FeatureCreationCollection collection) {
        // Build events to reuse event registration code
        List<FeatureCreationRequestEvent> toTreat = new ArrayList<>();
        for (Feature feature : collection.getFeatures()) {
            toTreat.add(FeatureCreationRequestEvent.build(collection.getRequestOwner(),
                                                          collection.getMetadata(),
                                                          feature));
        }
        return registerRequests(toTreat);
    }

    /**
     * Validate a list of {@link FeatureCreationRequestEvent}
     * and if validated create a list of {@link FeatureCreationRequest}
     *
     * @param item                request to manage
     * @param grantedRequests     collection of granted requests to populate
     * @param requestInfo         store request registration state
     * @param existingRequestIds  list of existing request ids in database (its a unique constraint)
     * @param existingEntityUrns  list of URNs from existing feature entities
     * @param existingRequestUrns list of URNs from existing feature creation requests
     * @param newUpdateRequests   list of update requests already prepared
     */
    private void prepareFeatureCreationRequest(FeatureCreationRequestEvent item,
                                               List<FeatureCreationRequest> grantedRequests,
                                               RequestInfo<String> requestInfo,
                                               Set<String> existingRequestIds,
                                               Set<FeatureUniformResourceName> existingRequestUrns,
                                               Set<FeatureUniformResourceName> existingEntityUrns,
                                               List<FeatureUpdateRequestEvent> newUpdateRequests) {

        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        String requestId = item.getRequestId();
        String requestOwner = item.getRequestOwner();
        Feature feature = item.getFeature();
        FeatureUniformResourceName urn = feature.getUrn();
        String featureId = feature.getId();
        FeatureCreationSessionMetadata sessionMetadata = item.getMetadata();
        String sessionOwner = sessionMetadata.getSessionOwner();
        String session = sessionMetadata.getSession();

        if (existingRequestIds.contains(requestId) || grantedRequests.stream()
                                                                     .anyMatch(request -> request.getRequestId()
                                                                                                 .equals(requestId))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        // Validate feature according to the data model
        errors.addAllErrors(validationService.validate(feature, ValidationMode.CREATION));

        // Validate provided URN
        if (urn != null) {
            String rejectField = "urn";
            String errorCode = "feature.request.urn.already.exists.error.message";
            String defaultMessageTemplate = "Creation request with this URN already exists in %s";
            if (existingRequestUrns.contains(urn)) {
                errors.rejectValue(rejectField, errorCode, String.format(defaultMessageTemplate, "existing requests"));
            } else if (grantedRequests.stream().anyMatch(request -> request.getUrn().equals(urn))) {
                errors.rejectValue(rejectField, errorCode, String.format(defaultMessageTemplate, "granted requests"));
            } else if (newUpdateRequests.stream().anyMatch(request -> request.getFeature().getUrn().equals(urn))) {
                errors.rejectValue(rejectField,
                                   errorCode,
                                   String.format(defaultMessageTemplate, "new update requests"));
            } else {
                // Check if provided URN match an existing feature
                if (existingEntityUrns.contains(urn)) {
                    if (sessionMetadata.isUpdateIfExists()) {
                        // if updateIfExists option is enabled, register an update request instead of a creation one.
                        newUpdateRequests.add(buidUpdateEventFromCreationEvent(item));
                        return;
                    } else {
                        errors.rejectValue("urn", "feature.urn.already.exists.error.message", "URN already exists");
                    }
                } else {
                    // New version should be greater than previous one
                    List<IUrnVersionByProvider> previousVersions = featureEntityRepository.findByProviderIdInOrderByVersionDesc(
                        Collections.singletonList(feature.getId()));
                    if (!previousVersions.isEmpty() && previousVersions.get(0).getVersion() >= urn.getVersion()) {
                        errors.rejectValue("urn", "feature.urn.version.invalid.error.message", "Version is invalid");
                    }
                }
            }
        }

        if (errors.hasErrors()) {

            String errorFeatureId = featureId != null ? featureId : "UNKNOWN ID";
            LOGGER.error("Error during feature {} validation the following errors have been found : {}",
                         errorFeatureId,
                         errors);

            requestInfo.addDeniedRequest(requestId, ErrorTranslator.getErrors(errors));
            // Monitoring log
            FeatureLogger.creationDenied(requestOwner, requestId, featureId, ErrorTranslator.getErrors(errors));
            // Publish DENIED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                        requestId,
                                                        requestOwner,
                                                        featureId,
                                                        urn,
                                                        RequestState.DENIED,
                                                        ErrorTranslator.getErrors(errors)));
            metrics.count(featureId, null, FeatureCreationState.CREATION_REQUEST_DENIED);
            // Update session properties
            featureSessionNotifier.incrementCount(sessionOwner,
                                                  session,
                                                  FeatureSessionProperty.DENIED_REFERENCING_REQUESTS);

        } else {

            // Manage granted request
            FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity.build(sessionOwner,
                                                                                         session,
                                                                                         sessionMetadata.getStorages(),
                                                                                         sessionMetadata.isOverride());
            FeatureCreationRequest request = FeatureCreationRequest.build(requestId,
                                                                          requestOwner,
                                                                          item.getRequestDate(),
                                                                          RequestState.GRANTED,
                                                                          null,
                                                                          feature,
                                                                          metadata,
                                                                          FeatureRequestStep.LOCAL_DELAYED,
                                                                          sessionMetadata.getPriority());
            request.setUrn(urn);
            // Monitoring log
            FeatureLogger.creationGranted(request.getRequestOwner(), request.getRequestId(), request.getProviderId());
            // Publish GRANTED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                        requestId,
                                                        requestOwner,
                                                        featureId,
                                                        urn,
                                                        RequestState.GRANTED,
                                                        null));
            // Add to granted request collection
            metrics.count(request.getProviderId(), null, FeatureCreationState.CREATION_REQUEST_GRANTED);
            grantedRequests.add(request);
            requestInfo.addGrantedRequest(request.getProviderId(), request.getRequestId());
            // Update session properties
            featureSessionNotifier.incrementCount(sessionOwner, session, FeatureSessionProperty.REFERENCING_REQUESTS);
        }
    }

    /**
     * Creates a {@link FeatureUpdateRequestEvent} from a {@link FeatureCreationRequestEvent}. Useful to update an already existing feature
     * when a creationEvent is received with a given urn and updateIfExists option enabled.
     */
    private FeatureUpdateRequestEvent buidUpdateEventFromCreationEvent(FeatureCreationRequestEvent creationEvent) {
        FeatureUpdateRequestEvent updateEvent = new FeatureUpdateRequestEvent();
        updateEvent.setMetadata(FeatureMetadata.build(creationEvent.getMetadata().getPriority()));
        updateEvent.setRequestId(creationEvent.getRequestId());
        updateEvent.setFeature(creationEvent.getFeature());
        updateEvent.getMessageProperties()
                   .setHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER, creationEvent.getRequestId());
        updateEvent.getMessageProperties()
                   .setHeader(AmqpConstants.REGARDS_REQUEST_OWNER_HEADER, creationEvent.getRequestOwner());
        updateEvent.getMessageProperties()
                   .setHeader(AmqpConstants.REGARDS_REQUEST_DATE_HEADER, creationEvent.getRequestDate().toString());
        return updateEvent;
    }

    @Override
    public int scheduleRequests() {

        long scheduleStart = System.currentTimeMillis();

        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<String> featureIdsScheduled = new HashSet<>();
        Set<Long> requestIds = new HashSet<>();
        List<ILightFeatureCreationRequest> requestsToSchedule = new ArrayList<>();

        List<ILightFeatureCreationRequest> dbRequests = featureCreationRequestRepo.findRequestsToSchedule(0,
                                                                                                               properties.getMaxBulkSize());
        Optional<PriorityLevel> highestPriorityLevel = dbRequests.stream()
                                                                 .max((p1, p2) -> Math.max(p1.getPriority()
                                                                                             .getPriorityLevel(),
                                                                                           p2.getPriority()
                                                                                             .getPriorityLevel()))
                                                                 .map(IAbstractRequest::getPriority);

        if (!dbRequests.isEmpty()) {
            for (ILightFeatureCreationRequest request : dbRequests) {
                // we will schedule only one feature request for a feature id
                if (!featureIdsScheduled.contains(request.getProviderId())) {
                    metrics.count(request.getProviderId(), null, FeatureCreationState.CREATION_REQUEST_SCHEDULED);
                    requestsToSchedule.add(request);
                    requestIds.add(request.getId());
                    featureIdsScheduled.add(request.getProviderId());
                    // Update session properties
                    featureSessionNotifier.incrementCount(request, FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS);
                }
            }
            featureCreationRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureCreationJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the highest request priority request
            JobInfo jobInfo = new JobInfo(false,
                                          highestPriorityLevel.orElse(PriorityLevel.NORMAL).getPriorityLevel(),
                                          jobParameters,
                                          authResolver.getUser(),
                                          FeatureCreationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.trace("------------->>> {} creation requests scheduled in {} ms",
                         requestsToSchedule.size(),
                         System.currentTimeMillis() - scheduleStart);

            return requestIds.size();
        }

        return 0;
    }

    @Override
    public Set<FeatureEntity> processRequests(Set<Long> requestIds, FeatureCreationJob featureCreationJob) {

        Map<Boolean, List<FeatureCreationRequest>> requestByHasError = featureCreationRequestRepo.findAllByIdIn(
                                                                                                     requestIds)
                                                                                                 .stream()
                                                                                                 .collect(Collectors.partitioningBy(
                                                                                                     request -> FeatureRequestStep.REMOTE_STORAGE_ERROR.equals(
                                                                                                         request.getLastExecErrorStep())));
        List<FeatureCreationRequest> requests = requestByHasError.get(false);
        List<FeatureCreationRequest> retryRequests = requestByHasError.get(true);

        long processStart = System.currentTimeMillis();

        // Update requests with feature set and publish files to storage
        Set<FeatureEntity> entities = createEntities(requests, featureCreationJob);
        updateRequestsWithFiles(requests);

        // Update requests for storage retry
        updateRequestsWithFiles(retryRequests);

        // Handle session for referenced products
        doOnSuccess(requests);

        // handling of requests without files is already done, hence they are successful
        Set<FeatureCreationRequest> requestWithoutFiles = requests.stream()
                                                                  .filter(request -> CollectionUtils.isEmpty(request.getFeature()
                                                                                                                    .getFiles()))
                                                                  .collect(Collectors.toSet());
        handleSuccessfulCreation(requestWithoutFiles);

        LOGGER.trace("------------->>> {} creation requests processed in {} ms",
                     requests.size(),
                     System.currentTimeMillis() - processStart);

        return entities;
    }

    @Override
    public void handleSuccessfulCreation(Set<FeatureCreationRequest> requests) {

        long startSuccessProcess = System.currentTimeMillis();

        boolean isNotificationActive = notificationSettingsService.isActiveNotification();

        for (FeatureCreationRequest request : requests) {
            // Monitoring log
            FeatureLogger.creationSuccess(request.getRequestOwner(),
                                          request.getRequestId(),
                                          request.getProviderId(),
                                          request.getFeature().getUrn());
            // Publish successful request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                        request.getRequestId(),
                                                        request.getRequestOwner(),
                                                        request.getProviderId(),
                                                        request.getFeature().getUrn(),
                                                        RequestState.SUCCESS));
            // if a previous version exists we will publish a FeatureDeletionRequest to delete it
            if ((request.getFeatureEntity().getPreviousVersionUrn() != null) && request.getMetadata().isOverride()) {
                this.notificationClient.notify(String.format(
                                                   "A FeatureEntity with the URN %s already exists for this feature",
                                                   request.getFeatureEntity().getPreviousVersionUrn()),
                                               "A duplicated feature has been detected",
                                               NotificationLevel.INFO,
                                               DefaultRole.ADMIN);
                publisher.publish(FeatureDeletionRequestEvent.build(request.getMetadata().getSessionOwner(),
                                                                    request.getFeatureEntity().getPreviousVersionUrn(),
                                                                    PriorityLevel.NORMAL));
            }
            // notify creation of feature
            if (isNotificationActive) {
                request.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                featureCreationRequestRepo.save(request);
            }
        }

        if (!requests.isEmpty()) {
            // If notification are not active, creation is ended and requests can be deleted. Else, we need to wait for
            // notification response status.
            if (!isNotificationActive) {
                doOnTerminated(requests);
                featureCreationRequestRepo.deleteByUrnIn(requests.stream()
                                                                 .map(AbstractFeatureRequest::getUrn)
                                                                 .collect(Collectors.toSet()));
                requests.forEach(em::detach);
                LOGGER.trace("------------->>> {} creation requests deleted in {} ms",
                             requests.size(),
                             System.currentTimeMillis() - startSuccessProcess);
            }
        }
        LOGGER.trace("------------->>> {} creation requests have been successfully handled in {} ms",
                     requests.size(),
                     System.currentTimeMillis() - startSuccessProcess);
    }

    private Set<FeatureEntity> createEntities(List<FeatureCreationRequest> requests,
                                              FeatureCreationJob featureCreationJob) {

        long start = System.currentTimeMillis();

        // Fetch the latest versions and URNs for each provider
        List<String> providerIds = requests.stream()
                                           .map(FeatureCreationRequest::getProviderId)
                                           .collect(Collectors.toList());
        Map<String, Integer> versionByProviders = new HashMap<>();
        Map<String, FeatureUniformResourceName> urnByProviders = new HashMap<>();
        featureEntityRepository.findByProviderIdInOrderByVersionDesc(providerIds).forEach(versionAndUrnByProvider -> {
            String providerId = versionAndUrnByProvider.getProviderId();
            // Since we fetch by version DESC, the first one is the latest version
            if (!versionByProviders.containsKey(providerId)) {
                versionByProviders.put(providerId, versionAndUrnByProvider.getVersion());
                urnByProviders.put(providerId, versionAndUrnByProvider.getUrn());
            }
        });

        Set<FeatureEntity> entities = requests.stream().map(request -> {
            Integer previousVersion = versionByProviders.get(request.getProviderId());
            FeatureUniformResourceName previousUrn = urnByProviders.get(request.getProviderId());
            return initFeatureEntity(request, previousVersion, previousUrn, featureCreationJob);
        }).collect(Collectors.toSet());
        featureEntityRepository.saveAll(entities);

        LOGGER.trace("------------->>> {} feature saved in {} ms", entities.size(), System.currentTimeMillis() - start);

        // Set 'last' to false for all previous versions
        Set<String> previousUrns = entities.stream()
                                           .map(FeatureEntity::getPreviousVersionUrn)
                                           .filter(Objects::nonNull)
                                           .map(FeatureUniformResourceName::toString)
                                           .collect(Collectors.toSet());
        if (!previousUrns.isEmpty()) {
            Timestamp now = Timestamp.valueOf(OffsetDateTime.now()
                                                            .withOffsetSameInstant(ZoneOffset.UTC)
                                                            .toLocalDateTime());
            featureCreationRequestRepo.updateLastByUrnIn(false, now, previousUrns);
        }

        return entities;
    }

    /**
     * Init a {@link FeatureEntity} from a {@link FeatureCreationRequest} and set it
     * as feature entity
     *
     * @param featureCreationRequest from we will create the {@link FeatureEntity}
     * @param previousVersion        previous urn for the last version
     * @return initialized feature entity
     */
    private FeatureEntity initFeatureEntity(FeatureCreationRequest featureCreationRequest,
                                            @Nullable Integer previousVersion,
                                            FeatureUniformResourceName previousUrn,
                                            FeatureCreationJob featureCreationJob) {

        Feature feature = featureCreationRequest.getFeature();
        feature.withHistory(featureCreationRequest.getRequestOwner());

        if (feature.getUrn() == null) {
            UUID uuid = UUID.nameUUIDFromBytes(feature.getId().getBytes());
            FeatureUniformResourceName urn = FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                              feature.getEntityType(),
                                                                              runtimeTenantResolver.getTenant(),
                                                                              uuid,
                                                                              computeNextVersion(previousVersion));
            feature.setUrn(urn);
        }

        feature.setLast(true);

        FeatureEntity featureEntity = FeatureEntity.build(featureCreationRequest.getMetadata().getSessionOwner(),
                                                          featureCreationRequest.getMetadata().getSession(),
                                                          feature,
                                                          previousUrn,
                                                          featureCreationRequest.getFeature().getModel());

        featureCreationRequest.setFeatureEntity(featureEntity);
        featureCreationRequest.setUrn(featureEntity.getUrn());

        if (featureCreationJob != null) {
            featureCreationJob.advanceCompletion();
        }
        metrics.count(featureCreationRequest.getProviderId(),
                      featureEntity.getUrn(),
                      FeatureCreationState.FEATURE_INITIALIZED);

        return featureEntity;
    }

    /**
     * After creation requests has been deleted, we have to delete all feature created.
     * Indeed, if a creation request is not completed then the associated feature is not valid.
     *
     * @param deletedRequests deleted {@link FeatureCreationRequest} requests
     */
    protected void postRequestDeleted(Collection<FeatureCreationRequest> deletedRequests) {
        // NOTE : Do not delete feature associated to creation request in error if the error is REMOTE_NOTIFICATION_ERROR
        // For all other errors during creation, the deletion of the request means deletion of the feature if exists.
        List<FeatureUniformResourceName> urnToDelete = deletedRequests.stream()
                                                                      .filter(r -> r.getFeatureEntity() != null)
                                                                      .filter(r -> r.getStep()
                                                                                   != FeatureRequestStep.REMOTE_NOTIFICATION_ERROR)
                                                                      .map(r -> r.getFeatureEntity().getUrn())
                                                                      .toList();
        featureDeletionService.registerRequests(FeatureDeletionCollection.build(urnToDelete, PriorityLevel.NORMAL));
    }

    /**
     * Compute the next version for a specific provider id we will increment the version passed in parameter
     * a null parameter mean a first version
     */
    private int computeNextVersion(Integer previousVersion) {
        return previousVersion == null ? 1 : previousVersion + 1;
    }

    private void updateRequestsWithFiles(List<FeatureCreationRequest> requests) {
        long subProcessStart = System.currentTimeMillis();
        Set<FeatureCreationRequest> requestWithFiles = requests.stream()
                                                               .filter(fcr -> !CollectionUtils.isEmpty(fcr.getFeature()
                                                                                                          .getFiles()))
                                                               .map(featureFilesService::handleRequestFiles)
                                                               .collect(Collectors.toSet());
        featureCreationRequestRepo.saveAll(requestWithFiles);
        LOGGER.trace("------------->>> {} creation requests with files updated in {} ms",
                     requestWithFiles.size(),
                     System.currentTimeMillis() - subProcessStart);
    }

    @Override
    public void handleStorageError(Collection<RequestResultInfoDTO> errorRequests) {
        Map<String, String> errorByGroupId = Maps.newHashMap();
        errorRequests.forEach(e -> errorByGroupId.put(e.getGroupId(), e.getErrorCause()));

        Set<FeatureCreationRequest> requests = featureCreationRequestRepo.findByGroupIdIn(errorByGroupId.keySet());

        if (!requests.isEmpty()) {
            // publish error notification for all request id
            requests.forEach(item -> publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                                                 item.getRequestId(),
                                                                                 item.getRequestOwner(),
                                                                                 item.getProviderId(),
                                                                                 item.getUrn(),
                                                                                 RequestState.ERROR,
                                                                                 null)));
            // set FeatureCreationRequest to error state
            for (FeatureCreationRequest request : requests) {
                String errorCause = Optional.ofNullable(errorByGroupId.get(request.getGroupId()))
                                            .orElse("unknown error.");
                LOGGER.error("Error received from storage for request {}. Cause : {}",
                             request.getProviderId(),
                             errorCause);
                addRemoteStorageError(request, errorCause);
            }
            doOnError(requests);
            featureCreationRequestRepo.saveAll(requests);
        }
    }

    @Override
    public FeatureRequestType getRequestType() {
        return FeatureRequestType.CREATION;
    }

    @Override
    protected void logRequestDenied(String requestOwner, String requestId, Set<String> errors) {
        FeatureLogger.creationDenied(requestOwner, requestId, null, errors);
    }

    @Override
    public Page<FeatureCreationRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page) {
        return featureCreationRequestRepo.findAll(new FeatureCreationRequestSpecificationsBuilder().withParameters(
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
            return RequestsInfo.build(featureCreationRequestRepo.count(new FeatureCreationRequestSpecificationsBuilder().withParameters(
                filters).build()));
        }
    }

    @Override
    protected IAbstractFeatureRequestRepository<FeatureCreationRequest> getRequestsRepository() {
        return featureCreationRequestRepo;
    }

    @Override
    protected FeatureCreationRequest updateForRetry(FeatureCreationRequest request) {
        // Nothing to do
        return request;
    }

    @Override
    protected void sessionInfoUpdateForRetry(Collection<FeatureCreationRequest> requests) {
        requests.forEach(request -> {
            featureSessionNotifier.decrementCount(request, FeatureSessionProperty.IN_ERROR_REFERENCING_REQUESTS);
            if (FeatureRequestStep.REMOTE_NOTIFICATION_ERROR.equals(request.getLastExecErrorStep())) {
                featureSessionNotifier.incrementCount(request, FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS);
            }
        });
    }

    @Override
    protected void sessionInfoUpdateForDelete(Collection<FeatureCreationRequest> requestList) {
        requestList.forEach(request -> {
            featureSessionNotifier.decrementCount(request, FeatureSessionProperty.IN_ERROR_REFERENCING_REQUESTS);
            featureSessionNotifier.decrementCount(request, FeatureSessionProperty.REFERENCING_REQUESTS);
        });
    }

    @Override
    public void doOnSuccess(Collection<FeatureCreationRequest> requests) {
        requests.forEach(request -> {
            featureSessionNotifier.incrementCount(request, FeatureSessionProperty.REFERENCED_PRODUCTS);
        });
    }

    @Override
    public void doOnTerminated(Collection<FeatureCreationRequest> requests) {
        requests.forEach(request -> {
            featureSessionNotifier.decrementCount(request, FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS);
        });
    }

    @Override
    public void doOnError(Collection<FeatureCreationRequest> requests) {
        requests.forEach(request -> {
            featureSessionNotifier.incrementCount(request, FeatureSessionProperty.IN_ERROR_REFERENCING_REQUESTS);
            featureSessionNotifier.decrementCount(request, FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS);
        });
    }

}
