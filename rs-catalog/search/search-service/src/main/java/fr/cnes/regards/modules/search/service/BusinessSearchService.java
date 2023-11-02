/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectGroup;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.GeometryCriterionBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Service handling the access groups in criterion.
     */
    private final IAccessRightFilter accessRightFilter;

    /**
     * Catalog search service (entity level search service)
     */
    protected ICatalogSearchService catalogSearchService;

    public BusinessSearchService(ICatalogSearchService catalogSearchService, IAccessRightFilter accessRightFilter) {
        this.catalogSearchService = catalogSearchService;
        this.accessRightFilter = accessRightFilter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends EntityFeature> FacetPage<F> search(ICriterion criterion,
                                                         SearchType searchType,
                                                         List<String> facets,
                                                         Pageable pageable)
        throws SearchException, OpenSearchUnknownParameter {
        FacetPage<AbstractEntity<?>> facetPage = catalogSearchService.search(criterion, searchType, facets, pageable);

        // Extract feature(s) and metadata from entity(ies)
        List<F> features = facetPage.getContent().stream().peek(entity -> {
            if (searchType.equals(SearchType.DATASETS) || searchType.equals(SearchType.DATAOBJECTS_RETURN_DATASETS)) {
                Dataset dataset = (Dataset) entity;
                try {
                    // Check dataset access rights for data object and for data files access
                    dataset.getFeature().setDataObjectsFilesAccessGranted(isContentAccessGranted(dataset));
                    dataset.getFeature().setDataObjectsAccessGranted(isDatasetDataObjectsAccessGranted(dataset));
                } catch (AccessRightFilterException e) {
                    LOGGER.warn("Unable to calculate user right to order dataset \"{}\"..", entity.getLabel(), e);
                }
            }
        }).map(entity -> (F) entity.getFeature()).collect(Collectors.toList());

        // Build facet page with features
        return new FacetPage<>(features, facetPage.getFacets(), facetPage.getPageable(), facetPage.getTotalElements());
    }

    /**
     * Check if current user has right to access content (files) of given entity.
     * Given entity can be Dataset or DataObject
     *
     * @return either true or false
     */
    @Override
    public boolean isContentAccessGranted(AbstractEntity<?> entity) throws AccessRightFilterException {
        Map<String, DataObjectGroup> datasetObjectsGroupsMap;
        final Set<String> userAccessGroups = accessRightFilter.getUserAccessGroups();
        if (userAccessGroups == null) {
            // access groups is null for admin users. Admin have always access
            return true;
        }
        if (entity instanceof Dataset dataset) {
            datasetObjectsGroupsMap = dataset.getMetadata().getDataObjectsGroups();
            List<DataObjectGroup> dataObjectGroups = userAccessGroups.stream()
                                                                     .filter(datasetObjectsGroupsMap::containsKey)
                                                                     .map(datasetObjectsGroupsMap::get)
                                                                     .toList();
            return dataObjectGroups.stream()
                                   .anyMatch(dataobjectGroup -> dataobjectGroup.getDataObjectAccess()
                                                                && dataobjectGroup.getDataFileAccess());
        } else if (entity instanceof DataObject dataObject) {
            return dataObject.getMetadata()
                             .getGroupsAccessRightsMap()
                             .entrySet()
                             .stream()
                             .anyMatch(entry -> userAccessGroups.contains(entry.getKey()) && entry.getValue());

        }
        return false;
    }

    /**
     * Check if current user has right to access data objects of given dataset.
     * Given entity can be Dataset or DataObject
     *
     * @return either true or false
     */
    public boolean isDatasetDataObjectsAccessGranted(AbstractEntity<?> entity) throws AccessRightFilterException {
        Map<String, DataObjectGroup> datasetObjectsGroupsMap;
        final Set<String> userAccessGroups = accessRightFilter.getUserAccessGroups();
        if (userAccessGroups == null) {
            // access groups is null for admin users. Admin have always access
            return true;
        }
        if (entity instanceof Dataset dataset) {
            datasetObjectsGroupsMap = dataset.getMetadata().getDataObjectsGroups();
            List<DataObjectGroup> dataObjectGroups = userAccessGroups.stream()
                                                                     .filter(datasetObjectsGroupsMap::containsKey)
                                                                     .map(datasetObjectsGroupsMap::get)
                                                                     .toList();
            return dataObjectGroups.stream().anyMatch(DataObjectGroup::getDataObjectAccess);
        }
        return false;
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
