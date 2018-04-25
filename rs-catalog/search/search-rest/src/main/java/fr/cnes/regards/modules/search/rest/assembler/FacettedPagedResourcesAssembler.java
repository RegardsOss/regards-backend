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
package fr.cnes.regards.modules.search.rest.assembler;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Custom ResourcesAssembler injecting facets in the resource.
 * Delegates all page concerns to a {@link PagedResourcesAssembler}.
 *
 * @param <T> The type wrapped by the resources
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResourcesAssembler<T extends IIndexable>
        implements ResourceAssembler<Page<T>, PagedResources<Resource<T>>> {

    private final PagedResourcesAssembler<T> delegate;

    /**
     * Constructor
     * @param delegate
     */
    public FacettedPagedResourcesAssembler(PagedResourcesAssembler<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new {@link FacettedPagedResources} by converting the given {@link FacetPage} into a
     * {@link PageMetadata} instance and wrapping the contained elements into {@link Resource} instances.
     * Will add pagination links based on the given the self link.
     *
     * @param facetPage must not be {@literal null}.
     * @return the facetted page of resources
     */
    public FacettedPagedResources<Resource<T>> toResource(FacetPage<T> facetPage) {
        PagedResources<Resource<T>> pagedResources = delegate.toResource(facetPage);
        Set<IFacet<?>> facets = facetPage.getFacets();
        Collection<Resource<T>> content = pagedResources.getContent();
        PageMetadata metaData = pagedResources.getMetadata();
        Iterable<Link> links = pagedResources.getLinks();
        return new FacettedPagedResources<>(facets, content, metaData, links);
    }

    @Override
    public PagedResources<Resource<T>> toResource(Page<T> entity) {
        return delegate.toResource(entity);
    }

}
