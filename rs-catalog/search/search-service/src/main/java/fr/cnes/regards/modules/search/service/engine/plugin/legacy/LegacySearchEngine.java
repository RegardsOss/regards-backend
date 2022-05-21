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
package fr.cnes.regards.modules.search.service.engine.plugin.legacy;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.*;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Legacy search engine for compatibility with legacy system
 *
 * @author Marc Sordi
 */
@Plugin(id = LegacySearchEngine.PLUGIN_ID, author = "REGARDS Team", contact = "regards@c-s.fr",
    description = "Legacy search engine", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
    version = "1.0.0")
public class LegacySearchEngine implements
    ISearchEngine<FacettedPagedModel<EntityModel<EntityFeature>>, Void, EntityModel<EntityFeature>, List<String>> {

    public static final String PLUGIN_ID = SearchEngineMappings.LEGACY_PLUGIN_ID;

    /**
     * Query parameter for facets
     */
    public static final String FACETS = "facets";

    /**
     * Pagination property
     */
    private static final String PAGE_NUMBER = "page";

    /**
     * Pagination property
     */
    private static final String PAGE_SIZE = "size";

    /**
     * Property values request property : text that property should contains
     */
    public static final String PARTIAL_TEXT = "partialText";

    /**
     * Query parser
     */
    @Autowired
    protected IOpenSearchService openSearchService;

    /**
     * Business search service
     */
    @Autowired
    protected IBusinessSearchService searchService;

    @Autowired
    protected ICatalogSearchService catalogSearchService;

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    @Override
    public boolean supports(SearchType searchType) {
        // Supports all search types
        return true;
    }

    private ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        return openSearchService.parse(queryParams);
    }

    /**
     * Parse request parameters and and add dataset context if necessary
     */
    @Override
    public ICriterion parse(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(context.getQueryParams());
        // Manage dataset URN path parameter as criterion
        if (context.getDatasetUrn().isPresent()) {
            criterion = ICriterion.and(criterion,
                                       ICriterion.eq(StaticProperties.FEATURE_TAGS_PATH,
                                                     context.getDatasetUrn().get().toString(),
                                                     StringMatchType.KEYWORD));
        }
        return criterion;
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> search(SearchContext context,
                                                                                 ISearchEngine<?, ?, ?, ?> parser,
                                                                                 IEntityLinkBuilder linkBuilder)
        throws ModuleException {
        // Convert parameters to business criterion considering dataset
        return doSearch(parser.parse(context), context, linkBuilder);

    }

    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> doSearch(ICriterion criterion,
                                                                                   SearchContext context,
                                                                                   IEntityLinkBuilder linkBuilder)
        throws ModuleException {
        // Extract facets: beware, theorically there should be only one facets parameter with values separated by ","
        // but take all cases into account
        List<String> facets = context.getQueryParams().get(FACETS);
        if (facets != null) {
            facets = facets.stream().flatMap(f -> Arrays.stream(f.split(","))).collect(Collectors.toList());
        }
        // Do business search
        FacetPage<EntityFeature> facetPage = searchService.search(criterion,
                                                                  context.getSearchType(),
                                                                  facets,
                                                                  context.getPageable());
        // Build and return HATEOAS response
        return ResponseEntity.ok(toResources(context, facetPage, linkBuilder));
    }

    /**
     * Format response with HATEOAS
     */
    private FacettedPagedModel<EntityModel<EntityFeature>> toResources(SearchContext context,
                                                                       FacetPage<EntityFeature> facetPage,
                                                                       IEntityLinkBuilder linkBuilder) {

        FacettedPagedModel<EntityModel<EntityFeature>> pagedResource = FacettedPagedModel.wrap(facetPage.getContent(),
                                                                                               new PagedModel.PageMetadata(
                                                                                                   facetPage.getSize(),
                                                                                                   facetPage.getNumber(),
                                                                                                   facetPage.getTotalElements(),
                                                                                                   facetPage.getTotalPages()),
                                                                                               facetPage.getFacets());

        // Add entity links
        for (EntityModel<EntityFeature> resource : pagedResource.getContent()) {
            resource.add(linkBuilder.buildEntityLinks(resourceService,
                                                      context,
                                                      resource.getContent().getEntityType(),
                                                      resource.getContent().getId()));
        }

        // Add pagination links
        if (facetPage.hasPrevious()) {
            addPaginationLink(pagedResource, context, LinkRels.PREVIOUS, linkBuilder);
        }
        addPaginationLink(pagedResource, context, LinkRels.SELF, linkBuilder);
        if (facetPage.hasNext()) {
            addPaginationLink(pagedResource, context, LinkRels.NEXT, linkBuilder);
        }

        return pagedResource;
    }

    private void addPaginationLink(RepresentationModel<?> resource,
                                   SearchContext context,
                                   LinkRelation rel,
                                   IEntityLinkBuilder linkBuilder) {

        int pageNumber;
        if (LinkRels.SELF.equals(rel)) {
            pageNumber = context.getPageable().getPageNumber();
        } else if (LinkRels.PREVIOUS.equals(rel)) {
            pageNumber = context.getPageable().getPageNumber() - 1;
        } else if (LinkRels.NEXT.equals(rel)) {
            pageNumber = context.getPageable().getPageNumber() + 1;
        } else {
            return;
        }

        // Specify pagination properties
        context.getQueryParams().put(PAGE_NUMBER, Arrays.asList(String.valueOf(pageNumber)));
        context.getQueryParams().put(PAGE_SIZE, Arrays.asList(String.valueOf(context.getPageable().getPageSize())));

        // Create link
        Link link = linkBuilder.buildPaginationLink(resourceService, context, rel);
        if (link != null) {
            resource.add(link);
        }
    }

    @Override
    public ResponseEntity<EntityModel<EntityFeature>> getEntity(SearchContext context, IEntityLinkBuilder linkBuilder)
        throws ModuleException {
        // Retrieve entity
        EntityFeature entity = searchService.get(context.getUrn().get());
        // Prepare resource
        EntityModel<EntityFeature> resource = resourceService.toResource(entity);
        resource.add(linkBuilder.buildEntityLinks(resourceService, context, entity.getEntityType(), entity.getId()));
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<String>> getPropertyValues(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Extract optional request parameters
        String partialText = context.getQueryParams().getFirst(PARTIAL_TEXT);
        // Do business search
        List<String> values = searchService.retrieveEnumeratedPropertyValues(criterion,
                                                                             context.getSearchType(),
                                                                             context.getPropertyNames()
                                                                                    .stream()
                                                                                    .findFirst()
                                                                                    .get(),
                                                                             context.getMaxCount().get(),
                                                                             partialText);
        // Build response
        return ResponseEntity.ok(values);
    }

    @Override
    public ResponseEntity<DocFilesSummary> getSummary(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Compute summary
        DocFilesSummary summary = searchService.computeDatasetsSummary(criterion,
                                                                       context.getSearchType(),
                                                                       context.getDatasetUrn().orElse(null),
                                                                       context.getDateTypes().get());
        // Build response
        return ResponseEntity.ok(summary);
    }

    @Override
    public ResponseEntity<List<EntityModel<? extends PropertyBound<?>>>> getPropertiesBounds(SearchContext context)
        throws ModuleException {
        List<PropertyBound<?>> bounds = catalogSearchService.retrievePropertiesBounds(context.getPropertyNames(),
                                                                                      parse(context),
                                                                                      context.getSearchType());
        return ResponseEntity.ok(bounds.stream().map(EntityModel::of).collect(Collectors.toList()));
    }
}
