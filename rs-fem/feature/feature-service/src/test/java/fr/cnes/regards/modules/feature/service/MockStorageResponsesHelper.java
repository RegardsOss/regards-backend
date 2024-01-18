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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.assertj.core.util.Lists;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test util class to simulate management of responses from storage microservice.
 *
 * @author Sébastien Binda
 */
@Component
public class MockStorageResponsesHelper {

    private FeatureConfigurationProperties properties;

    private IFeatureRequestService featureRequestService;

    private IFeatureUpdateRequestRepository featureUpdateRequestRepo;

    private IFeatureCreationRequestRepository featureCreationRequestRepo;

    public MockStorageResponsesHelper(FeatureConfigurationProperties properties,
                                      IFeatureCreationRequestRepository featureCreationRequestRepo,
                                      IFeatureUpdateRequestRepository featureUpdateRequestRepo,
                                      IFeatureRequestService featureRequestService) {
        this.properties = properties;
        this.featureRequestService = featureRequestService;
        this.featureUpdateRequestRepo = featureUpdateRequestRepo;
        this.featureCreationRequestRepo = featureCreationRequestRepo;
    }

    /**
     * Mock storage responses success for all feature creation requests in database.
     * Including reference and storage requests.
     */
    public void mockFeatureCreationStorageSuccess() {
        mockStorageResponses(featureCreationRequestRepo, Optional.empty(), null, true);
    }

    /**
     * Mock storage responses success for all feature update requests in database.
     * Including reference and storage requests.
     */
    public void mockFeatureUpdateStorageSuccess() {
        mockStorageResponses(featureUpdateRequestRepo, Optional.empty(), null, true);
    }

    /**
     * Mock storage responses success with some errors for all feature creation request in database.
     * Including reference and storage requests.
     */
    public void mockStorageResponses(IAbstractFeatureRequestRepository repo, int nbSuccess, int nbErrors) {
        if (nbSuccess > 0) {
            mockStorageResponses(repo, Optional.of(nbSuccess), null, true);
        }
        if (nbErrors > 0) {
            mockStorageResponses(repo, Optional.of(nbErrors), null, false);
        }
    }

    /**
     * Mock storage responses success for all nbRequestsToMock first creation request in database.
     * Including reference and storage requests.
     */
    public void mockFeatureCreationStorageSuccess(Optional<Integer> nbRequestsToMock) {
        mockStorageResponses(featureCreationRequestRepo, nbRequestsToMock, null, true);
    }

    /**
     * Mock storage responses success for all creation request in database matching the given groupIds.
     * Including reference and storage requests.
     */
    public void mockFeatureCreationStorageSuccess(Collection<String> groupIds) {
        mockStorageResponses(featureCreationRequestRepo, Optional.empty(), groupIds, true);
    }

    private void mockStorageResponses(IAbstractFeatureRequestRepository repo,
                                      Optional<Integer> nbRequestsToMock,
                                      Collection<String> groupIds,
                                      boolean success) {
        int pageSize = properties.getMaxBulkSize();
        if (nbRequestsToMock.isPresent() && nbRequestsToMock.get() < pageSize) {
            pageSize = nbRequestsToMock.get();
        }
        Pageable pageToRequest = PageRequest.of(0, pageSize);
        Page<AbstractFeatureRequest> fcrPage;
        int handled = 0;
        do {
            if (groupIds != null && !groupIds.isEmpty()) {
                fcrPage = repo.findByStepAndGroupIdIn(FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                      groupIds,
                                                      pageToRequest);
            } else {
                fcrPage = repo.findByStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED, pageToRequest);
            }
            List<AbstractFeatureRequest> requestsToHandle = fcrPage.getContent();
            if (nbRequestsToMock.isPresent() && (handled + fcrPage.getSize()) > nbRequestsToMock.get()) {
                requestsToHandle = requestsToHandle.stream()
                                                   .limit(nbRequestsToMock.get() - handled)
                                                   .collect(Collectors.toList());
            }
            if (success) {
                mockStorageSuccess(requestsToHandle);
            } else {
                mockStorageError(requestsToHandle);
            }
            handled += fcrPage.getSize();
        } while (fcrPage.hasNext() && handled < nbRequestsToMock.orElse(Integer.MAX_VALUE));
    }

    private Set<RequestResultInfoDto> toStorageRequestInfoResponse(FeatureUpdateRequest featureRequest,
                                                                   boolean success) {
        Set<RequestResultInfoDto> requestsInfo = Sets.newHashSet();
        List<StorageMetadata> storages = featureRequest.getMetadata() != null ?
            featureRequest.getMetadata().getStorages() :
            new ArrayList<>();
        featureRequest.getFeature().getFiles().forEach(file -> {
            requestsInfo.addAll(toInfo(file, featureRequest, storages, success));
        });
        return requestsInfo;
    }

    private Set<RequestResultInfoDto> toStorageRequestInfoResponse(FeatureCreationRequest featureRequest,
                                                                   boolean success) {
        Set<RequestResultInfoDto> requestsInfo = Sets.newHashSet();
        List<StorageMetadata> storages = featureRequest.getMetadata().getStorages();
        featureRequest.getFeatureEntity().getFeature().getFiles().forEach(file -> {
            file.getLocations().forEach(location -> {
                requestsInfo.addAll(toInfo(file, featureRequest, storages, success));
            });
        });
        return requestsInfo;
    }

    private Set<RequestResultInfoDto> toInfo(FeatureFile file,
                                             AbstractFeatureRequest featureRequest,
                                             Collection<StorageMetadata> storages,
                                             boolean success) {
        Set<RequestResultInfoDto> requestsInfo = Sets.newHashSet();
        file.getLocations().forEach(location -> {
            FileReferenceMetaInfoDto meta = new FileReferenceMetaInfoDto(file.getAttributes().getChecksum(),
                                                                         file.getAttributes().getAlgorithm(),
                                                                         file.getAttributes().getFilename(),
                                                                         file.getAttributes().getFilesize(),
                                                                         null,
                                                                         null,
                                                                         file.getAttributes().getMimeType().toString(),
                                                                         file.getAttributes().getDataType().toString());
            if (location.getStorage() != null && !location.getStorage().isEmpty()) {
                requestsInfo.add(createStorageResultInfo(success,
                                                         featureRequest.getGroupId(),
                                                         location.getStorage(),
                                                         meta,
                                                         Optional.of(location.getUrl())));
            } else if (storages != null && !storages.isEmpty()) {
                storages.forEach(storage -> {
                    requestsInfo.add(createStorageResultInfo(success,
                                                             featureRequest.getGroupId(),
                                                             storage.getPluginBusinessId(),
                                                             meta,
                                                             Optional.empty()));
                });
            }
        });
        return requestsInfo;
    }

    private RequestResultInfoDto createStorageResultInfo(boolean success,
                                                         String groupId,
                                                         String storage,
                                                         FileReferenceMetaInfoDto meta,
                                                         Optional<String> refUrl) {
        FileLocationDto fl = new FileLocationDto(storage, refUrl.orElse("storage://internal/store/directory"));
        FileReferenceDto fr = new FileReferenceDto(OffsetDateTime.now(), meta, fl, Lists.newArrayList());
        String errorCause = null;
        if (!success) {
            errorCause = "Simulated storage error";
        }
        return RequestResultInfoDto.build(groupId,
                                          meta.getChecksum(),
                                          storage,
                                          null,
                                          Lists.newArrayList(),
                                          fr,
                                          errorCause);
    }

    private void mockStorageSuccess(Collection<AbstractFeatureRequest> featureRequestsPage) {
        // mock rs-storage response success for file storage
        Set<RequestResultInfoDto> requestsInfo = Sets.newHashSet();
        featureRequestsPage.forEach(featureRequest -> {
            if (featureRequest instanceof FeatureCreationRequest) {
                requestsInfo.addAll(this.toStorageRequestInfoResponse((FeatureCreationRequest) featureRequest, true));
            } else if (featureRequest instanceof FeatureUpdateRequest) {
                requestsInfo.addAll(this.toStorageRequestInfoResponse((FeatureUpdateRequest) featureRequest, true));
            }
        });

        // simulate storage response
        featureRequestService.handleStorageSuccess(requestsInfo);
    }

    private void mockStorageError(Collection<AbstractFeatureRequest> featureRequestsPage) {
        // mock rs-storage response success for file storage
        Set<RequestResultInfoDto> requestsInfo = Sets.newHashSet();
        featureRequestsPage.forEach(featureRequest -> {
            if (featureRequest instanceof FeatureCreationRequest) {
                requestsInfo.addAll(toStorageRequestInfoResponse((FeatureCreationRequest) featureRequest, false));
            } else if (featureRequest instanceof FeatureUpdateRequest) {
                requestsInfo.addAll(toStorageRequestInfoResponse((FeatureUpdateRequest) featureRequest, false));
            }
        });

        // simulate storage response
        featureRequestService.handleStorageError(requestsInfo);
    }

}
