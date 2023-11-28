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

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.filecatalog.dto.request.RequestResultInfoDto;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

/**
 * @author kevin
 * @author Sébastien Binda
 */
public interface IFeatureRequestService {

    /**
     * Retrieve {@link FeatureRequestDTO}s for given {@link FeatureRequestTypeEnum}
     *
     * @param type    {@link FeatureRequestTypeEnum}
     * @param filters {@link SearchFeatureRequestParameters}
     * @return {@link FeatureRequestDTO}s
     */
    RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type,
                                            SearchFeatureRequestParameters filters,
                                            Pageable page);

    /**
     * Set the status STORAGE_OK to all {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param requestsInfo a list of {@link RequestResultInfoDto} received from storage
     */
    void handleStorageSuccess(Set<RequestResultInfoDto> requestsInfo);

    /**
     * Set the status STORAGE_ERROR to the {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param errorRequests request errors {@link RequestResultInfoDto}
     */
    void handleStorageError(Collection<RequestResultInfoDto> errorRequests);

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
     */
    void handleDeletionError(Collection<RequestResultInfoDto> errorRequests);

    /**
     * Delete requests with given selection
     */
    RequestHandledResponse delete(FeatureRequestTypeEnum type, SearchFeatureRequestParameters selection);

    /**
     * Retry requests with given selection
     *
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     */
    RequestHandledResponse retry(FeatureRequestTypeEnum type, SearchFeatureRequestParameters selection);

    /**
     * Update status and request step of given requests
     */
    public void updateRequestStateAndStep(Set<Long> requestIds, RequestState status, FeatureRequestStep requestStep);
}