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

import java.security.Policy.Parameters;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;

/**
 * @author kevin
 *
 */
public interface IFeatureRequestService {

    /**
     * Retrieve {@link FeatureRequestDTO}s for given {@link FeatureRequestTypeEnum}
     * @param type {@link FeatureRequestTypeEnum}
     * @param parameters FeatureRequestSearchParameters {@link Parameters} search parameters
     * @param page
     * @return {@link FeatureRequestDTO}s
     */
    public RequestsPage<FeatureRequestDTO> findAll(FeatureRequestTypeEnum type,
            FeatureRequestSearchParameters searchParameters, Pageable page);

    /**
     * Set the status STORAGE_OK to all {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param groupIds a list of group id
     */
    void handleStorageSuccess(Set<String> groupIds);

    /**
     * Set the status STORAGE_ERROR to the {@link FeatureEntity} references by
     * group id in the list send in parameter
     *
     * @param groupId
     */
    void handleStorageError(Set<String> groupIds);

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
    void handleDeletionError(Set<String> groupIds);

    /**
     * Delete request with the given id
     * @param requestId
     * @throws EntityOperationForbiddenException
     */
    public void delete(Long requestId) throws EntityOperationForbiddenException;

}