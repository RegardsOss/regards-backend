/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.facetpage;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.web.util.UriComponents;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;

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

    @SuppressWarnings({ "unchecked" })
    public FacettedPagedResources<Resource<T>> toResource(FacetPage<T> pFacetPage) {
        PagedResources<Resource<T>> pagedResources = super.toResource(pFacetPage);
        Set<IFacet<?>> facets = pFacetPage.getFacets();
        Collection<Resource<T>> content = (Collection<Resource<T>>) HateoasUtils
                .unwrapList(Lists.newArrayList(pagedResources.getContent()));
        PageMetadata metaData = pagedResources.getMetadata();
        Iterable<Link> links = pagedResources.getLinks();

        return new FacettedPagedResources<>(facets, content, metaData, links);
    }

}
