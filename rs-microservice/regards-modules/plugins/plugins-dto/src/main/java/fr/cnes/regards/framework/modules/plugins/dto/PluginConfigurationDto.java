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
package fr.cnes.regards.framework.modules.plugins.dto;

import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for a plugin configuration
 *
 * @author Thibaud Michaudel
 **/
public class PluginConfigurationDto {

    private PluginMetaData metaData;

    /**
     * Unique identifier of the plugin. This id is the id defined in the "@Plugin" annotation of the plugin
     * implementation class.
     * <b>DO NOT SET @NotNull even if pluginId must not be null. Validation is done between front and back but at
     * creation, pluginId is retrieved from plugin metadata and thus set by back</b>
     */
    private String pluginId;

    /**
     * Label to identify the configuration.
     */
    private String label;

    /**
     * A serialized {@link UUID} for business identifier
     */
    private String businessId;

    /**
     * Version of the plugin configuration. Is set with the plugin version. This attribute is used to check if the saved
     * configuration plugin version differs from the loaded plugin.
     * <b>DO NOT SET @NotNull even if version must not be null. Validation is done between front and back but at
     * creation, version is retrieved from plugin metadata and thus set by back</b>
     */
    private String version;

    /**
     * Priority order of the plugin.
     */
    private Integer priorityOrder = 0;

    /**
     * The plugin configuration is active.
     */
    private Boolean active = true;

    /**
     * Configuration parameters of the plugin
     */
    private Set<IPluginParam> parameters = new HashSet<IPluginParam>();

    /**
     * Icon of the plugin. It must be an URL to a svg file.
     */
    private URL iconUrl;

    public String getPluginId() {
        return pluginId;
    }

    public String getLabel() {
        return label;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getVersion() {
        return version;
    }

    public Integer getPriorityOrder() {
        return priorityOrder;
    }

    public Boolean isActive() {
        return active;
    }

    public URL getIconUrl() {
        return iconUrl;
    }

    public PluginConfigurationDto(String pluginId,
                                  String label,
                                  String businessId,
                                  String version,
                                  Integer priorityOrder,
                                  Boolean active,
                                  URL iconUrl,
                                  Set<IPluginParam> parameters,
                                  PluginMetaData metaData) {
        this.metaData = metaData;
        this.pluginId = pluginId;
        this.label = label;
        this.businessId = businessId;
        this.version = version;
        this.priorityOrder = priorityOrder;
        this.active = active;
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }

        this.iconUrl = iconUrl;
    }

    public PluginMetaData getMetaData() {
        return metaData;
    }

    public Set<IPluginParam> getParameters() {
        return parameters;
    }

    /**
     * Return the {@link IPluginParam} of a specific parameter
     *
     * @param name the parameter to get the value
     * @return {@link IPluginParam} or null
     */
    public IPluginParam getParameter(String name) {
        for (IPluginParam p : parameters) {
            if ((p != null) && p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
