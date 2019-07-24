/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * General utility methods for working with {@link ResponseEntity}s and {@link Resource}s.
 * @author xbrochar
 */
public final class HateoasUtils {

    /**
     * Utility classes must not have public or default constructor
     */
    private HateoasUtils() {

    }

    /**
     * Wraps an object in a {@link Resource}.
     * @param toWrap The resource to wrap
     * @param links The resource's links
     * @param <T> The resource type
     * @return The wrap resource
     */
    public static <T> Resource<T> wrap(T toWrap, Link... links) {
        return new Resource<>(toWrap, links);
    }

    /**
     * Wraps an list of objects in a list {@link Resource}s.
     * @param toWrap The resource to wrap
     * @param <T> The resource type
     * @return The wrap resource
     */
    public static <T> List<Resource<T>> wrapList(List<T> toWrap) {
        List<Resource<T>> asResources = new ArrayList<>();
        for (T item : toWrap) {
            asResources.add(new Resource<>(item));
        }
        return asResources;
    }

    /**
     * Wraps a collection of objects in a collection of {@link Resource}s.
     * @param toWrap The resource to wrap
     * @param <T> The resource type
     * @return The wrap resource
     */
    public static <T> Collection<Resource<T>> wrapCollection(Collection<T> toWrap) {
        Collection<Resource<T>> asResources = new ArrayList<>();
        for (T item : toWrap) {
            asResources.add(new Resource<>(item));
        }
        return asResources;
    }

    /**
     * Unwraps a {@link Resource}.
     * @param wrapped The wrapped resource
     * @param <T> The wrapped resource type
     * @return The unwrapped resource
     */
    public static <T> T unwrap(Resource<T> wrapped) {
        return wrapped.getContent();
    }

    /**
     * Unwraps a {@link List} of {@link Resource}s.
     * @param wrapped A list of resources
     * @param <T> The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapList(List<Resource<T>> wrapped) {
        List<T> result = new ArrayList<>();
        for (Resource<T> r : wrapped) {
            result.add(r.getContent());
        }
        return result;
    }

    /**
     * Unwraps a {@link Collection} of {@link Resource}s.
     * @param wrapped A collection of resources
     * @param <T> The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapCollection(Collection<Resource<T>> wrapped) {
        return unwrapList(new ArrayList<>(wrapped));
    }

    /**
     * Transforms a collection to a paged resources of resource(without links) of one page with all the elements.
     * @param elements elements to wrap
     * @return PagedResources<Resource                                                               <                                                               ?>> of one page containing all base elements
     */
    public static <T> PagedResources<Resource<T>> wrapToPagedResources(Collection<T> elements) {
        List<Resource<T>> elementResources = elements.stream().map(Resource<T>::new).collect(Collectors.toList());
        return new PagedResources<>(elementResources, new PageMetadata(elements.size(), 0, elements.size()));
    }

    /**
     * Retrieve all elements from a hateoas paginated endpoint
     * @param pageSize number of elements to retrieve by page
     * @param request request to execute for each page
     * @return {@link List} of results
     */
    public static <T> List<T> retrieveAllPages(int pageSize,
            Function<Pageable, ResponseEntity<PagedResources<Resource<T>>>> request) {
        List<T> results = new ArrayList<>();
        List<Resource<T>> pageResources = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, pageSize);
        boolean newPage;
        do {
            ResponseEntity<PagedResources<Resource<T>>> response = request.apply(pageable);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                PagedResources<Resource<T>> page = response.getBody();
                pageResources.clear();
                pageResources.addAll(page.getContent());
                results.addAll(HateoasUtils.unwrapList(pageResources));
                if (results.size() < page.getMetadata().getTotalElements()) {
                    pageable = pageable.next();
                    newPage = true;
                } else {
                    newPage = false;
                }
            } else {
                newPage = false;
            }
        } while (newPage);
        return results;
    }

}
