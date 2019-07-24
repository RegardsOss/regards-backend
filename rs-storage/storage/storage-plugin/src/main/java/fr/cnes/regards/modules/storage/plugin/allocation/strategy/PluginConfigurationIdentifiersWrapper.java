/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * List<Long> wrapper for PluginParameter of {@link AIPMiscAllocationStrategyPlugin}
 * @author Sébastien Binda
 */
public class PluginConfigurationIdentifiersWrapper {

    @PluginParameter(label = "Plugin configuration indentifiers")
    private List<Long> pluginConfIdentifiers = new ArrayList<>();

    public PluginConfigurationIdentifiersWrapper(List<Long> pluginConfIdentifiers) {
        super();
        this.pluginConfIdentifiers = pluginConfIdentifiers;
    }

    public void setPluginConfIdentifiers(List<Long> pluginConfIdentifiers) {
        this.pluginConfIdentifiers = pluginConfIdentifiers;
    }

    public List<Long> getPluginConfIdentifiers() {
        return pluginConfIdentifiers;
    }
}
