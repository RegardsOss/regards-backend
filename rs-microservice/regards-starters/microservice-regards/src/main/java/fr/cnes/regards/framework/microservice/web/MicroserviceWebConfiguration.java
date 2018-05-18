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
package fr.cnes.regards.framework.microservice.web;

import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * Class MicroserviceWebConfiguration
 *
 * Configuration class for Spring Web Mvc.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceWebConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configurePathMatch(final PathMatchConfigurer pConfigurer) {
        pConfigurer.setUseSuffixPatternMatch(false);
        super.configurePathMatch(pConfigurer);
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer pConfigurer) {
        // Avoid to match uri path extension with a content negociator.
        pConfigurer.favorPathExtension(false);
        super.configureContentNegotiation(pConfigurer);
    }
}
