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

import com.google.common.base.Strings;

/**
 * Plugin parameter type
 *
 * @author Christophe Mertz
 */
public class PluginParameterType {

    /**
     * The parameter's name
     */
    private String name;

    /**
     * The parameter's type Java
     */
    private String type;

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
     * A constructor with the attribute
     * 
     * @param pName
     *            The parameter's name
     * @param pType
     *            The parameter's type Java
     * @param pTypeEnum
     *            The parameter's type (ie PRIMITIVE or PLUGIN)
     * 
     */
    public PluginParameterType(String pName, String pType, ParamType pTypeEnum) {
        super();
        this.name = pName;
        this.type = pType;
        this.paramType = pTypeEnum;
    }

    public PluginParameterType(String pName, String pType, String pDefValue, boolean pOptional, ParamType pTypeEnum) {
        super();
        this.name = pName;
        this.type = pType;
        this.paramType = pTypeEnum;
        if (!Strings.isNullOrEmpty(pDefValue)) {
            this.defaultValue = pDefValue;
        }
        this.optional = pOptional;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        this.name = pName;
    }

    public String getType() {
        return type;
    }

    public void setType(String pType) {
        this.type = pType;
    }

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType pParamType) {
        this.paramType = pParamType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean isOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    /**
     * An enumeration with PRIMITIVE and PLUGIN defaultValue
     * 
     * @author Christophe Mertz
     *
     */
    public enum ParamType {
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
