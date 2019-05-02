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

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;

/**
 * Parameter associated to a plugin configuration <PluginConfiguration>
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_plugin_parameter")
@SequenceGenerator(name = "pluginParameterSequence", initialValue = 1, sequenceName = "seq_plugin_parameter")
public class PluginParameter implements IIdentifiable<Long> {

    /**
     * Unique id
     */
    @ConfigIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginParameterSequence")
    private Long id;

    /**
     * Parameter name
     */
    @Column(nullable = false)
    @NotNull(message = "The plugin parameter name cannot be null")
    private String name;

    /**
     * Parameter value
     */
    @Column
    // Comment the @Column to create database with HBM2DDL for scriptGenerator.
    // Uncomment this line to create database with HBM2DDL for scriptGenerator.
    // @Transient
    private PluginParameterValue value;

    /**
     * {@link PluginConfiguration} parameter is optional This is used when a plugin parameter leads to a plugin
     * configuration. For example, a datasource (used by Dataset) is a plugin configuration and has a paramater
     * "connection" which is also a plugin configuration (the connection to database)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "next_conf_id", foreignKey = @ForeignKey(name = "fk_param_next_conf_id"), nullable = true)
    private PluginConfiguration pluginConfiguration;

    /**
     * The parameter is dynamic
     */
    private Boolean dynamic = false;

    /**
     * The set of possible values for the dynamic parameter
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_plugin_param_dyn_value", joinColumns = @JoinColumn(name = "id"),
            foreignKey = @ForeignKey(name = "fk_plugin_param_dyn_value_param_id"))
    @Column(name = "value")
    // Uncomment this two lines to create database with HBM2DDL for scriptGenerator.
    // You have to remove the @Column on the previous value field too.
    // @Type(type = "text")
    // @Convert(disableConversion = true)
    private Set<PluginParameterValue> dynamicsValues = new HashSet<>();

    /**
     * The parameter is only dynamic
     */
    @Transient
    private boolean onlyDynamic = false;

    @Transient
    @GsonIgnore
    private transient String decryptedValue;

    /**
     * Needed for deserialization
     */
    public PluginParameter() {
        // Nothing to do
    }

    /**
     * Do not use this constructor for manipulating plugin parameter. <br/>
     * <b>Use PluginParameterFactory to create parameters.</b>
     * @param name parameter name
     * @param value JSON normalized parameter value
     */
    public PluginParameter(String name, String value) {
        this.name = name;
        if (value != null) {
            this.value = PluginParameterValue.create(value);
        }
    }

    /**
     * Constructor for PluginConfiguration parameter (a DB connection for example)
     * @param name the parameter name
     * @param pluginConfiguration the plugin configuration
     */
    public PluginParameter(final String name, final PluginConfiguration pluginConfiguration) {
        super();
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
    }

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setIsDynamic(Boolean pIsDynamic) {
        dynamic = pIsDynamic;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration pPluginConfiguration) {
        pluginConfiguration = pPluginConfiguration;
    }

    public Set<PluginParameterValue> getDynamicsValues() {
        return dynamicsValues;
    }

    public void setDynamicsValues(Set<PluginParameterValue> pDynamicValues) {
        dynamicsValues = pDynamicValues;
    }

    public List<String> getDynamicsValuesAsString() {
        final List<String> result = new ArrayList<>();
        if (dynamicsValues != null && !dynamicsValues.isEmpty()) {
            dynamicsValues.forEach(d -> result.add(d.getValue()));
        }
        return result;
    }

    public boolean isValidDynamicValue(String value) {
        if (dynamicsValues == null || dynamicsValues.isEmpty()) {
            // No restriction
            return true;
        } else {
            for (PluginParameterValue dyn : dynamicsValues) {
                if (dyn.getValue().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getValue() {
        return value == null ? null : value.getValue();
    }

    /**
     * Do not use this setter for updating plugin parameter value. <br/>
     * <b>Use PluginParameterFactory update method.</b>
     * @param value JSON normalized parameter value
     */
    public void setValue(String value) {
        if (value != null) {
            this.value = PluginParameterValue.create(value);
        } else {
            this.value = null;
        }
    }

    /**
     * @return the stripped value (no enclosing quotes)
     */
    public String getStripParameterValue() {
        // Strip quotes using Gson
        Gson gson = new Gson();
        String value = null;
        if (this.value != null) {
            String tmp = this.value.getValue();
            if (tmp.startsWith("\"")) {
                JsonElement el = gson.fromJson(tmp, JsonElement.class);
                value = el == null ? null : el.getAsString();
            } else {
                value = tmp;
            }
        }
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /**
     * The name of the parameter is the natural id. Two plugin parameters can have the same name but not within same
     * plugin configuration
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PluginParameter other = (PluginParameter) obj;

        if (name == null) {
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }

    @Override
    public String toString() {
        String strBuilder = name + " - " + value + " - " + dynamic.toString();
        return strBuilder;
    }

    /**
     * Return 0 if the current {@link PluginParameter} is exactly the same as the given one.
     */
    public int compareTo(PluginParameter parameter) {
        return Comparator.comparing(PluginParameter::getName).thenComparing(PluginParameter::isDynamic)
                .compare(this, parameter);
    }

    public boolean isOnlyDynamic() {
        return onlyDynamic;
    }

    public void setOnlyDynamic(boolean onlyDynamic) {
        this.onlyDynamic = onlyDynamic;
    }

    public void setDecryptedValue(String decryptedValue) {
        this.decryptedValue = decryptedValue;
    }

    public String getDecryptedValue() {
        return decryptedValue;
    }
}
