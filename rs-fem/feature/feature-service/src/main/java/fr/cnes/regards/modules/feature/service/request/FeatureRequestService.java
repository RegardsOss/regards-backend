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
package fr.cnes.regards.modules.feature.service.request;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.IProviderIdByUrn;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureCopyService;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
import fr.cnes.regards.modules.feature.service.IFeatureDeletionService;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.feature.service.IFeatureUpdateService;
import fr.cnes.regards.modules.feature.service.dump.IFeatureMetadataService;

/**
 *
 * @author kevin
 *
 */
@Service
@MultitenantTransactional
public class FeatureRequestService implements IFeatureRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureRequestService.class);

    @Autowired
    private IFeatureCreationRequestRepository fcrRepo;

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

    @Override
    public RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection,
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
    public void delete(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection) {
        switch (type) {
            case COPY:
                featureCopyService.deleteRequests(selection);
                break;
            case CREATION:
                featureCreationService.deleteRequests(selection);
                break;
            case DELETION:
                featureDeletionService.deleteRequests(selection);
                break;
            case NOTIFICATION:
                featureNotificationService.deleteRequests(selection);
                break;
            case SAVE_METADATA:
                featureMetadataService.deleteRequests(selection);
                break;
            case UPDATE:
                featureUpdateService.deleteRequests(selection);
                break;
            default:
                LOGGER.error("Not available type {} for Feature Requests", type.toString());
                break;
        }
    }

    @Override
    public void retry(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection) {
        switch (type) {
            case COPY:
                featureCopyService.retryRequests(selection);
                break;
            case CREATION:
                featureCreationService.retryRequests(selection);
                break;
            case DELETION:
                featureDeletionService.retryRequests(selection);
                break;
            case NOTIFICATION:
                featureNotificationService.retryRequests(selection);
                break;
            case SAVE_METADATA:
                featureMetadataService.retryRequests(selection);
                break;
            case UPDATE:
                featureUpdateService.retryRequests(selection);
                break;
            default:
                LOGGER.error("Not available type {} for Feature Requests", type.toString());
                break;
        }
    }

    @Override
    public void handleStorageSuccess(Set<String> groupIds) {
        Set<FeatureCreationRequest> request = this.fcrRepo.findByGroupIdIn(groupIds);

        featureCreationService.handleSuccessfulCreation(request);
    }

    @Override
    public void handleStorageError(Set<String> groupIds) {
        Set<FeatureCreationRequest> request = this.fcrRepo.findByGroupIdIn(groupIds);

        // publish error notification for all request id
        request.forEach(item -> publisher.publish(FeatureRequestEvent
                .build(FeatureRequestType.CREATION, item.getRequestId(), item.getRequestOwner(),
                       item.getFeature() != null ? item.getFeature().getId() : null, null, RequestState.ERROR, null)));
        // set FeatureCreationRequest to error state
        request.forEach(item -> item.setState(RequestState.ERROR));
        request.forEach(item -> item.setStep(FeatureRequestStep.REMOTE_STORAGE_ERROR));

        this.fcrRepo.saveAll(request);

    }

    @Override
    public void handleDeletionSuccess(Set<String> groupIds) {
        featureDeletionService.processStorageRequests(groupIds);
    }

    @Override
    public void handleDeletionError(Set<String> groupIds) {
        Set<FeatureDeletionRequest> request = this.fdrRepo.findByGroupIdIn(groupIds);

        // publish success notification for all request id
        request.forEach(item -> publisher.publish(FeatureRequestEvent
                .build(FeatureRequestType.DELETION, item.getRequestId(), item.getRequestOwner(), null, item.getUrn(),
                       RequestState.ERROR, null)));
        // set FeatureDeletionRequest to error state
        request.forEach(item -> item.setState(RequestState.ERROR));

        this.fdrRepo.saveAll(request);
    }

    /**
     * Retrieve missing providerId from {@link FeatureRequestDTO}s if available with associated feature.
     *
     * @param requests {@link FeatureRequestDTO}s page
     */
    private void addProviderIdsToRequests(Page<FeatureRequestDTO> requests) {
        List<FeatureUniformResourceName> missingUrns = requests.stream().map(r -> r.getUrn()).filter(r -> r != null)
                .collect(Collectors.toList());
        if (!missingUrns.isEmpty()) {
            List<IProviderIdByUrn> providerIds = abstractFeatureRequestRepo
                    .findFeatureProviderIdFromRequestUrns(missingUrns);
            requests.forEach(r -> {
                providerIds.stream().filter(i -> i.getUrn().equals(r.getUrn())).findFirst()
                        .ifPresent(i -> r.setProviderId(i.getProviderId()));
            });
        }
    }

}
