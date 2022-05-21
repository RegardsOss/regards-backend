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
package fr.cnes.regards.modules.feature.service.request;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.*;
import fr.cnes.regards.modules.feature.service.dump.IFeatureMetadataService;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kevin
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FeatureRequestService implements IFeatureRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureRequestService.class);

    @Autowired
    private IFeatureCreationRequestRepository fcrRepo;

    @Autowired
    private IFeatureUpdateRequestRepository furRepo;

    @Autowired
    private IFeatureDeletionRequestRepository fdrRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    @Autowired
    public IFeatureCreationService featureCreationService;

    @Autowired
    public IFeatureDeletionService featureDeletionService;

    @Autowired
    public IFeatureUpdateService featureUpdateService;

    @Autowired
    public IFeatureNotificationService featureNotificationService;

    @Autowired
    public IFeatureCopyService featureCopyService;

    @Autowired
    public IFeatureMetadataService featureMetadataService;

    @Autowired
    public IFeatureEntityRepository featureRepo;

    @Override
    public RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type,
                                                   FeatureRequestsSelectionDTO selection,
                                                   Pageable page) {
        Page<FeatureRequestDTO> results = new PageImpl<>(Lists.newArrayList(), page, 0L);
        RequestsInfo info = RequestsInfo.build(0L);
        switch (type) {
            case COPY:
                results = featureCopyService.findRequests(selection, page)
                                            .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCopyService.getInfo(selection);
                break;
            case CREATION:
                results = featureCreationService.findRequests(selection, page)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCreationService.getInfo(selection);
                break;
            case DELETION:
                results = featureDeletionService.findRequests(selection, page)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureDeletionService.getInfo(selection);
                break;
            case NOTIFICATION:
                results = featureNotificationService.findRequests(selection, page)
                                                    .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureNotificationService.getInfo(selection);
                break;
            case SAVE_METADATA:
                results = featureMetadataService.findRequests(selection, page)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureMetadataService.getInfo(selection);
                break;
            case UPDATE:
                results = featureUpdateService.findRequests(selection, page)
                                              .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureUpdateService.getInfo(selection);
                break;
            default:
                LOGGER.error("Not available type {} for Feature Requests", type.toString());
                break;
        }

        addProviderIdsToRequests(results);
        return new RequestsPage<>(results.getContent(), info, results.getPageable(), results.getTotalElements());
    }

    @Override
    public RequestHandledResponse delete(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection) {
        switch (type) {
            case COPY:
                return featureCopyService.deleteRequests(selection);
            case CREATION:
                return featureCreationService.deleteRequests(selection);
            case DELETION:
                return featureDeletionService.deleteRequests(selection);
            case NOTIFICATION:
                return featureNotificationService.deleteRequests(selection);
            case SAVE_METADATA:
                return featureMetadataService.deleteRequests(selection);
            case UPDATE:
                return featureUpdateService.deleteRequests(selection);
            default:
                String message = String.format("Not available type %s for Feature Requests", type.toString());
                LOGGER.error(message);
                return RequestHandledResponse.build(0, 0, message);
        }
    }

    @Override
    public RequestHandledResponse retry(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection) {
        switch (type) {
            case COPY:
                return featureCopyService.retryRequests(selection);
            case CREATION:
                return featureCreationService.retryRequests(selection);
            case DELETION:
                return featureDeletionService.retryRequests(selection);
            case NOTIFICATION:
                return featureNotificationService.retryRequests(selection);
            case SAVE_METADATA:
                return featureMetadataService.retryRequests(selection);
            case UPDATE:
                return featureUpdateService.retryRequests(selection);
            default:
                String message = String.format("Not available type %s for Feature Requests", type.toString());
                LOGGER.error(message);
                return RequestHandledResponse.build(0, 0, message);
        }
    }

    @Override
    public void updateRequestsStatus(Set<Long> requestIds, RequestState status) {
        this.abstractFeatureRequestRepo.updateState(status, requestIds);
    }

    @Override
    public void handleStorageSuccess(Set<RequestResultInfoDTO> requestsInfo) {
        long scheduleStart = System.currentTimeMillis();
        LOGGER.trace("Handling {} storage success responses from storage", requestsInfo.size());
        Map<String, List<RequestResultInfoDTO>> requestInfoPerGroupId = requestsInfo.stream()
                                                                                    .collect(Collectors.groupingBy(
                                                                                        RequestResultInfoDTO::getGroupId));
        Map<FeatureEntity, List<RequestResultInfoDTO>> updateInfos = new HashMap<>();

        // Find FeatureCreationRequest associated to success storage responses if any
        Set<FeatureCreationRequest> creationRequests = this.fcrRepo.findByGroupIdIn(requestInfoPerGroupId.keySet());
        for (FeatureCreationRequest r : creationRequests) {
            updateInfos.put(r.getFeatureEntity(), requestInfoPerGroupId.get(r.getGroupId()));
        }

        // Find FeatureUpdateRequest associated to success storage responses if any
        Set<FeatureUpdateRequest> updateRequests = this.furRepo.findByGroupIdIn(requestInfoPerGroupId.keySet());
        if (!updateRequests.isEmpty()) {
            // Retrieve features to update associated to storage responses
            List<FeatureEntity> featuresToUpdate = featureRepo.findCompleteByUrnIn(updateRequests.stream()
                                                                                                 .map(r -> r.getUrn())
                                                                                                 .collect(Collectors.toSet()));
            for (FeatureEntity f : featuresToUpdate) {
                // Associate Feature to FeatureUpdaterRequest thanks to feature urn
                Optional<FeatureUpdateRequest> request = updateRequests.stream()
                                                                       .filter(r -> r.getUrn().equals(f.getUrn()))
                                                                       .findFirst();
                if (request.isPresent()) {
                    updateInfos.put(f, requestInfoPerGroupId.get(request.get().getGroupId()));
                }
            }
        }

        // Updates all features files locations from both create and update requests
        List<FeatureEntity> updatedFeatures = featureUpdateService.updateFilesLocations(updateInfos);
        for (FeatureEntity updatedFeature : updatedFeatures) {
            // After update done, if updated feature is associated to an update request we need to set feature toNotify
            // in the request for further notification step.
            Optional<FeatureUpdateRequest> updateRequest = updateRequests.stream()
                                                                         .filter(r -> r.getUrn()
                                                                                       .equals(updatedFeature.getUrn()))
                                                                         .findFirst();
            updateRequest.ifPresent(u -> u.setToNotify(updatedFeature.getFeature()));
        }

        // Handle successful update and create requests
        featureCreationService.handleSuccessfulCreation(creationRequests);
        featureUpdateService.doOnSuccess(updateRequests);
        LOGGER.debug("------------->>> {} features updated from {} storage responses "
                         + "associated to {} creation requests and {} update requests in {} ms",
                     updatedFeatures.size(),
                     requestsInfo.size(),
                     creationRequests.size(),
                     updateRequests.size(),
                     System.currentTimeMillis() - scheduleStart);

    }

    @Override
    public void handleStorageError(Collection<RequestResultInfoDTO> errorRequests) {
        this.featureCreationService.handleStorageError(errorRequests);
        this.featureUpdateService.handleStorageError(errorRequests);
    }

    @Override
    public void handleDeletionSuccess(Set<String> groupIds) {
        featureDeletionService.processStorageRequests(groupIds);
    }

    @Override
    public void handleDeletionError(Collection<RequestResultInfoDTO> errorRequests) {
        Map<String, String> errorByGroupId = Maps.newHashMap();
        errorRequests.forEach(e -> errorByGroupId.put(e.getGroupId(), e.getErrorCause()));
        Set<FeatureDeletionRequest> deletionRequests = fdrRepo.findByGroupIdIn(errorByGroupId.keySet());

        // publish error notification for all deletionRequests id
        deletionRequests.forEach(item -> publisher.publish(FeatureRequestEvent.build(FeatureRequestType.DELETION,
                                                                                     item.getRequestId(),
                                                                                     item.getRequestOwner(),
                                                                                     null,
                                                                                     item.getUrn(),
                                                                                     RequestState.ERROR,
                                                                                     null)));
        // set FeatureDeletionRequest to error state
        deletionRequests.forEach(r -> {
            r.setState(RequestState.ERROR);
            r.addError(String.format("Error during file deletion : %s",
                                     Optional.ofNullable(errorByGroupId.get(r.getGroupId())).orElse("unknown error.")));
        });

        featureDeletionService.doOnError(deletionRequests);
        this.fdrRepo.saveAll(deletionRequests);
    }

    /**
     * Retrieve missing providerId from {@link FeatureRequestDTO}s if available with associated feature.
     *
     * @param requests {@link FeatureRequestDTO}s page
     */
    private void addProviderIdsToRequests(Page<FeatureRequestDTO> requests) {
        List<FeatureUniformResourceName> missingUrns = requests.stream()
                                                               .map(FeatureRequestDTO::getUrn)
                                                               .filter(Objects::nonNull)
                                                               .collect(Collectors.toList());
        if (!missingUrns.isEmpty()) {
            List<IProviderIdByUrn> providerIds = abstractFeatureRequestRepo.findFeatureProviderIdFromRequestUrns(
                missingUrns);
            requests.forEach(r -> {
                providerIds.stream()
                           .filter(i -> i.getUrn().equals(r.getUrn()))
                           .findFirst()
                           .ifPresent(i -> r.setProviderId(i.getProviderId()));
            });
        }
    }

}
