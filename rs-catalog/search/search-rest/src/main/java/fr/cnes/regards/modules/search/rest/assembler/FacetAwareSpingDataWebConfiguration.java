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
package fr.cnes.regards.modules.search.rest.assembler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Config class to register {@link HandlerMethodArgumentResolver}s for our custom {@link ResourceAssembler}s.
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class FacetAwareSpingDataWebConfiguration extends HateoasAwareSpringDataWebConfiguration {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private DatasetLinkAdder datasetLinkAdder;

    /**
     * Register the {@link FacettedPagedResourcesAssemblerArgumentResolver}
     * @return the bean
     */
    @Bean
    public FacettedPagedResourcesAssemblerArgumentResolver facettedPagedResourcesAssemblerArgumentResolver() {
        return new FacettedPagedResourcesAssemblerArgumentResolver(pageableResolver(), null);
    }

    /**
     * Register the {@link DatasetResourcesAssemblerArgumentResolver}
     * @return the bean
     */
    @Bean
    public DatasetResourcesAssemblerArgumentResolver datasetResourcesAssemblerArgumentResolver() {
        return new DatasetResourcesAssemblerArgumentResolver(resourceService, datasetLinkAdder);
    }

    /**
     * Register the {@link DatasetResourcesAssemblerArgumentResolver}
     * @return the bean
     */
    @Bean
    public PagedDatasetResourcesAssemblerArgumentResolver pagedDatasetResourcesAssemblerArgumentResolver() {
        return new PagedDatasetResourcesAssemblerArgumentResolver(datasetLinkAdder, pageableResolver());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addArgumentResolvers(java.util.List)
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(facettedPagedResourcesAssemblerArgumentResolver());
        argumentResolvers.add(datasetResourcesAssemblerArgumentResolver());
        argumentResolvers.add(pagedDatasetResourcesAssemblerArgumentResolver());
    }

}
