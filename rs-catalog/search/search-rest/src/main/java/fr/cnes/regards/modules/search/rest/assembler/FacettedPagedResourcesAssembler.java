/*
 * LICENSE_PLACEHOLDER
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

import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Custom {@link PagedResourcesAssembler}
 *
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResourcesAssembler<T> extends PagedResourcesAssembler<T> {

    /**
     * @param pResolver
     * @param pBaseUri
     */
    public FacettedPagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver pResolver,
            UriComponents pBaseUri) {
        super(pResolver, pBaseUri);
    }

    public FacettedPagedResources<Resource<T>> toResource(FacetPage<T> pFacetPage) {
        PagedResources<Resource<T>> pagedResources = super.toResource(pFacetPage);
        Set<IFacet<?>> facets = pFacetPage.getFacets();
        Collection<Resource<T>> content = pagedResources.getContent();
        PageMetadata metaData = pagedResources.getMetadata();
        Iterable<Link> links = pagedResources.getLinks();
        return new FacettedPagedResources<>(facets, content, metaData, links);
    }

}
