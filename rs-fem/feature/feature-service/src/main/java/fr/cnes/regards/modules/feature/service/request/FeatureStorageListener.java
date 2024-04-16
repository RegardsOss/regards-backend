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

import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureCopyService;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageRequestListener;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class offers callbacks from storage events
 *
 * @author kevin
 * @author Sébastien Binda
 */
@Component
@EnableRetry
public class FeatureStorageListener implements IStorageRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStorageListener.class);

    @Autowired
    private IFeatureRequestService featureRequestService;

    @Autowired
    private IFeatureCopyService featureCopyService;

    @Override
    public void onRequestGranted(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onRequestDenied(Set<RequestInfo> requests) {
        // Only groupId is available on request denied
        Collection<RequestResultInfoDto> errorRequests = requests.stream()
                                                                 .map(r -> RequestResultInfoDto.build(r.getGroupId(),
                                                                                                      r.getMessage()))
                                                                 .collect(Collectors.toSet());
        this.featureRequestService.handleStorageError(errorRequests);
    }

    @Override
    public void onCopySuccess(Set<RequestInfo> requests) {
        List<FeatureCopyRequest> copies = new ArrayList<>();
        for (RequestInfo info : requests) {
            for (RequestResultInfoDto result : info.getSuccessRequests()) {
                copies.addAll(result.getRequestOwners()
                                    .stream()
                                    .filter(owner -> FeatureUniformResourceName.isValidUrn(owner))
                                    .map(owner -> FeatureCopyRequest.build(UUID.randomUUID().toString(),
                                                                           owner,
                                                                           OffsetDateTime.now(),
                                                                           FeatureRequestStep.LOCAL_DELAYED,
                                                                           PriorityLevel.NORMAL,
                                                                           FeatureUniformResourceName.fromString(owner),
                                                                           result.getRequestStorePath(),
                                                                           RequestState.GRANTED,
                                                                           result.getRequestChecksum()))
                                    .collect(Collectors.toSet()));
            }
        }
        this.featureCopyService.registerRequests(copies);
    }

    @Override
    public void onCopyError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onAvailable(Set<RequestInfo> requests) {
        // Nothing to do

    }

    @Override
    public void onAvailabilityError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    @Retryable(value = { OptimisticLockingFailureException.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    public void onDeletionSuccess(Set<RequestInfo> requests) {
        this.featureRequestService.handleDeletionSuccess(requests.stream()
                                                                 .map(RequestInfo::getGroupId)
                                                                 .collect(Collectors.toSet()));

    }

    @Override
    public void onDeletionError(Set<RequestInfo> requests) {
        this.featureRequestService.handleDeletionError(requests.stream()
                                                               .flatMap(r -> r.getErrorRequests().stream())
                                                               .collect(Collectors.toSet()));
    }

    @Override
    @Retryable(value = { OptimisticLockingFailureException.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    public void onReferenceSuccess(Set<RequestInfo> requests) {
        this.featureRequestService.handleStorageSuccess(requests.stream()
                                                                .flatMap(r -> r.getSuccessRequests().stream())
                                                                .collect(Collectors.toSet()));
    }

    @Override
    public void onReferenceError(Set<RequestInfo> requests) {
        this.featureRequestService.handleStorageError(requests.stream()
                                                              .flatMap(r -> r.getErrorRequests().stream())
                                                              .collect(Collectors.toSet()));
    }

    @Override
    @Retryable(value = { OptimisticLockingFailureException.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    public void onStoreSuccess(Set<RequestInfo> requests) {
        this.featureRequestService.handleStorageSuccess(requests.stream()
                                                                .flatMap(r -> r.getSuccessRequests().stream())
                                                                .collect(Collectors.toSet()));
    }

    @Override
    public void onStoreError(Set<RequestInfo> requests) {
        this.featureRequestService.handleStorageError(requests.stream()
                                                              .flatMap(r -> r.getErrorRequests().stream())
                                                              .collect(Collectors.toSet()));
    }

    @Recover
    public void recoverOptimisticRetries(Exception e, Set<RequestInfo> requests) {
        LOGGER.error("Too many retries for optimistic lock. Optimistic lock is maybe not the right solution here", e);
    }

}
