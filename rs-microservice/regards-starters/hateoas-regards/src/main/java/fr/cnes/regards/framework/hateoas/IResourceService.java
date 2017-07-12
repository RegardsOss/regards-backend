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
package fr.cnes.regards.framework.hateoas;

import org.springframework.cglib.core.Converter;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

/**
 *
 * Hypermedia resource service
 *
 * @author msordi
 *
 */
public interface IResourceService {

    /**
     * Convert object to resource
     *
     * @param <T>
     *            element to convert
     * @param pObject
     *            object
     * @return {@link Resource}
     */
    default <T> Resource<T> toResource(T pObject) {
        Assert.notNull(pObject);
        return new Resource<>(pObject);
    }

    /**
     * Add a link to a resource for a single method
     *
     * @param <T>
     *            resource content type
     * @param pResource
     *            resource to manage
     * @param pController
     *            controller
     * @param pMethodName
     *            method name
     * @param pRel
     *            rel name
     * @param pMethodParams
     *            method parameters
     */
    <T> void addLink(Resource<T> pResource, Class<?> pController, String pMethodName, String pRel,
            MethodParam<?>... pMethodParams);

    /**
     * Custom way of adding link to a resource handling request params.
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
     *
     * @param <T>
     *            resource content type
     * @param <C>
     *            controller type
     * @param pResource
     *            resource to manage
     * @param pController
     *            controller
     * @param pMethodName
     *            method name
     * @param pRel
     *            rel name
     * @param pMethodParams
     *            method parameters
     */
    <T, C> void addLinkWithParams(Resource<T> pResource, Class<C> pController, String pMethodName, String pRel,
            MethodParam<?>... pMethodParams);
}
