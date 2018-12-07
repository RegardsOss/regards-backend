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
package fr.cnes.regards.framework.module.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration class to be sure that a @RequestMapping annotated class is taken into account
 * ONLY if it also annotated @Controller.<br>
 * In fact, this configuration exists because @FeignClient annotated classes are often also annotated @RequestMapping
 * but must not be taken into account as rest controllers (and avoid feign starters dependencies).<br>
 * This should be normal behavior but Spring developpers refuse to change it because of legacy codes.
 * @author Olivier Rousselot
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class) // Needed by WebMvcConfigurationSupport inheritance
public class WebMvcConfiguration extends WebMvcConfigurationSupport {
    @Override
    public RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {

            /**
             * Expects a handler to have a type-level @{@link Controller}
             * annotation AND a type-level @{@link RequestMapping} annotation.
             */
            @Override
            protected boolean isHandler(Class<?> beanType) {
                return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) &&
                        AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
            }
        };
    }
}
