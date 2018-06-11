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
     * Parameter name
     */
    private String name;

    /**
     * Minimum number of occurence of the parameter in search request
     */
    private Integer minimum;

    /**
     * Maximum number of occurence of the parameter in search request
     */
    private Integer maximum;

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
    private Integer optionsCardinality;

    /**
     * Regards {@link AttributeModel} identifier.
     */
    private String attributeModelName;

    /**
     * Operator to apply when searching in catalog for this parameter.
     */
    private ParameterOperator operator = ParameterOperator.EQ;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
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

    public Integer getOptionsCardinality() {
        return optionsCardinality;
    }

    public void setOptionsCardinality(Integer optionsCardinality) {
        this.optionsCardinality = optionsCardinality;
    }

    public String getAttributeModelName() {
        return attributeModelName;
    }

    public void setAttributeModelName(String attributeModelName) {
        this.attributeModelName = attributeModelName;
    }

    public ParameterOperator getOperator() {
        return operator;
    }

    public void setOperator(ParameterOperator operator) {
        this.operator = operator;
    }

}
