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
package fr.cnes.regards.modules.search.client;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;

/**
 * Complex search client. Handle complex searches on catalog with :
 *  - Multiple search request
 *  - Handling search engine implementations (legacy, opensearch, ...)
 *  - Handling a dataset on which apply search
 *  - Handling search date limit
 *  - Handling includes entity by ids
 *  - Handling excludes entity by ids
 * @author Sébastien Binda
 *
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.complex-search.client")
@RequestMapping(value = IComplexSearchClient.TYPE_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
public interface IComplexSearchClient {

    String TYPE_MAPPING = "/complex/search";

    String SUMMARY_MAPPING = "/summary";

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST, value = IComplexSearchClient.SUMMARY_MAPPING)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestBody ComplexSearchRequest complexSearchRequest);

    /**
     * Compute a complex search
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchDataObjects(
            @RequestBody ComplexSearchRequest complexSearchRequest);

}
