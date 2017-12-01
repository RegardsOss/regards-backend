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

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Plugin parameter type
 *
 * @author Christophe Mertz
 */
public class PluginParameterType {

    /**
     * The parameter's name used as a key for database registration
     */
    private String name;

    /**
     * The parameter label, a required human readable information
     */
    private String label;

    /**
     * The parameter description, an optional further human readable information if the label is not explicit enough!
     */
    private String description;

    /**
     * The JAVA parameter's type
     */
    private String type;

    /**
     * Argument parameter types for parameterized types
     */
    private String[] parameterizedSubTypes;

    /**
     * The parameters's type {@link ParamType}.
     */
    private ParamType paramType;

    /**
     * A default value for the paramater
     */
    private String defaultValue;

    /**
     * Define if the parameter is optional or mandatory
     */
    private Boolean optional;

    /**
     * The parameters of the plugin
     */
    private List<PluginParameterType> parameters = new ArrayList<>();

    /**
     * {@link PluginParameterType} builder.<br/>
     * Additional setter can be used :
     * <ul>
     * <li>{@link #setDefaultValue(String)}</li>
     * <li>{@link #setParameters(List)}</li>
     * <li>{@link #setParameterizedSubTypes(String...)}</li>
     * </ul>
     * @param name parameter's name used as a key for database registration
     * @param label a required human readable information
     * @param description an optional further human readable information if the label is not explicit enough!
     * @param clazz The JAVA class
     * @param paramType {@link ParamType}
     * @param optional true if parameter is optional
     * @return {@link PluginParameterType}
     */
    public static PluginParameterType create(String name, String label, String description, Class<?> clazz,
            ParamType paramType, Boolean optional) {
        PluginParameterType ppt = new PluginParameterType();

        // Validate and set
        Assert.hasText(name, "Name is required");
        ppt.setName(name);

        Assert.hasText(label, "Label is required");
        ppt.setLabel(label);

        ppt.setDescription(description);

        Assert.notNull(clazz, "Class is required");
        ppt.setType(clazz.getName());

        Assert.notNull(paramType, "Parameter type is required");
        ppt.setParamType(paramType);

        Assert.notNull(optional, "Optional value is required");
        ppt.setOptional(optional);

        return ppt;
    }

    public String getName() {
        return name;
    }

    private void setName(String pName) {
        this.name = pName;
    }

    public String getType() {
        return type;
    }

    private void setType(String pType) {
        this.type = pType;
    }

    public ParamType getParamType() {
        return paramType;
    }

    private void setParamType(ParamType pParamType) {
        this.paramType = pParamType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        Assert.hasText(defaultValue, "Default value is required");
        this.defaultValue = defaultValue;
    }

    public Boolean isOptional() {
        return optional;
    }

    private void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public List<PluginParameterType> getParameters() {
        return parameters;
    }

    public void addAllParameters(List<PluginParameterType> parameterTypes) {
        if (parameterTypes != null) {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameters.addAll(parameterTypes);
        }
    }

    public String getLabel() {
        return label;
    }

    private void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public String[] getParameterizedSubTypes() {
        return parameterizedSubTypes;
    }

    public void setParameterizedSubTypes(String... parameterizedSubTypes) {
        this.parameterizedSubTypes = parameterizedSubTypes;
    }

    /**
     * An enumeration with PRIMITIVE and PLUGIN defaultValue
     *
     * @author Christophe Mertz
     *
     */
    public enum ParamType {

        /**
         * Parameter type {@link Map}
         */
        MAP,
        /**
         * Parameter type {@link java.util.Collection}
         */
        COLLECTION,
        /**
         * Object type (not parameterized)
         */
        OBJECT,
        /**
         * Parameter type primitif
         */
        PRIMITIVE,
        /**
         * Parameter type plugin
         */
        PLUGIN
    }
}
