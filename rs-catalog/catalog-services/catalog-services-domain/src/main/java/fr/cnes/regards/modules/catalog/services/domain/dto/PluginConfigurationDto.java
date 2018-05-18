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
package fr.cnes.regards.modules.catalog.services.domain.dto;

import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.annotations.GetCatalogServicePluginAnnotation;

/**
 * Adds the given applicationModes and the entityTypes to a {@link PluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class PluginConfigurationDto extends PluginConfiguration {

    private final Set<ServiceScope> applicationModes;

    private final Set<EntityType> entityTypes;

    /**
     * Finds the application mode of the given plugin configuration
     */
    private static final Function<PluginConfiguration, CatalogServicePlugin> GET_CATALOG_SERVICE_PLUGIN_ANNOTATION = new GetCatalogServicePluginAnnotation();

    /**
     * For a {@link PluginConfiguration}, return its corresponding DTO, in which we have added fields <code>applicationModes</code>
     * and <code>entityTypes</code>
     *
     * @param pPluginConfiguration
     */
    public PluginConfigurationDto(PluginConfiguration pPluginConfiguration) {
        super(pPluginConfiguration);
        applicationModes = Sets
                .newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).applicationModes());
        entityTypes = Sets.newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).entityTypes());
    }

    /**
     * @return the applicationModes
     */
    public Set<ServiceScope> getApplicationModes() {
        return applicationModes;
    }

    /**
     * @return the entityTypes
     */
    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

}
