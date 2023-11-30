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

import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureDeletionCollection;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.FeatureDeletionJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Service for deleting Features
 *
 * @author Kevin Marchois
 */
public interface IFeatureDeletionService extends IAbstractFeatureService<FeatureDeletionRequest> {

    /**
     * Register delete requests in database for further processing from incoming request events
     */
    RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureDeletionRequestEvent> events);

    /**
     * Process batch of requests during job
     */
    void processRequests(List<FeatureDeletionRequest> requests, FeatureDeletionJob featureDeletionJob);

    /**
     * Process batch of successful storage request
     */
    void processStorageRequests(Set<String> groupIds);

    /**
     * Register {@link FeatureDeletionRequest} from a {@link FeatureDeletionCollection}
     *
     * @return {@link RequestInfo} contain {@link FeatureUniformResourceName} of granted/denied features
     */
    RequestInfo<FeatureUniformResourceName> registerRequests(@Valid FeatureDeletionCollection collection);

    /**
     * Find all {@link FeatureDeletionRequest}s
     *
     * @return {@link FeatureDeletionRequest}s
     */
    Page<FeatureDeletionRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page);

    /**
     * Find requests information
     *
     * @param filters {@link SearchFeatureRequestParameters}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(SearchFeatureRequestParameters filters);

    List<FeatureDeletionRequest> findAllByIds(Iterable ids);
}
