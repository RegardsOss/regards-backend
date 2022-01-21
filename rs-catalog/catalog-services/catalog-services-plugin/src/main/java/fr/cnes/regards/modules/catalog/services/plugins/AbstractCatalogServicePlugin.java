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
package fr.cnes.regards.modules.catalog.services.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Used to set specific parameters to each catalog service plugins.
 * <ul>
 * <li>applyToAllDatasets : Is used by service manager to know if the plugin is associated to all datasets.<br/>
 * See ServiceManager#getServicesAssociatedToAllDatasets() from catalog-services-service
 * </li>
 * </ul>
 *
 * @author Sébastien Binda
 *
 */
public class AbstractCatalogServicePlugin {

    /**
     * Plugin parameter name used to retrieve the parameter value from a plugin configuration
     */
    public static final String APPLY_TO_ALL_DATASETS_PARAM = "applyToAllDatasets";

    /**
     * Plugin parameter to define if the service is automatically appliable to all datasets of the catalog.
     * NOTE : This parameter is private because it is never used in plugins implementation it is only used by the serviceManager
     */
    @PluginParameter(name = AbstractCatalogServicePlugin.APPLY_TO_ALL_DATASETS_PARAM,
            label = "Activate this service for datas of every datasets",
            description = "If this parameter is not true, then you have to configure each dataset to allow access to this service.",
            defaultValue = "false", optional = false)
    private Boolean applyToAllDatasets;

}
