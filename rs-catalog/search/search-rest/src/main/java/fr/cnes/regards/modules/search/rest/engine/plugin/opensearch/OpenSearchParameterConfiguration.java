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

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * OpenSearchParameterConfiguration
 * @author Sébastien Binda
 */
public class OpenSearchParameterConfiguration {

    /**
     * Opensearch parameter name
     */
    private String name;

    /**
     * Opensearch parameter namespace
     */
    private String namespace;

    /**
     * Minimum number of occurence of the parameter in search request
     */
    private int minimum;

    /**
     * Maximum number of occurence of the parameter in search request
     */
    private int maximum;

    /**
     * Pattern to validate value.
     */
    private String pattern;

    /**
     * Parameter title
     */
    private String title;

    /**
     * Does the parameter handle the option values when writting the description xml file.
     */
    private boolean optionsEnabled;

    /**
     * Maximum number of options or -1 for all values.
     */
    private int optionsCardinality;

    /**
     * Regards {@link AttributeModel} json path.
     */
    private String attributeModelJsonPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinimum() {
        return minimum;
    }

    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
