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
package fr.cnes.regards.modules.feature.service.request;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.*;
import fr.cnes.regards.modules.feature.service.dump.IFeatureMetadataService;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
    public FeatureFilesService featureFilesService;

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
                                                   SearchFeatureRequestParameters filters,
                                                   Pageable page) {
        Page<FeatureRequestDTO> results = new PageImpl<>(Lists.newArrayList(), page, 0L);
        Pageable updatedPage = handlePaginationEmbeddedAttributes(page, type);
        RequestsInfo info = RequestsInfo.build(0L);
        switch (type) {
            case COPY:
                results = featureCopyService.findRequests(filters, updatedPage)
                                            .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCopyService.getInfo(filters);
                break;
            case CREATION:
                results = featureCreationService.findRequests(filters, updatedPage)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCreationService.getInfo(filters);
                break;
            case DELETION:
                results = featureDeletionService.findRequests(filters, updatedPage)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureDeletionService.getInfo(filters);
                break;
            case NOTIFICATION:
                results = featureNotificationService.findRequests(filters, updatedPage)
                                                    .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureNotificationService.getInfo(filters);
                break;
            case SAVE_METADATA:
                results = featureMetadataService.findRequests(filters, updatedPage)
                                                .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureMetadataService.getInfo(filters);
                break;
            case UPDATE:
                results = featureUpdateService.findRequests(filters, updatedPage)
                                              .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureUpdateService.getInfo(filters);
                break;
            default:
                LOGGER.error("Not available type {} for Feature Requests", type);
                break;
        }
        addProviderIdsToRequests(results);

        return new RequestsPage<>(results.getContent(), info, results.getPageable(), results.getTotalElements());
    }

    @Override
    public RequestHandledResponse delete(FeatureRequestTypeEnum type, SearchFeatureRequestParameters selection) {
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
                String message = String.format("Not available type %s for Feature Requests", type);
                LOGGER.error(message);
                return RequestHandledResponse.build(0, 0, message);
        }
    }

    @Override
    public RequestHandledResponse retry(FeatureRequestTypeEnum type, SearchFeatureRequestParameters selection) {
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
                String message = String.format("Not available type %s for Feature Requests", type);
                LOGGER.error(message);
                return RequestHandledResponse.build(0, 0, message);
        }
    }

    @Override
    public void updateRequestStateAndStep(Set<Long> requestIds, RequestState status, FeatureRequestStep requestStep) {
        this.abstractFeatureRequestRepo.updateStateAndStep(status, requestStep, requestIds);
    }

    @Override
    public void handleStorageSuccess(Set<RequestResultInfoDto> requestsInfo) {
        long scheduleStart = System.currentTimeMillis();
        LOGGER.trace("Handling {} storage success responses from storage", requestsInfo.size());
        List<FeatureEntity> updatedFeatures = new ArrayList<>();
        Map<String, List<RequestResultInfoDto>> requestInfoPerGroupId = requestsInfo.stream()
                                                                                    .collect(Collectors.groupingBy(
                                                                                        RequestResultInfoDto::getGroupId));

        // Find FeatureCreationRequest associated to success storage responses if any
        Set<FeatureCreationRequest> creationRequests = this.fcrRepo.findByGroupIdIn(requestInfoPerGroupId.keySet());
        // For each creation requests update files locations
        for (FeatureCreationRequest r : creationRequests) {
            updatedFeatures.add(featureFilesService.updateFeatureLocations(r.getFeatureEntity(),
                                                                           requestInfoPerGroupId.get(r.getGroupId()),
                                                                           r.getFeatureEntity()
                                                                            .getFeature()
                                                                            .getFiles()));
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
                    // For each update requests update files locations
                    updatedFeatures.add(featureFilesService.updateFeatureLocations(f,
                                                                                   requestInfoPerGroupId.get(request.get()
                                                                                                                    .getGroupId()),
                                                                                   request.get()
                                                                                          .getFeature()
                                                                                          .getFiles()));
                }
            }
        }

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
    public void handleStorageError(Collection<RequestResultInfoDto> errorRequests) {
        this.featureCreationService.handleStorageError(errorRequests);
        this.featureUpdateService.handleStorageError(errorRequests);
    }

    @Override
    public void handleDeletionSuccess(Set<String> groupIds) {
        featureDeletionService.processStorageRequests(groupIds);
    }

    @Override
    public void handleDeletionError(Collection<RequestResultInfoDto> errorRequests) {
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
            r.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR);
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

    /**
     * Perform transformation in Sort object to transform attributes from dto {@link FeatureRequestDTO}
     * to entity {@link AbstractRequest}
     */
    private Pageable handlePaginationEmbeddedAttributes(Pageable page, FeatureRequestTypeEnum type) {
        Pageable newPage = page;
        if (page != null && page.getSort() != null) {
            Sort newSort = Sort.by(page.getSort()
                                       .stream()
                                       .map(s -> mapDtoAttributeSortOrderToEntitySortOrder(s, type))
                                       .filter(s -> s != null)
                                       .toList());
            newPage = PageRequest.of(page.getPageNumber(), page.getPageSize(), newSort);
        }
        return newPage;
    }

    /**
     * Map a Sort.Order from a dto {@link FeatureRequestDTO} to a new Sort.Order with the associated entity
     * {@link AbstractRequest} attribute
     */
    private Sort.Order mapDtoAttributeSortOrderToEntitySortOrder(Sort.Order dtoAttributeSortOrder,
                                                                 FeatureRequestTypeEnum type) {
        String dtoAttributeName = dtoAttributeSortOrder.getProperty();
        switch (dtoAttributeName) {
            case FeatureRequestDTO.PROVIDER_ID_FIELD_NAME -> {
                return AbstractRequest.getProviderIdProperty(type)
                                      .map(property -> dtoAttributeSortOrder.withProperty(property))
                                      .orElse(null);
            }
            case FeatureRequestDTO.SESSION_FIELD_NAME -> {
                return AbstractRequest.getSessionProperty(type)
                                      .map(property -> dtoAttributeSortOrder.withProperty(property))
                                      .orElse(null);
            }
            case FeatureRequestDTO.SOURCE_FIELD_NAME -> {
                return AbstractRequest.getSourceProperty(type)
                                      .map(property -> dtoAttributeSortOrder.withProperty(property))
                                      .orElse(null);
            }
            default -> {
                return dtoAttributeSortOrder;
            }
        }
    }

}
