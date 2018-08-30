/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.engine.ISearchEngineDispatcher;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;

/**
 * Complex search controller. Handle complex searches on catalog with multiple search requests. Each request handles :
 *  - search engine implementations (legacy, opensearch, ...)
 *  - dataset on which apply search
 *  - search date limit
 *  - includes entity by ids
 *  - excludes entity by ids
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(path = ComplexSearchController.TYPE_MAPPING)
public class ComplexSearchController implements IResourceController<EntityFeature> {

    public static final String TYPE_MAPPING = "/complex/search";

    public static final String SUMMARY_MAPPING = "/summary";

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Engine request dispatcher
     */
    @Autowired
    private ISearchEngineDispatcher dispatcher;

    /**
     * Business search service
     */
    @Autowired
    protected IBusinessSearchService searchService;

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, value = ComplexSearchController.SUMMARY_MAPPING)
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(
            @RequestBody ComplexSearchRequest complexSearchRequest) throws ModuleException {
        List<ICriterion> searchCriterions = Lists.newArrayList();
        for (SearchRequest request : complexSearchRequest.getRequests()) {
            searchCriterions.add(computeComplexCriterion(request));
        }

        List<DataType> dataTypes = complexSearchRequest.getDataTypes();
        if ((dataTypes == null) || dataTypes.isEmpty()) {
            dataTypes = Lists.newArrayList();
            for (DataType type : DataType.values()) {
                dataTypes.add(type);
            }
        }
        DocFilesSummary summary = searchService.computeDatasetsSummary(ICriterion.or(searchCriterions),
                                                                       SearchType.DATAOBJECTS, null, dataTypes);
        return new ResponseEntity<>(summary, HttpStatus.OK);

    }

    /**
     * Compute a complex search
     * {@link DataType})
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<PagedResources<Resource<EntityFeature>>> searchDataObjects(
            @RequestBody ComplexSearchRequest complexSearchRequest, PagedResourcesAssembler<EntityFeature> assembler)
            throws ModuleException {
        List<ICriterion> searchCriterions = Lists.newArrayList();
        for (SearchRequest request : complexSearchRequest.getRequests()) {
            searchCriterions.add(computeComplexCriterion(request));
        }
        FacetPage<EntityFeature> facetPage = searchService
                .search(ICriterion.or(searchCriterions), SearchType.DATAOBJECTS, null,
                        new PageRequest(complexSearchRequest.getPage(), complexSearchRequest.getSize()));
        return new ResponseEntity<>(toPagedResources(facetPage, assembler), HttpStatus.OK);
    }

    /**
     * Compute a {@link SearchRequest} to a {@link ICriterion}
     * @throws ModuleException
     */
    private ICriterion computeComplexCriterion(SearchRequest searchRequest) throws ModuleException {
        UniformResourceName datasetUrn = null;
        if (searchRequest.getDatasetUrn() != null) {
            datasetUrn = UniformResourceName.fromString(searchRequest.getDatasetUrn());
        }
        ISearchEngine<?, ?, ?, ?> searchEngine = dispatcher.getSearchEngine(Optional.ofNullable(datasetUrn),
                                                                            searchRequest.getEngineType());

        // Open search request
        SearchContext context = SearchContext.build(SearchType.DATAOBJECTS, searchRequest.getEngineType(),
                                                    SearchEngineMappings.getJsonHeaders(),
                                                    searchRequest.getSearchParameters(), new PageRequest(0, 1));
        if (searchRequest.getDatasetUrn() != null) {
            context = context.withDatasetUrn(UniformResourceName.fromString(searchRequest.getDatasetUrn()));
        }
        ICriterion reqCrit = searchEngine.parse(context);

        // Date criterion
        if (searchRequest.getSearchDateLimit() != null) {
            reqCrit = ICriterion.and(reqCrit,
                                     ICriterion.lt(StaticProperties.CREATION_DATE, searchRequest.getSearchDateLimit()));
        }

        // Include ids criterion
        if ((searchRequest.getEntityIdsToInclude() != null) && !searchRequest.getEntityIdsToInclude().isEmpty()) {
            ICriterion idsCrit = null;
            for (String ipId : searchRequest.getEntityIdsToInclude()) {
                if (idsCrit == null) {
                    idsCrit = ICriterion.eq(StaticProperties.IP_ID, ipId);
                } else {
                    idsCrit = ICriterion.or(idsCrit, ICriterion.eq(StaticProperties.IP_ID, ipId));
                }
            }
            if (idsCrit != null) {
                reqCrit = ICriterion.and(reqCrit, idsCrit);
            }
        }

        // Exclude ids criterion
        if ((searchRequest.getEntityIdsToExclude() != null) && !searchRequest.getEntityIdsToExclude().isEmpty()) {
            for (String ipId : searchRequest.getEntityIdsToExclude()) {
                reqCrit = ICriterion.and(reqCrit, ICriterion.not(ICriterion.eq(StaticProperties.IP_ID, ipId)));
            }
        }

        return reqCrit;
    }

    @Override
    public Resource<EntityFeature> toResource(EntityFeature entity, Object... extras) {
        return resourceService.toResource(entity);
    }
}
