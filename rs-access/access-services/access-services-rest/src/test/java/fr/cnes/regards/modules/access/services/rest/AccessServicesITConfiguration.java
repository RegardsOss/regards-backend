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

import java.net.URL;
import java.util.List;
import java.util.Set;

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
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * Module-wide configuration for tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class AccessServicesITConfiguration {

    private static final Long ID = 0L;

    private static final String LABEL = "the label";

    private static URL ICON_URL;

    private static final Set<ServiceScope> APPLICATION_MODES = Sets.newHashSet(ServiceScope.MANY);

    private static final Set<EntityType> ENTITY_TYPES = Sets.newHashSet(EntityType.COLLECTION);

    @Bean
    public ICatalogServicesClient catalogServicesClient() {
        ICatalogServicesClient client = Mockito.mock(ICatalogServicesClient.class);

        Mockito.when(client.retrieveServices("datasetFromConfigClass", ServiceScope.MANY))
                .thenReturn(new ResponseEntity<List<Resource<PluginConfigurationDto>>>(
                        HateoasUtils.wrapList(Lists.newArrayList(dummyPluginConfigurationDto())), HttpStatus.OK));

        return client;
    }

    @Bean
    public PluginConfigurationDto dummyPluginConfigurationDto() {
        final PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        PluginConfiguration pluginConfiguration = new PluginConfiguration(metaData, "testConf");

        return new PluginConfigurationDto(pluginConfiguration, APPLICATION_MODES, ENTITY_TYPES);
    }

    @Bean
    public UIPluginConfiguration dummyUiPluginConfiguration() {
        UIPluginConfiguration pluginConfiguration = new UIPluginConfiguration();
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        pluginConfiguration.setId(ID);
        pluginConfiguration.setLabel(LABEL);
        pluginConfiguration.setPluginDefinition(pluginDefinition);

        pluginDefinition.setIconUrl(ICON_URL);
        pluginDefinition.setApplicationModes(APPLICATION_MODES);
        pluginDefinition.setEntityTypes(ENTITY_TYPES);

        return pluginConfiguration;
    }
}
