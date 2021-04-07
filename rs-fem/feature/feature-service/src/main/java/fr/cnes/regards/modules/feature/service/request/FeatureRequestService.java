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

import java.util.Set;

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
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
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
    public RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type,
            FeatureRequestSearchParameters searchParameters, Pageable page) {
        Page<FeatureRequestDTO> results = new PageImpl<>(Lists.newArrayList(), page, 0L);
        RequestsInfo info = RequestsInfo.build(0L);
        switch (type) {
            case COPY:
                results = featureCopyService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCopyService.getInfo();
                break;
            case CREATION:
                results = featureCreationService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureCreationService.getInfo();
                break;
            case DELETION:
                results = featureDeletionService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureDeletionService.getInfo();
                break;
            case NOTIFICATION:
                results = featureNotificationService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureNotificationService.getInfo();
                break;
            case SAVE_METADATA:
                results = featureMetadataService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureMetadataService.getInfo();
                break;
            case UPDATE:
                results = featureUpdateService.findRequests(searchParameters, page)
                        .map(fcr -> AbstractFeatureRequest.toDTO(fcr));
                info = featureUpdateService.getInfo();
                break;
            default:
                LOGGER.error("Not available type {} for Feature Requests", type.toString());
                break;
        }

        return new RequestsPage<>(results.getContent(), info, results.getPageable(), results.getTotalElements());
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

}
