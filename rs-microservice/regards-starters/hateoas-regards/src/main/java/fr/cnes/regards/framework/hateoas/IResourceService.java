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

import org.springframework.cglib.core.Converter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * Hypermedia resource service
 * @author msordi
 */
public interface IResourceService {

    /**
     * Convert object to resource
     * @param <T> element to convert
     * @param object object
     * @return {@link Resource}
     */
    default <T> Resource<T> toResource(T object) {
        Assert.notNull(object, "Object is required");
        return new Resource<>(object);
    }

    /**
     * Utility method to add a build link to the specified {@link ResourceSupport}
     */
    void addLink(ResourceSupport resource, Class<?> controller, String methodName, String rel,
            MethodParam<?>... methodParams);

    /**
     * Build a link for a single method
     * @param controller controller
     * @param methodName method name
     * @param rel rel name
     * @param methodParams method parameters
     */
    Link buildLink(Class<?> controller, String methodName, String rel, MethodParam<?>... methodParams);

    /**
     * Utility method to add a build link with parameters to the specified {@link ResourceSupport}
     */
    <C> void addLinkWithParams(ResourceSupport resource, Class<C> controller, String methodName, String rel,
            MethodParam<?>... methodParams);

    /**
     * Custom way of building link handling request params.
     *
     * For example, an endpoint like getSomething(@RequestParam String name) mapped to: "/something" will generate a
     * link like "http://someting?name=myName"
     *
     * BUT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! It cannot handle type conversion, even if you declared the correct
     * Spring {@link Converter}.
     *
     * For example, an endpoint like getSomething(@RequestParam ComplexEntity entity) mapped to: "/something" will
     * generate a conversion error, telling that it could not find the appropriate converter, even if you defined in
     * your classpath a converter implementing Converter<ComplexEntity, String>
     * @param <C> controller type
     * @param controller controller
     * @param methodName method name
     * @param rel rel name
     * @param methodParams method parameters
     */
    <C> Link buildLinkWithParams(Class<C> controller, String methodName, String rel, MethodParam<?>... methodParams);
}
