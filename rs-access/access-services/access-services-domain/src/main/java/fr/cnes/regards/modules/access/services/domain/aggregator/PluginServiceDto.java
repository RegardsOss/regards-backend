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
package fr.cnes.regards.modules.access.services.domain.aggregator;

import java.util.Set;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

/**
 * DTO class representing either a {@link PluginConfigurationDto}, either a {@link UIPluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public final class PluginServiceDto {

    private final String configId;

    private final String label;

    private final String iconUrl;

    private final Set<ServiceScope> applicationModes;

    private final Set<EntityType> entityTypes;

    private final PluginServiceType type;

    /**
     * Constructor. It is private in order to force the caller to use one of the 'from' builder methods
     * @param configId
     * @param label
     * @param iconUrl
     * @param applicationModes
     * @param entityTypes
     * @param type
     */
    private PluginServiceDto(String configId, String label, String iconUrl, Set<ServiceScope> applicationModes,
            Set<EntityType> entityTypes, PluginServiceType type) {
        super();
        Assert.notNull(configId, "Plugin configuration is mandatory to create a PluginServiceDTO");
        this.configId = configId;
        this.label = label;
        this.iconUrl = iconUrl;
        this.applicationModes = applicationModes;
        this.entityTypes = entityTypes;
        this.type = type;
    }

    /**
     * Build a new instance from the given {@link PluginConfigurationDto}
     * @param pluginConfigurationDto
     * @return the new instance
     */
    public static final PluginServiceDto fromPluginConfigurationDto(PluginConfigurationDto pluginConfigurationDto) {
        // Retrieve applicationModes & entityTypes from Dto
        Set<ServiceScope> appModes = pluginConfigurationDto.getApplicationModes();
        Set<EntityType> entTypes = pluginConfigurationDto.getEntityTypes();

        String iconUrl = null;
        if (pluginConfigurationDto.getIconUrl() != null) {
            iconUrl = pluginConfigurationDto.getIconUrl().toString();
        }

        return new PluginServiceDto(pluginConfigurationDto.getBusinessId(), pluginConfigurationDto.getLabel(), iconUrl,
                appModes, entTypes, PluginServiceType.CATALOG);
    }

    /**
     * Build a new instance from the given {@link UIPluginConfiguration}
     * @param uiPluginConfiguration
     * @return the new instance
     */
    public static final PluginServiceDto fromUIPluginConfiguration(UIPluginConfiguration uiPluginConfiguration) {
        return new PluginServiceDto(String.valueOf(uiPluginConfiguration.getId()), uiPluginConfiguration.getLabel(),
                uiPluginConfiguration.getPluginDefinition().getIconUrl(),
                uiPluginConfiguration.getPluginDefinition().getApplicationModes(),
                uiPluginConfiguration.getPluginDefinition().getEntityTypes(), PluginServiceType.UI);
    }

    /**
     * @return the configId
     */
    public String getConfigId() {
        return configId;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the iconUrl
     */
    public String getIconUrl() {
        return iconUrl;
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

    /**
     * @return the type
     */
    public PluginServiceType getType() {
        return type;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof PluginServiceDto) {
            PluginServiceDto ps = (PluginServiceDto) arg0;
            return this.getConfigId().equals(ps.getConfigId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getConfigId().hashCode();
    }

}
