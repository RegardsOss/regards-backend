/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.client.cache.CacheableRolesClient;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import org.assertj.core.util.Lists;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

/**
 * Module-wide configuration for tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class AccessServicesITConfiguration {

    private static final String LABEL = "the label";

    private static final Set<ServiceScope> APPLICATION_MODES = Sets.newHashSet(ServiceScope.MANY);

    private static final Set<EntityType> ENTITY_TYPES = Sets.newHashSet(EntityType.COLLECTION);

    private static Long ID = 0L;

    @Bean
    public CacheableRolesClient cacheableServiceAggregatorClient(IRolesClient rolesClient) {
        return new CacheableRolesClient(rolesClient);
    }

    @Bean
    public ICatalogServicesClient catalogServicesClient() {
        ICatalogServicesClient client = Mockito.mock(ICatalogServicesClient.class);

        Mockito.doReturn(new ResponseEntity<>(HateoasUtils.wrapList(Lists.newArrayList(dummyPluginConfigurationDto())),
                                              HttpStatus.OK))
               .when(client)
               .retrieveServices(Lists.newArrayList("datasetFromConfigClass"), Lists.newArrayList(ServiceScope.MANY));

        return client;
    }

    @Bean
    public PluginConfigurationDto dummyPluginConfigurationDto() {
        PluginUtils.setup();
        PluginMetaData metaData = PluginUtils.createPluginMetaData(SampleServicePlugin.class);
        PluginConfiguration pluginConfiguration = new PluginConfiguration("testConf",
                                                                          SampleServicePlugin.class.getAnnotation(Plugin.class)
                                                                                                   .id());
        pluginConfiguration.setId(ID);
        pluginConfiguration.setMetaDataAndPluginId(metaData);
        ID = ID + 1;
        return new PluginConfigurationDto(pluginConfiguration);
    }

    @Bean
    public UIPluginConfiguration dummyUiPluginConfiguration() {
        UIPluginConfiguration pluginConfiguration = new UIPluginConfiguration();
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        pluginConfiguration.setId(ID);
        pluginConfiguration.setLabel(LABEL);
        pluginConfiguration.setPluginDefinition(pluginDefinition);

        pluginDefinition.setIconUrl("/plop.png");
        pluginDefinition.setApplicationModes(APPLICATION_MODES);
        pluginDefinition.setEntityTypes(ENTITY_TYPES);

        ID = ID + 1;

        return pluginConfiguration;
    }
}
