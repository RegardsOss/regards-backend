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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
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
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.ILightFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.LightFeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationCollection;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.feature.dto.FeatureSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.FeatureMetrics.FeatureCreationState;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureCreationJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;

/**
 * Feature service management
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureCreationService extends AbstractFeatureService implements IFeatureCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCreationService.class);

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Autowired
    private ILightFeatureCreationRequestRepository featureCreationRequestLightRepo;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IFeatureValidationService validationService;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private FeatureMetrics metrics;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private Gson gson;

    @Autowired
    private Validator validator;

    @Override
    public RequestInfo<String> registerRequests(List<FeatureCreationRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();

        List<FeatureCreationRequest> grantedRequests = new ArrayList<>();
        RequestInfo<String> requestInfo = new RequestInfo<>();
        Set<String> existingRequestIds = this.featureCreationRequestRepo.findRequestId();

        events.forEach(item -> prepareFeatureCreationRequest(item, grantedRequests, requestInfo, existingRequestIds));
        LOGGER.trace("------------->>> {} creation requests prepared in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureCreationRequest from a list of FeatureCreationRequestEvent
        featureCreationRequestRepo.saveAll(grantedRequests);
        LOGGER.trace("------------->>> {} creation requests registered in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        return requestInfo;
    }

    @Override
    public RequestInfo<String> registerRequests(FeatureCreationCollection collection) {
        // Build events to reuse event registration code
        List<FeatureCreationRequestEvent> toTreat = new ArrayList<FeatureCreationRequestEvent>();
        for (Feature feature : collection.getFeatures()) {
            toTreat.add(FeatureCreationRequestEvent.build(collection.getRequestOwner(), collection.getMetadata(),
                                                          feature));
        }
        return registerRequests(toTreat);
    }

    /**
     * Validate a list of {@link FeatureCreationRequestEvent}
     * and if validated create a list of {@link FeatureCreationRequest}
     *
     * @param item            request to manage
     * @param grantedRequests collection of granted requests to populate
     * @param requestInfo store request registration state
     * @param existingRequestIds list of existing request ids in database (its a unique constraint)
     */
    private void prepareFeatureCreationRequest(FeatureCreationRequestEvent item,
            List<FeatureCreationRequest> grantedRequests, RequestInfo<String> requestInfo,
            Set<String> existingRequestIds) {

        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId())
                || grantedRequests.stream().anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        // Validate feature according to the data model
        errors.addAllErrors(validationService.validate(item.getFeature(), ValidationMode.CREATION));

        if (errors.hasErrors()) {
            LOGGER.error("Error during feature {} validation the following errors have been founded{}",
                         item.getFeature().getId(), errors.toString());
            requestInfo.addDeniedRequest(item.getRequestId(), ErrorTranslator.getErrors(errors));
            // Monitoring log
            FeatureLogger.creationDenied(item.getRequestOwner(), item.getRequestId(),
                                         item.getFeature() != null ? item.getFeature().getId() : null,
                                         ErrorTranslator.getErrors(errors));
            // Publish DENIED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION, item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        item.getFeature() != null ? item.getFeature().getId() : null,
                                                        null, RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            metrics.count(item.getFeature() != null ? item.getFeature().getId() : null, null,
                          FeatureCreationState.CREATION_REQUEST_DENIED);
            return;
        }
        FeatureSessionMetadata md = item.getMetadata();
        // Manage granted request
        FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity
                .build(md.getSessionOwner(), md.getSession(), item.getMetadata().getStorages(),
                       item.getMetadata().isOverride());
        FeatureCreationRequest request = FeatureCreationRequest
                .build(item.getRequestId(), item.getRequestOwner(), item.getRequestDate(), RequestState.GRANTED, null,
                       item.getFeature(), metadata, FeatureRequestStep.LOCAL_DELAYED, item.getMetadata().getPriority());
        // Monitoring log
        FeatureLogger.creationGranted(request.getRequestOwner(), request.getRequestId(), request.getProviderId());
        // Publish GRANTED request
        publisher.publish(FeatureRequestEvent
                .build(FeatureRequestType.CREATION, item.getRequestId(), item.getRequestOwner(),
                       item.getFeature() != null ? item.getFeature().getId() : null, null, RequestState.GRANTED, null));

        // Add to granted request collection
        metrics.count(request.getProviderId(), null, FeatureCreationState.CREATION_REQUEST_GRANTED);
        grantedRequests.add(request);
        requestInfo.addGrantedRequest(request.getProviderId(), request.getRequestId());
    }

    @Override
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();

        // Shedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<String> featureIdsScheduled = new HashSet<>();
        Set<Long> requestIds = new HashSet<>();
        List<LightFeatureCreationRequest> requestsToSchedule = new ArrayList<>();

        List<LightFeatureCreationRequest> dbRequests = this.featureCreationRequestLightRepo
                .findRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(), PageRequest
                        .of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));

        if (!dbRequests.isEmpty()) {
            for (LightFeatureCreationRequest request : dbRequests) {
                // we will schedule only one feature request for a feature id
                if (!featureIdsScheduled.contains(request.getProviderId())) {
                    metrics.count(request.getProviderId(), null, FeatureCreationState.CREATION_REQUEST_SCHEDULED);
                    requestsToSchedule.add(request);
                    requestIds.add(request.getId());
                    featureIdsScheduled.add(request.getProviderId());
                }
            }
            featureCreationRequestLightRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureCreationJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false, requestsToSchedule.get(0).getPriority().getPriorityLevel(),
                    jobParameters, authResolver.getUser(), FeatureCreationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.trace("------------->>> {} creation requests scheduled in {} ms", requestsToSchedule.size(),
                         System.currentTimeMillis() - scheduleStart);

            return requestIds.size();
        }

        return 0;
    }

    @Override
    public Set<FeatureEntity> processRequests(List<FeatureCreationRequest> requests) {

        long processStart = System.currentTimeMillis();
        long subProcessStart;

        // Look for versions
        List<String> providerIds = new ArrayList<>();
        requests.forEach(r -> providerIds.add(r.getProviderId()));
        Map<String, Integer> versionByProviders = new HashMap<>();
        Map<String, FeatureUniformResourceName> urnByProviders = new HashMap<>();
        for (IUrnVersionByProvider vbp : featureRepo.findByProviderIdInOrderByVersionDesc(providerIds)) {
            if (!versionByProviders.containsKey(vbp.getProviderId())) {
                // Register max version
                versionByProviders.put(vbp.getProviderId(), vbp.getVersion());
                urnByProviders.put(vbp.getProviderId(), vbp.getUrn());
            }
        }

        // Register features
        subProcessStart = System.currentTimeMillis();
        Set<FeatureEntity> entities = requests.stream()
                .map(request -> initFeatureEntity(request, versionByProviders.get(request.getProviderId()),
                                                  urnByProviders.get(request.getProviderId())))
                .collect(Collectors.toSet());
        this.featureRepo.saveAll(entities);
        LOGGER.trace("------------->>> {} feature saved in {} ms", entities.size(),
                     System.currentTimeMillis() - subProcessStart);

        // Update requests with feature setted for each of them + publish files to storage
        subProcessStart = System.currentTimeMillis();
        List<FeatureCreationRequest> requestsWithFiles = requests.stream()
                .filter(fcr -> (fcr.getFeature().getFiles() != null) && !fcr.getFeature().getFiles().isEmpty())
                .map(fcr -> publishFiles(fcr)).collect(Collectors.toList());
        this.featureCreationRequestRepo.saveAll(requestsWithFiles);
        LOGGER.trace("------------->>> {} creation requests with files updated in {} ms", requestsWithFiles.size(),
                     System.currentTimeMillis() - subProcessStart);

        // Delete requests without files
        subProcessStart = System.currentTimeMillis();
        List<FeatureCreationRequest> requestsWithoutFiles = new ArrayList<>();
        for (FeatureCreationRequest request : requests) {
            if ((request.getFeature().getFiles() == null) || request.getFeature().getFiles().isEmpty()) {
                // Register request
                requestsWithoutFiles.add(request);
                // Monitoring log
                FeatureLogger.creationSuccess(request.getRequestOwner(), request.getRequestId(),
                                              request.getProviderId(), request.getFeature().getUrn());
                // Publish successful request
                publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION, request.getRequestId(),
                                                            request.getRequestOwner(), request.getProviderId(),
                                                            request.getFeature().getUrn(), RequestState.SUCCESS));

                // if a previous version exists we will publish a FeatureDeletionRequest to delete it
                if ((request.getFeatureEntity().getPreviousVersionUrn() != null)
                        && request.getMetadata().isOverride()) {
                    this.notificationClient
                            .notify(String.format("A FeatureEntity with the URN {} already exists for this feature",
                                                  request.getFeatureEntity().getPreviousVersionUrn()),
                                    "A duplicated feature has been detected", NotificationLevel.INFO,
                                    DefaultRole.ADMIN);
                    publisher.publish(FeatureDeletionRequestEvent
                            .build(request.getMetadata().getSessionOwner(),
                                   request.getFeatureEntity().getPreviousVersionUrn(), PriorityLevel.NORMAL));
                }
            }
        }

        if (!requestsWithoutFiles.isEmpty()) {
            // notify feature creation without files
            publisher.publish(requestsWithoutFiles.stream()
                    .map(request -> NotificationActionEvent.build(gson.toJsonTree(request.getFeature()),
                                                                  FeatureManagementAction.CREATED.name()))
                    .collect(Collectors.toList()));
        }

        // Successful requests are deleted now!
        featureCreationRequestRepo.deleteInBatch(requestsWithoutFiles);
        LOGGER.trace("------------->>> {} creation requests without files deleted in {} ms",
                     requestsWithoutFiles.size(), System.currentTimeMillis() - subProcessStart);

        LOGGER.trace("------------->>> {} creation requests processed in {} ms", requests.size(),
                     System.currentTimeMillis() - processStart);
        return entities;
    }

    /**
     * Publish all contained files inside the {@link FeatureCreationRequest} to
     * storage
     *
     * @param fcr currently createing feature
     * @return
     */
    private FeatureCreationRequest publishFiles(FeatureCreationRequest fcr) {
        for (FeatureFile file : fcr.getFeature().getFiles()) {
            FeatureFileAttributes attribute = file.getAttributes();
            for (FeatureFileLocation loc : file.getLocations()) {
                // there is no metadata but a file location so we will update reference
                if (!fcr.getMetadata().hasStorage()) {
                    fcr.setGroupId(this.storageClient
                            .reference(FileReferenceRequestDTO
                                    .build(attribute.getFilename(), attribute.getChecksum(), attribute.getAlgorithm(),
                                           attribute.getMimeType().toString(), attribute.getFilesize(),
                                           fcr.getFeature().getUrn().toString(), loc.getStorage(), loc.getUrl()))
                            .getGroupId());
                }
                for (StorageMetadata metadata : fcr.getMetadata().getStorages()) {
                    if (loc.getStorage() == null) {
                        fcr.setGroupId(this.storageClient.store(FileStorageRequestDTO
                                .build(attribute.getFilename(), attribute.getChecksum(), attribute.getAlgorithm(),
                                       attribute.getMimeType().toString(), fcr.getFeature().getUrn().toString(),
                                       loc.getUrl(), metadata.getPluginBusinessId(), Optional.of(loc.getUrl())))
                                .getGroupId());
                    } else {
                        fcr.setGroupId(this.storageClient.reference(FileReferenceRequestDTO
                                .build(attribute.getFilename(), attribute.getChecksum(), attribute.getAlgorithm(),
                                       attribute.getMimeType().toString(), attribute.getFilesize(),
                                       fcr.getFeature().getUrn().toString(), loc.getStorage(), loc.getUrl()))
                                .getGroupId());
                    }
                }
            }
        }
        return fcr;
    }

    /**
     * Init a {@link FeatureEntity} from a {@link FeatureCreationRequest} and set it
     * as feature entity
     * @param previousVersion number of last version in database for a provider id
     * @param fcr from we will create the {@link FeatureEntity}
     * @param previousVersion previous urn for the last version
     * @return
     */
    private FeatureEntity initFeatureEntity(FeatureCreationRequest fcr, @Nullable Integer previousVersion,
            FeatureUniformResourceName previousUrn) {

        Feature feature = fcr.getFeature();
        feature.withHistory(fcr.getRequestOwner());

        UUID uuid = UUID.nameUUIDFromBytes(feature.getId().getBytes());
        feature.setUrn(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, feature.getEntityType(),
                                                        runtimeTenantResolver.getTenant(), uuid,
                                                        computeNextVersion(previousVersion)));

        FeatureEntity created = FeatureEntity.build(fcr.getMetadata().getSessionOwner(), fcr.getMetadata().getSession(),
                                                    feature, previousUrn, fcr.getFeature().getModel());
        created.setVersion(feature.getUrn().getVersion());
        fcr.setFeatureEntity(created);

        metrics.count(fcr.getProviderId(), created.getUrn(), FeatureCreationState.FEATURE_INITIALIZED);

        return created;
    }

    /**
     * Compute the next version for a specific provider id we will increment the version passed in parameter
     * a null parameter mean a first version
     */
    private int computeNextVersion(Integer previousVersion) {
        return previousVersion == null ? 1 : previousVersion + 1;
    }
}
