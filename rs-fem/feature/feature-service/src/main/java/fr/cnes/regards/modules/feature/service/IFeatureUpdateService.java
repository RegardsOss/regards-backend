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

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureUpdateCollection;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.FeatureUpdateJob;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This service handles feature update workflow.
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
public interface IFeatureUpdateService extends IAbstractFeatureService<FeatureUpdateRequest> {

    /**
     * Register update requests in database for further processing from incoming request events
     */
    RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureUpdateRequestEvent> events);

    /**
     * Register update requests in database for further processing from feature collection
     */
    RequestInfo<FeatureUniformResourceName> registerRequests(FeatureUpdateCollection toHandle);

    /**
     * Process batch of feature update requests during job {@link FeatureUpdateJob}
     *
     * @return updated features
     */
    Set<FeatureEntity> processRequests(List<FeatureUpdateRequest> requests, FeatureUpdateJob featureUpdateJob);

    /**
     * Find all {@link FeatureUpdateRequest}s
     *
     * @return {@link FeatureUpdateRequest}s
     */
    Page<FeatureUpdateRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page);

    /**
     * Find requests information with search parameters context
     *
     * @param filters {@link SearchFeatureRequestParameters}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(SearchFeatureRequestParameters filters);

    /**
     * Handle storage response errors from storage microservice by looking for {@link FeatureUpdateRequest} associated
     *
     * @param errorRequests error requests results
     */
    void handleStorageError(Collection<RequestResultInfoDto> errorRequests);

    List<FeatureUpdateRequest> findAllByOrderIdsByRequestDateAsc(Set<Long> ids);
}
