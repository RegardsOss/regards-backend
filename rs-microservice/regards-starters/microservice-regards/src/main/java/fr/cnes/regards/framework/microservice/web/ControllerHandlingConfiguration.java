/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.microservice.web;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 *
 * Provide our own {@link RequestMappingHandlerMapping} instance to customize controller detection.
 * @author Marc SORDI
 *
 */
public class ControllerHandlingConfiguration implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {

            /**
             * Expects a handler to have a type-level @{@link Controller}
             * annotation.
             */
            @Override
            protected boolean isHandler(Class<?> beanType) {
                return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
            }
        };
    }
}
