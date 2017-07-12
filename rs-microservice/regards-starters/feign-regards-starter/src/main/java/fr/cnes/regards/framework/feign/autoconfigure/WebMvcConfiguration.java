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
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.EnableWebMvcConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcProperties;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 *
 * Class WebMvcConfiguration
 *
 * Update Spring Web Mvc configuration to ignore RequestMapping on FeignClient implementations.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ConditionalOnWebApplication
public class WebMvcConfiguration extends EnableWebMvcConfiguration {

    public WebMvcConfiguration(final ObjectProvider<WebMvcProperties> pMvcPropertiesProvider,
            final ObjectProvider<WebMvcRegistrations> pMvcRegistrationsProvider,
            final ListableBeanFactory pBeanFactory) {
        super(pMvcPropertiesProvider, pMvcRegistrationsProvider, pBeanFactory);
    }

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new ExcludeFeignRequestMappingHandler();
    }
}
