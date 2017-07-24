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
package fr.cnes.regards.modules.configuration.rest;

import java.util.Collection;
import java.util.Set;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;

import fr.cnes.regards.modules.indexer.domain.facet.IFacet;

/**
 * Extend the {@link PagedResources} to add a "facets" field.
 * @param <T> The type of the resoures
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResources<T> extends PagedResources<T> {

    /**
     * The set of facets
     */
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((facets == null) ? 0 : facets.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FacettedPagedResources<?> other = (FacettedPagedResources<?>) obj;
        if (facets == null) {
            if (other.facets != null) {
                return false;
            }
        } else if (!facets.equals(other.facets)) {
            return false;
        }
        return true;
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
