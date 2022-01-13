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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * @author kevin
 * @author Sébastien Binda
 */
public interface IFeatureRequestService {

    /**
     * Retrieve {@link FeatureRequestDTO}s for given {@link FeatureRequestTypeEnum}
     * @param type {@link FeatureRequestTypeEnum}
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page
     * @return {@link FeatureRequestDTO}s
     */
    public RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection,
            Pageable page);

    /**
     * Set the status STORAGE_OK to all {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param requestsInfo a list of {@link RequestResultInfoDTO} received from storage
     */
    void handleStorageSuccess(Set<RequestResultInfoDTO> requestsInfo);

    /**
     * Set the status STORAGE_ERROR to the {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param errorRequests request errors {@link RequestResultInfoDTO}
     */
    void handleStorageError(Collection<RequestResultInfoDTO> errorRequests);

    /**
     * Delete all {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param groupIds a list of group id
     */
    void handleDeletionSuccess(Set<String> groupIds);

    /**
     * Set the status STORAGE_ERROR to the {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param groupId
     */
    void handleDeletionError(Collection<RequestResultInfoDTO> errorRequests);

    /**
     * Delete requests with given selection
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     * @throws EntityOperationForbiddenException
     */
    public RequestHandledResponse delete(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection);

    /**
     * Retry requests with given selection
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     * @throws EntityOperationForbiddenException
     */
    public RequestHandledResponse retry(FeatureRequestTypeEnum type, FeatureRequestsSelectionDTO selection);

    /**
     * Update status of given requests
     * @param requestIds
     * @param status
     */
    public void  updateRequestsStatus(Set<Long> requestIds, RequestState status);
}