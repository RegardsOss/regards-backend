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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.GeometryCriterionBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business search service
 *
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
public class BusinessSearchService implements IBusinessSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessSearchService.class);

    /**
     * Catalog search service (entity level search service)
     */
    protected ICatalogSearchService catalogSearchService;

    public BusinessSearchService(ICatalogSearchService searchService) {
        this.catalogSearchService = searchService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends EntityFeature> FacetPage<F> search(ICriterion criterion,
                                                         SearchType searchType,
                                                         List<String> facets,
                                                         Pageable pageable)
        throws SearchException, OpenSearchUnknownParameter {
        FacetPage<AbstractEntity<?>> facetPage = catalogSearchService.search(criterion, searchType, facets, pageable);

        // Extract feature(s) from entity(ies)
        List<F> features = new ArrayList<>();
        facetPage.getContent().forEach(entity -> features.add((F) entity.getFeature()));

        // Build facet page with features
        return new FacetPage<>(features, facetPage.getFacets(), facetPage.getPageable(), facetPage.getTotalElements());
    }

    @Override
    public Boolean isValidGeometry(String wktGeometry) {

        try {
            GeometryCriterionBuilder.build(wktGeometry);
            return true;
        } catch (InvalidGeometryException e) {
            LOGGER.debug(String.format("Invalid geometry : %s", e.getMessage()), e);
            return false;
        }
    }

    @Override
    public <F extends EntityFeature> F get(UniformResourceName urn)
        throws EntityOperationForbiddenException, EntityNotFoundException {
        AbstractEntity<F> entity = catalogSearchService.get(urn);
        return entity.getFeature();
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                                  SearchType searchType,
                                                  UniformResourceName dataset,
                                                  List<DataType> dataTypes) {
        // Just delegate to entity search service
        return catalogSearchService.computeDatasetsSummary(criterion, searchType, dataset, dataTypes);
    }

    @Override
    public List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                         SearchType searchType,
                                                         String propertyPath,
                                                         int maxCount,
                                                         String partialText) {
        // Just delegate to entity search service
        return catalogSearchService.retrieveEnumeratedPropertyValues(criterion,
                                                                     searchType,
                                                                     propertyPath,
                                                                     maxCount,
                                                                     partialText);
    }

}
