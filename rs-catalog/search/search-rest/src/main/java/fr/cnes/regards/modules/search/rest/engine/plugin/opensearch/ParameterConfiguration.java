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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * OpenSearchParameterConfiguration
 * @author Sébastien Binda
 */
public class ParameterConfiguration {

    /**
     * Opensearch parameter name
     */
    @PluginParameter(name = "name", label = "Opensearch name of the parameter",
            description = "Name that will be handled by opensearch parameters extensions. Example in time extension the parameter {time:start} name is start")
    private String name;

    /**
     * Opensearch parameter namespace
     */
    @PluginParameter(name = "namespace", label = "Opensearch namespace of the parameter",
            description = "Namespace that will be handled by opensearch parameters extensions. Example in time extension the parameter {time:start} namespace is time")
    private String namespace;

    /**
     * Does the parameter handle the option values when writting the description xml file.
     */
    @PluginParameter(name = "optionsEnabled", label = "Enable generation of possible values.",
            description = "Enable the generation of possible values of the parameter in the opensearch descriptor xml file. Be carful, this option can rise the generation time of the opensearch descriptor xml file.")
    private boolean optionsEnabled;

    /**
     * Maximum number of options or -1 for all values.
     */
    @PluginParameter(name = "optionsEnabled", label = "",
            description = "Only used if the optionsEnabled parameter is set to TRUE. Limit the number of possbile values of the parameter in the opensearch descriptor xml file.")
    private int optionsCardinality;

    /**
     * Regards {@link AttributeModel} json path.
     */
    @PluginParameter(name = "attributeModelJsonPath", label = "Full json path of associated REGARDS attribute",
            description = "Full jsonpath is  : properties.<optional fragment name>.<attribute name>")
    private String attributeModelJsonPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOptionsEnabled() {
        return optionsEnabled;
    }

    public void setOptionsEnabled(boolean optionsEnabled) {
        this.optionsEnabled = optionsEnabled;
    }

    public int getOptionsCardinality() {
        return optionsCardinality;
    }

    public void setOptionsCardinality(int optionsCardinality) {
        this.optionsCardinality = optionsCardinality;
    }

    public String getAttributeModelJsonPath() {
        return attributeModelJsonPath;
    }

    public void setAttributeModelJsonPath(String attributeModelJsonPath) {
        this.attributeModelJsonPath = attributeModelJsonPath;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
