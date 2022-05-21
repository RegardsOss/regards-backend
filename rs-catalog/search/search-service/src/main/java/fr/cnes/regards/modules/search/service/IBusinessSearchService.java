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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business search service.<br/>
 * Relies on {@link ICatalogSearchService} but only return public features inside each entity.
 *
 * @author Marc Sordi
 */
public interface IBusinessSearchService {

    /**
     * Business search
     *
     * @param criterion  business criterions according to indexed properties
     * @param searchType search type
     * @param facets     list of facet
     * @param pageable   pagination properties
     * @return a facet page of entity feature
     */
    <F extends EntityFeature> FacetPage<F> search(ICriterion criterion,
                                                  SearchType searchType,
                                                  List<String> facets,
                                                  Pageable pageable) throws SearchException, OpenSearchUnknownParameter;

    /**
     * Retrieve a feature by its identifier
     *
     * @param urn feature identifier
     */
    <F extends EntityFeature> F get(UniformResourceName urn)
        throws EntityOperationForbiddenException, EntityNotFoundException;

    /**
     * Compute summary for given request (delegate method to catalog search service)
     *
     * @param criterion  business criterions
     * @param searchType search key
     * @param dataset    restriction to a specified dataset
     * @param dataTypes  file types on which to compute summary
     * @return summary
     */
    DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                           SearchType searchType,
                                           UniformResourceName dataset,
                                           List<DataType> dataTypes) throws SearchException;

    /**
     * Retrieve property values for specified property name (delegate method to catalog search service)
     *
     * @param criterion    business criterions
     * @param searchType   the search type containing the search type and the result type
     * @param propertyPath target propertu
     * @param maxCount     maximum result count
     * @param partialText  text that property should contains (can be null)
     */
    List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                  SearchType searchType,
                                                  String propertyPath,
                                                  int maxCount,
                                                  String partialText)
        throws SearchException, OpenSearchUnknownParameter;
}
