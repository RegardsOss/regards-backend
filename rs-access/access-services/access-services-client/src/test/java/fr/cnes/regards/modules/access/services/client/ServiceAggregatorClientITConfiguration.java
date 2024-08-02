/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.client;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.client.cache.CacheableRolesClient;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Module-wide configuration for Integration Tests
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class ServiceAggregatorClientITConfiguration {

    private static Long ID = 0L;

    @Bean
    public CacheableRolesClient cacheableRolesClient(IRolesClient rolesClient) {
        return new CacheableRolesClient(rolesClient);
    }

    @Bean
    public ICatalogServicesClient catalogServicesClient() {
        ICatalogServicesClient client = Mockito.mock(ICatalogServicesClient.class);

        ResponseEntity<List<EntityModel<PluginConfigurationDto>>> result = new ResponseEntity<List<EntityModel<PluginConfigurationDto>>>(
            HateoasUtils.wrapList(Lists.newArrayList(dummyPluginConfigurationDto())),
            HttpStatus.OK);

        Mockito.when(client.retrieveServices(Mockito.anyList(), Mockito.any())).thenReturn(result);

        return client;
    }

    public PluginConfigurationDto dummyPluginConfigurationDto() {
        PluginUtils.setup();
        final PluginMetaData metaData = new PluginMetaData();
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        metaData.setPluginId(SampleServicePlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration pluginConfiguration = new PluginConfiguration("testConf", metaData.getPluginId());
        pluginConfiguration.setMetaDataAndPluginId(metaData);
        pluginConfiguration.setId(ID);
        ID = ID + 1;
        return new PluginConfigurationDto(pluginConfiguration);
    }

}
