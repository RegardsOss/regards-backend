/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler.resource;

import java.util.Collection;
import java.util.Set;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;

import fr.cnes.regards.modules.indexer.domain.facet.IFacet;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResources<T> extends PagedResources<T> {

    private final Set<IFacet<?>> facets;

    /**
     * @param pFacets
     * @param pContent
     * @param pMetadata
     * @param pLinks
     */
    public FacettedPagedResources(Set<IFacet<?>> pFacets, Collection<T> pContent, PageMetadata pMetadata,
            Iterable<Link> pLinks) {
        super(pContent, pMetadata, pLinks);
        facets = pFacets;
    }

    /**
     * @return the facets
     */
    public Set<IFacet<?>> getFacets() {
        return facets;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.PagedResources#equals(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object pObj) {

        if (this == pObj) {
            return true;
        }

        if ((pObj == null) || !pObj.getClass().equals(getClass())) {
            return false;
        }

        FacettedPagedResources<T> that = (FacettedPagedResources<T>) pObj;

        return super.equals(pObj) && (this.facets == that.facets);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.PagedResources#hashCode()
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result += this.facets == null ? 0 : 31 * this.facets.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.ResourceSupport#toString()
     */
    @Override
    public String toString() {
        return String.format("FacettedPagedResources { content: %s, metadata: %s, links: %s, facets: %s }",
                             getContent(), getMetadata(), getLinks(), getFacets());
    }
}
