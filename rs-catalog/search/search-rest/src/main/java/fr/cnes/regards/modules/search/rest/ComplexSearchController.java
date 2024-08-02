/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import fr.cnes.regards.modules.model.gson.helper.AttributeHelper;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.dto.ComplexSearchRequest;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import fr.cnes.regards.modules.search.service.SearchException;
import fr.cnes.regards.modules.search.service.engine.ISearchEngineDispatcher;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Complex search controller. Handle complex searches on catalog with multiple search requests. Each request handles :
 * - search engine implementations (legacy, opensearch, ...)
 * - dataset on which apply search
 * - search date limit
 * - includes entity by ids
 * - excludes entity by ids
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(path = ComplexSearchController.TYPE_MAPPING)
public class ComplexSearchController implements IResourceController<EntityFeature> {

    public static final String TYPE_MAPPING = "/complex/search";

    public static final String SUMMARY_MAPPING = "/summary";

    public static final String SEARCH_DATAOBJECTS_ATTRIBUTES = "/dataobjects/attributes";

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

    @Autowired
    private IAttributeHelper attributeHelper;

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST, value = ComplexSearchController.SUMMARY_MAPPING)
    @ResourceAccess(description = "Provide a summary for a given dataset from user current basket",
                    role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(
        @RequestBody ComplexSearchRequest complexSearchRequest) throws ModuleException {
        List<ICriterion> searchCriterions = Lists.newArrayList();
        for (SearchRequest request : complexSearchRequest.getRequests()) {
            searchCriterions.add(dispatcher.computeComplexCriterion(request));
        }

        List<DataType> dataTypes = complexSearchRequest.getDataTypes();
        if ((dataTypes == null) || dataTypes.isEmpty()) {
            dataTypes = Lists.newArrayList();
            for (DataType type : DataType.values()) {
                dataTypes.add(type);
            }
        }
        DocFilesSummary summary = searchService.computeDatasetsSummary(ICriterion.or(searchCriterions),
                                                                       SearchType.DATAOBJECTS,
                                                                       null,
                                                                       dataTypes);
        return new ResponseEntity<>(summary, HttpStatus.OK);

    }

    /**
     * Compute a complex search
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Get features from a complex search", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<EntityFeature>>> searchDataObjects(
        @RequestBody @Valid ComplexSearchRequest complexSearchRequest,
        @Parameter(hidden = true) PagedResourcesAssembler<EntityFeature> assembler) throws ModuleException {
        List<ICriterion> searchCriterions = Lists.newArrayList();
        for (SearchRequest request : complexSearchRequest.getRequests()) {
            searchCriterions.add(dispatcher.computeComplexCriterion(request));
        }
        FacetPage<EntityFeature> facetPage = searchService.search(ICriterion.or(searchCriterions),
                                                                  SearchType.DATAOBJECTS,
                                                                  null,
                                                                  PageRequest.of(complexSearchRequest.getPage(),
                                                                                 complexSearchRequest.getSize()));
        return new ResponseEntity<>(toPagedResources(facetPage, assembler), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = ComplexSearchController.SEARCH_DATAOBJECTS_ATTRIBUTES)
    @ResourceAccess(description = "Get common model attributes associated to data objects results of the given request",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<Set<AttributeModel>> searchDataobjectsAttributes(@RequestBody SearchRequest searchRequest,
                                                                           @RequestHeader HttpHeaders headers)
        throws SearchException, ModuleException {
        List<String> modelNames = searchService.retrieveEnumeratedPropertyValues(dispatcher.computeComplexCriterion(
            searchRequest), SearchType.DATAOBJECTS, AttributeHelper.MODEL_ATTRIBUTE, 100, null);
        return ResponseEntity.ok(attributeHelper.getAllCommonAttributes(modelNames));
    }

    @Override
    public EntityModel<EntityFeature> toResource(EntityFeature entity, Object... extras) {
        return resourceService.toResource(entity);
    }
}
