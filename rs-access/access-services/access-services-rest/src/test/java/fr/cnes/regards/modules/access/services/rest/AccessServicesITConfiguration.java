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
package fr.cnes.regards.modules.access.services.rest;

import java.util.List;

import org.assertj.core.util.Lists;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class AccessServicesITConfiguration {

    @Bean
    public ICatalogServicesClient catalogServicesClient() {
        ICatalogServicesClient client = Mockito.mock(ICatalogServicesClient.class);

        PluginConfigurationDto dto0 = new PluginConfigurationDto(new PluginConfiguration(),
                Sets.newHashSet(ServiceScope.ONE, ServiceScope.MANY),
                Sets.newHashSet(EntityType.DATA, EntityType.DATASET));
        PluginConfigurationDto dto1 = new PluginConfigurationDto(new PluginConfiguration(),
                Sets.newHashSet(ServiceScope.MANY), Sets.newHashSet(EntityType.DATASET));

        Mockito.when(client.retrieveServices("datasetFromConfigClass", ServiceScope.MANY))
                .thenReturn(new ResponseEntity<List<Resource<PluginConfigurationDto>>>(
                        HateoasUtils.wrapList(Lists.newArrayList(dto0, dto1)), HttpStatus.OK));

        return client;
    }

}
