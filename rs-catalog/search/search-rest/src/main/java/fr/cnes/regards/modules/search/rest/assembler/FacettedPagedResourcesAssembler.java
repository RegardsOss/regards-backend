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

import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.web.util.UriComponents;

import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.facet.FacetPage;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Custom {@link PagedResourcesAssembler}
 *
 * @param <T> The type wrapped by the resources
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResourcesAssembler<T extends IIndexable> extends PagedResourcesAssembler<T> {

    /**
     * Constructor
     *
     * @param pResolver
     * @param pBaseUri
     */
    public FacettedPagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver pResolver,
            UriComponents pBaseUri) {
        super(pResolver, pBaseUri);
    }

    /**
     * Creates a new {@link FacettedPagedResources} by converting the given {@link FacetPage} into a {@link PageMetadata} instance and wrapping the contained elements into {@link Resource} instances.
     * Will add pagination links based on the given the self link.
     *
     * @param pFacetPage must not be {@literal null}.
     * @return the facetted page of resources
     */
    public FacettedPagedResources<Resource<T>> toResource(FacetPage<T> pFacetPage) {
        PagedResources<Resource<T>> pagedResources = super.toResource(pFacetPage);
        Set<IFacet<?>> facets = pFacetPage.getFacets();
        Collection<Resource<T>> content = pagedResources.getContent();
        PageMetadata metaData = pagedResources.getMetadata();
        Iterable<Link> links = pagedResources.getLinks();
        return new FacettedPagedResources<>(facets, content, metaData, links);
    }

}
