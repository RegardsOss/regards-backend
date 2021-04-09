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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;

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
import fr.cnes.regards.modules.feature.dao.FeatureCreationRequestSpecification;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationCollection;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.FeatureSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
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
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
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
    private Validator validator;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @PersistenceContext
    private EntityManager em;

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
        List<FeatureCreationRequestEvent> toTreat = new ArrayList<>();
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
            LOGGER.error("Error during feature {} validation the following errors have been founded : {}",
                         item.getFeature() != null ? item.getFeature().getId() : "UNKNOWN ID", errors);
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

        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<String> featureIdsScheduled = new HashSet<>();
        Set<Long> requestIds = new HashSet<>();
        List<ILightFeatureCreationRequest> requestsToSchedule = new ArrayList<>();

        List<ILightFeatureCreationRequest> dbRequests = this.featureCreationRequestRepo
                .findRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(),
                                        PageRequest.of(0, properties.getMaxBulkSize(),
                                                       Sort.by(Order.asc("priority"), Order.asc("requestDate"))))
                .getContent();

        if (!dbRequests.isEmpty()) {
            for (ILightFeatureCreationRequest request : dbRequests) {
                // we will schedule only one feature request for a feature id
                if (!featureIdsScheduled.contains(request.getProviderId())) {
                    metrics.count(request.getProviderId(), null, FeatureCreationState.CREATION_REQUEST_SCHEDULED);
                    requestsToSchedule.add(request);
                    requestIds.add(request.getId());
                    featureIdsScheduled.add(request.getProviderId());
                }
            }
            featureCreationRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

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
    public Set<FeatureEntity> processRequests(Set<Long> requestIds, FeatureCreationJob featureCreationJob) {
        List<FeatureCreationRequest> requests = featureCreationRequestRepo.findAllByIdIn(requestIds);
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
                                                  urnByProviders.get(request.getProviderId()), featureCreationJob))
                .collect(Collectors.toSet());
        // get previous versions to set last to false
        Set<String> previousUrns = entities.stream().filter(entity -> entity.getPreviousVersionUrn() != null)
                .map(entity -> entity.getPreviousVersionUrn().toString()).collect(Collectors.toSet());

        // save new features
        this.featureRepo.saveAll(entities);
        if (!previousUrns.isEmpty()) {
            featureCreationRequestRepo.updateLastByUrnIn(false, previousUrns);
        }
        LOGGER.trace("------------->>> {} feature saved in {} ms", entities.size(),
                     System.currentTimeMillis() - subProcessStart);
        // Update requests with feature setted for each of them + publish files to storage
        subProcessStart = System.currentTimeMillis();
        Set<FeatureCreationRequest> requestWithFiles = requests.stream()
                .filter(fcr -> (fcr.getFeature().getFiles() != null) && !fcr.getFeature().getFiles().isEmpty())
                .map(this::handleRequestWithFiles).collect(Collectors.toSet());
        featureCreationRequestRepo.saveAll(requestWithFiles);
        LOGGER.trace("------------->>> {} creation requests with files updated in {} ms", requestWithFiles.size(),
                     System.currentTimeMillis() - subProcessStart);

        // Delete requests without files
        Set<FeatureCreationRequest> requestWithoutFiles = requests.stream()
                .filter(request -> (request.getFeature().getFiles() == null)
                        || request.getFeature().getFiles().isEmpty())
                .collect(Collectors.toSet());
        // handling of requests without files is already done so they are successful
        handleSuccessfulCreation(requestWithoutFiles);
        LOGGER.trace("------------->>> {} creation requests processed in {} ms", requests.size(),
                     System.currentTimeMillis() - processStart);
        return entities;
    }

    @Override
    public void handleSuccessfulCreation(Set<FeatureCreationRequest> requests) {
        long startSuccessProcess = System.currentTimeMillis();
        for (FeatureCreationRequest request : requests) {
            // Monitoring log
            FeatureLogger.creationSuccess(request.getRequestOwner(), request.getRequestId(), request.getProviderId(),
                                          request.getFeature().getUrn());
            // Publish successful request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.CREATION, request.getRequestId(),
                                                        request.getRequestOwner(), request.getProviderId(),
                                                        request.getFeature().getUrn(), RequestState.SUCCESS));

            // if a previous version exists we will publish a FeatureDeletionRequest to delete it
            if ((request.getFeatureEntity().getPreviousVersionUrn() != null) && request.getMetadata().isOverride()) {
                this.notificationClient
                        .notify(String.format("A FeatureEntity with the URN %s already exists for this feature",
                                              request.getFeatureEntity().getPreviousVersionUrn()),
                                "A duplicated feature has been detected", NotificationLevel.INFO, DefaultRole.ADMIN);
                publisher.publish(FeatureDeletionRequestEvent.build(request.getMetadata().getSessionOwner(),
                                                                    request.getFeatureEntity().getPreviousVersionUrn(),
                                                                    PriorityLevel.NORMAL));
            }
        }

        if (!requests.isEmpty()) {
            // See if notifications are required
            if (notificationSettingsService.retrieve().isActiveNotification()) {
                // notify creation of feature
                for (FeatureCreationRequest request : requests) {
                    request.setStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED);
                    featureCreationRequestRepo.save(request);
                }
            } else {
                for (FeatureCreationRequest fcr : requests) {
                    em.detach(fcr);
                }
                // Successful requests are deleted now!
                featureCreationRequestRepo.deleteByUrnIn(requests.stream().map(AbstractFeatureRequest::getUrn)
                        .collect(Collectors.toSet()));
                LOGGER.trace("------------->>> {} creation requests deleted in {} ms", requests.size(),
                             System.currentTimeMillis() - startSuccessProcess);
            }
        }
        LOGGER.trace("------------->>> {} creation requests have been successfully handled in {} ms", requests.size(),
                     System.currentTimeMillis() - startSuccessProcess);
    }

    /**
     * Handle {@link FeatureCreationRequest} with files to be stored or referenced by storage microservice:
     * <ul>
     *     <li>No storage metadata at all -> feature files are to be referenced</li>
     *     <li>for each metadata without any data storage identifier specified -> feature files are to be stored</li>
     *     <li>for each metadata with a data storage identifier specified -> feature files are to be referenced</li>
     * </ul>
     *
     * @param fcr currently creating feature
     */
    private FeatureCreationRequest handleRequestWithFiles(FeatureCreationRequest fcr) {
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
        fcr.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
        return fcr;
    }

    /**
     * Init a {@link FeatureEntity} from a {@link FeatureCreationRequest} and set it
     * as feature entity
     * @param fcr from we will create the {@link FeatureEntity}
     * @param previousVersion previous urn for the last version
     * @param featureCreationJob
     * @return initialized feature entity
     */
    private FeatureEntity initFeatureEntity(FeatureCreationRequest fcr, @Nullable Integer previousVersion,
            FeatureUniformResourceName previousUrn, FeatureCreationJob featureCreationJob) {

        Feature feature = fcr.getFeature();
        feature.withHistory(fcr.getRequestOwner());

        UUID uuid = UUID.nameUUIDFromBytes(feature.getId().getBytes());
        feature.setUrn(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, feature.getEntityType(),
                                                        runtimeTenantResolver.getTenant(), uuid,
                                                        computeNextVersion(previousVersion)));
        // as version compute is previous + 1, this feature is forcibly the last
        feature.setLast(true);
        FeatureEntity created = FeatureEntity.build(fcr.getMetadata().getSessionOwner(), fcr.getMetadata().getSession(),
                                                    feature, previousUrn, fcr.getFeature().getModel());
        created.setVersion(feature.getUrn().getVersion());
        fcr.setFeatureEntity(created);
        fcr.setUrn(created.getUrn());
        if (featureCreationJob != null) {
            featureCreationJob.advanceCompletion();
        }
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

    @Override
    public FeatureRequestType getRequestType() {
        return FeatureRequestType.CREATION;
    }

    @Override
    protected void logRequestDenied(String requestOwner, String requestId, Set<String> errors) {
        FeatureLogger.creationDenied(requestOwner, requestId, null, errors);
    }

    @Override
    public Page<FeatureCreationRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page) {
        return featureCreationRequestRepo
                .findAll(FeatureCreationRequestSpecification.searchAllByFilters(selection, page), page);
    }

    @Override
    public RequestsInfo getInfo(FeatureRequestsSelectionDTO selection) {
        if ((selection.getFilters() != null) && ((selection.getFilters().getState() != null)
                && (selection.getFilters().getState() != RequestState.ERROR))) {
            return RequestsInfo.build(0L);
        } else {
            selection.getFilters().withState(RequestState.ERROR);
            return RequestsInfo.build(featureCreationRequestRepo
                    .count(FeatureCreationRequestSpecification.searchAllByFilters(selection, PageRequest.of(0, 1))));
        }
    }

    @Override
    public void deleteRequests(FeatureRequestsSelectionDTO selection) {
        Pageable page = PageRequest.of(0, 500);
        Page<FeatureCreationRequest> requestsPage;
        boolean stop = false;
        do {
            requestsPage = findRequests(selection, page);
            featureCreationRequestRepo.deleteAll(requestsPage.filter(r -> r.isDeletable()));
            if ((requestsPage.getNumber() < MAX_PAGE_TO_DELETE) && requestsPage.hasNext()) {
                page = requestsPage.nextPageable();
            } else {
                stop = true;
            }
        } while (!stop);
    }
}
