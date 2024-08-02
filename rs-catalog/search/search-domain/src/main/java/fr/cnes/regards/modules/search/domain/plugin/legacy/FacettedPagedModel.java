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
package fr.cnes.regards.modules.search.domain.plugin.legacy;

import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Extend the {@link PagedModel} to add a "facets" field.
 *
 * @param <T> The type of the resources
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedModel<T> extends PagedModel<T> {

    /**
     * The set of facets
     */
    private final Set<IFacet<?>> facets;

    public FacettedPagedModel(Set<IFacet<?>> facets, Collection<T> content, PageMetadata metadata, Link... links) {
        this(facets, content, metadata, Arrays.asList(links));
    }

    public FacettedPagedModel(Set<IFacet<?>> facets,
                              Collection<T> content,
                              PageMetadata metadata,
                              Iterable<Link> pLinks) {
        super(content, metadata, pLinks);
        this.facets = facets;
    }

    public Set<IFacet<?>> getFacets() {
        return facets;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityModel<S>, S> FacettedPagedModel<T> wrap(Iterable<S> content,
                                                                           PageMetadata metadata,
                                                                           Set<IFacet<?>> facets) {
        ArrayList<T> resources = new ArrayList<T>();

        if (content != null) {
            for (S element : content) {
                if (element != null) {
                    resources.add((T) EntityModel.of(element));
                }
            }
        }

        return new FacettedPagedModel<T>(facets, resources, metadata);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (facets == null ? 0 : facets.hashCode());
        return result;
    }

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
        FacettedPagedModel<?> other = (FacettedPagedModel<?>) obj;
        if (facets == null) {
            if (other.facets != null) {
                return false;
            }
        } else if (!facets.equals(other.facets)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("FacettedPagedResources { content: %s, metadata: %s, links: %s, facets: %s }",
                             getContent(),
                             getMetadata(),
                             getLinks(),
                             getFacets());
    }
}
