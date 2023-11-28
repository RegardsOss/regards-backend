/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;

/**
 * @author Thomas Fache
 **/
class PluginConfigurationTestBuilder {

    public static final String DEFAULT_VERSION = "1.0.0";

    private final PluginConfiguration pluginConfiguration;

    private PluginConfigurationTestBuilder() {
        pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setVersion(DEFAULT_VERSION);
    }

    public static PluginConfigurationTestBuilder aPlugin() {
        return new PluginConfigurationTestBuilder();
    }

    public PluginConfiguration build() {
        return pluginConfiguration;
    }

    public PluginConfigurationTestBuilder identified(String businessId) {
        pluginConfiguration.setBusinessId(businessId);
        return this;
    }

    public PluginConfigurationTestBuilder named(String label) {
        pluginConfiguration.setLabel(label);
        return this;
    }

    public PluginConfigurationTestBuilder inVersion(String version) {
        pluginConfiguration.setVersion(version);
        return this;
    }

    public PluginConfigurationTestBuilder withPluginId(String pluginId) {
        pluginConfiguration.setPluginId(pluginId);
        return this;
    }

    public PluginConfigurationTestBuilder parameterized_by(IPluginParam parameter) {
        pluginConfiguration.getParameters().add(parameter);
        return this;
    }
}
