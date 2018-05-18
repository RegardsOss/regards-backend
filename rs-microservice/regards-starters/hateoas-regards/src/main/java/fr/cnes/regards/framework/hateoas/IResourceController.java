/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.hateoas;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import com.google.common.base.Preconditions;

/**
 * Single resource interface
 * @param <T> data transfer object (whatever object)
 * @author msordi
 */
@FunctionalInterface
public interface IResourceController<T> {

    /**
     * Convert an element to a {@link Resource}
     * @param element element to convert
     * @param extras Extra URL path parameters for links
     * @return a {@link Resource}
     */
    Resource<T> toResource(T element, Object... extras);

    /**
     * Convert a list of elements to a list of {@link Resource}
     * @param elements list of elements to convert
     * @param extras Extra URL path parameters for links
     * @return a list of {@link Resource}
     */
    default List<Resource<T>> toResources(final Collection<T> elements, final Object... extras) {
        Preconditions.checkNotNull(elements);
        return elements.stream().map(resource -> toResource(resource, extras)).collect(Collectors.toList());
    }

    /**
     * Convert a list of elements to a list of {@link Resource}
     * @param elements list of elements to convert
     * @param extras Extra URL path parameters for links
     * @return a list of {@link Resource}
     */
    default PagedResources<Resource<T>> toPagedResources(final Page<T> elements,
            final PagedResourcesAssembler<T> assembler, final Object... extras) {
        Preconditions.checkNotNull(elements);
        final PagedResources<Resource<T>> pageResources = assembler.toResource(elements);
        pageResources.forEach(resource -> resource.add(toResource(resource.getContent(), extras).getLinks()));
        return pageResources;
    }
}
