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
package fr.cnes.regards.modules.search.rest.engine.plugin.legacy;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

/**
 * Legacy search engine for compatibility with legacy system
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "legacy", author = "REGARDS Team", contact = "regards@c-s.fr", description = "Legacy search engine",
        licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss", version = "1.0.0")
public class LegacySearchEngine implements ISearchEngine<FacettedPagedResources<Resource<AbstractEntity>>, Void> {

    /**
     * Query parameter for facets
     */
    protected static final String FACETS = "facets";

    /**
     * Pagination property
     */
    protected static final String PAGE_NUMBER = "page";

    /**
     * Pagination property
     */
    protected static final String PAGE_SIZE = "size";

    /**
     * Query parser
     */
    @Autowired
    protected IOpenSearchService openSearchService;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    @Override
    public boolean supports(SearchType searchType) {
        // Support all search types
        return true;
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        return openSearchService.parse(queryParams);
    }

    @Override
    public ResponseEntity<FacettedPagedResources<Resource<AbstractEntity>>> search(SearchContext context)
            throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(context.getQueryParams());
        // Extract facets
        List<String> facets = context.getQueryParams().get(FACETS);
        // Do business search
        FacetPage<AbstractEntity> facetPage = searchService.search(criterion, context.getSearchType(), facets,
                                                                   context.getPageable());
        // Build and return HATEOAS response
        return ResponseEntity.ok(toResources(context, facetPage));
    }

    /**
     * Format response with HATEOAS
     */
    private FacettedPagedResources<Resource<AbstractEntity>> toResources(SearchContext context,
            FacetPage<AbstractEntity> facetPage) {

        FacettedPagedResources<Resource<AbstractEntity>> pagedResource = FacettedPagedResources
                .wrap(facetPage.getContent(), new PagedResources.PageMetadata(facetPage.getSize(),
                        facetPage.getNumber(), facetPage.getTotalElements(), facetPage.getTotalPages()),
                      facetPage.getFacets());

        // Adding pagination links
        if (facetPage.hasPrevious()) {
            addPaginationLink(pagedResource, context, LinkRels.PREVIOUS);
        }
        addPaginationLink(pagedResource, context, LinkRels.SELF);
        if (facetPage.hasNext()) {
            addPaginationLink(pagedResource, context, LinkRels.NEXT);
        }

        return pagedResource;
    }

    private void addPaginationLink(ResourceSupport resource, SearchContext context, String rel) {

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
        Link link = SearchEngineController.buildPaginationLink(resourceService, context, rel);
        if (link != null) {
            resource.add(link);
        }
    }
}
