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
package fr.cnes.regards.framework.geojson;

import java.util.ArrayList;
import java.util.List;

import org.springframework.restdocs.payload.FieldDescriptor;

import com.google.common.base.Strings;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;

/**
 * Builds the description of all fields found in {@link PluginConfiguration}.
 * @author Christophe Mertz
 */
public class PluginConfigurationFieldDescriptors {

    private String initPrefix;

    public PluginConfigurationFieldDescriptors() {
        super();
        initPrefix = null;
    }

    public PluginConfigurationFieldDescriptors(String prefix) {
        super();
        this.initPrefix = prefix;
    }

    public List<FieldDescriptor> build() {
        return this.build(false);
    }

    public List<FieldDescriptor> build(boolean update) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields pluginConfField = new ConstrainedFields(PluginConfiguration.class);

        if (update) {
            lfd.add(pluginConfField.withPath(addPrefix("id"), "id", "Unique identifier").type("Long"));
        }
        lfd.add(pluginConfField.withPath(addPrefix("pluginId"), "pluginId", "Plugin configuration identifier")
                        .type("String"));
        lfd.add(pluginConfField.withPath(addPrefix("label"), "label", "A label to identify the configuration")
                        .type("String"));
        lfd.add(pluginConfField.withPath(addPrefix("version"), "version", "The version of the configuration")
                        .type("String"));
        lfd.add(pluginConfField.withPath(addPrefix("priorityOrder"), "priorityOrder",
                                         "The priority order of the configuration").type("Integer"));
        lfd.add(pluginConfField.withPath(addPrefix("active"), "active", "If true, the configuration is active")
                        .type("Boolean"));
        lfd.add(pluginConfField.withPath(addPrefix("pluginClassName"), "pluginClassName", "The plugin class name")
                        .type("String"));
        lfd.add(pluginConfField.withPath(addPrefix("interfaceNames"), "interfaceNames",
                                         "The interfaces that implements the @PluginInterace annotation and implemented by the pluginClassName")
                        .type("Array"));
        lfd.add(pluginConfField.withPath(addPrefix("iconUrl"), "iconUrl", "Icon of the plugin",
                                         "It must be an URL to a svg file").type("URL"));
        lfd.add(pluginConfField
                        .withPath(addPrefix("parameters"), "parameters", "The parameters configuration of the plugin")
                        .type("Array"));

        lfd.addAll(buildPluginParameterDescription("parameters[]."));

        return lfd;
    }

    private List<FieldDescriptor> buildPluginParameterDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields representationInformationField = new ConstrainedFields(PluginParameter.class);

        //        lfd.add(representationInformationField.withPath(addPrefix(prefix, "id"), "id", "Unique identifier"));
        lfd.add(representationInformationField.withPath(addPrefix(prefix, "name"), "name", "The parameter name"));
        lfd.add(representationInformationField.withPath(addPrefix(prefix, "value"), "value", "The parameter name"));
        //        lfd.add(representationInformationField.withPath(addPrefix(prefix, "pluginConfiguration"), "pluginConfiguration", "This is used when a plugin parameter leads to a plugin configuration"));
        lfd.add(representationInformationField
                        .withPath(addPrefix(prefix, "dynamic"), "dynamic", "The parameter is dynamic"));
        lfd.add(representationInformationField.withPath(addPrefix(prefix, "dynamicsValues"), "dynamicsValues",
                                                        "The set of possible values for the dynamic parameter"));
        lfd.add(representationInformationField
                        .withPath(addPrefix(prefix, "onlyDynamic"), "onlyDynamic", "The parameter is only dynamic"));

        return lfd;
    }

    private String addPrefix(String path) {
        return Strings.isNullOrEmpty(this.initPrefix) ? path : initPrefix + path;
    }

    private String addPrefix(String prefix, String path) {
        return Strings.isNullOrEmpty(prefix) ? path : prefix + path;
    }
}
