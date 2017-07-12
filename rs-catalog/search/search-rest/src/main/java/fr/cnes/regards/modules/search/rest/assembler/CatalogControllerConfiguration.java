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
package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class CatalogControllerConfiguration {

    @Autowired
    HateoasPageableHandlerMethodArgumentResolver pageableResolver;

    /**
     * Custom resources assembler for pages of {@link AbstractEntity} with facets
     * @return the bean
     */
    @Bean
    public FacettedPagedResourcesAssembler<AbstractEntity> abstractEntityResourcesAssembler() {
        return new FacettedPagedResourcesAssembler<>(pageableResolver, null);
    }

    /**
     * Custom resources assembler for pages of {@link DataObject} with facets
     * @return the bean
     */
    @Bean
    public FacettedPagedResourcesAssembler<DataObject> dataobjectResourcesAssembler() {
        return new FacettedPagedResourcesAssembler<>(pageableResolver, null);
    }

}
