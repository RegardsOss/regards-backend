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

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Parameter associated to a plugin configuration <PluginConfiguration>
 *
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_plugin_parameter")
@SequenceGenerator(name = "pluginParameterSequence", initialValue = 1, sequenceName = "seq_plugin_parameter")
public class PluginParameter implements IIdentifiable<Long> {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginParameterSequence")
    private Long id;

    /**
     * Parameter name
     */
    @Column(nullable = false)
    @NotNull
    private String name;

    /**
     * Parameter value
     */
    @Column
    // @Type(type = "text") - Cannot be used with a Converter and is not even used with Flyway DB tool so comment it!
    private PluginParameterValue value;

    /**
     * {@link PluginConfiguration} parameter is optional This is used when a plugin parameter leads to a plugin
     * configuration. For example, a datasource (used by Dataset) is a plugin configuration and has a paramater
     * "connection" which is also a plugin configuration (the connection to database)
     */
    @ManyToOne
    @JoinColumn(name = "next_conf_id", foreignKey = @ForeignKey(name = "fk_param_next_conf_id"), nullable = true)
    private PluginConfiguration pluginConfiguration;

    /**
     * The parameter is dynamic
     */
    private Boolean dynamic = false;

    /**
     * The set of possible values for the dynamic parameter
     */
    @ElementCollection
    @CollectionTable(name = "t_plugin_param_dyn_value", joinColumns = @JoinColumn(name = "id"),
            foreignKey = @ForeignKey(name = "fk_plugin_param_dyn_value_param_id"))
    @Column(name = "value")
    private Set<PluginParameterValue> dynamicsValues = new HashSet<>();

    /**
     * The parameter is only dynamic
     */
    @Transient
    private boolean onlyDynamic = false;

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
     * Constructor
     *
     * @param pName the parameter name
     * @param pPluginConfiguration the plugin configuration
     */
    public PluginParameter(final String pName, final PluginConfiguration pPluginConfiguration) {
        super();
        name = pName;
        pluginConfiguration = pPluginConfiguration;
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String pName) {
        name = pName;
    }

    public final Boolean isDynamic() {
        return dynamic;
    }

    public final void setIsDynamic(Boolean pIsDynamic) {
        dynamic = pIsDynamic;
    }

    public final PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public final void setPluginConfiguration(PluginConfiguration pPluginConfiguration) {
        pluginConfiguration = pPluginConfiguration;
    }

    public Set<PluginParameterValue> getDynamicsValues() {
        return dynamicsValues;
    }

    public List<String> getDynamicsValuesAsString() {
        final List<String> result = new ArrayList<>();
        if ((dynamicsValues != null) && !dynamicsValues.isEmpty()) {
            dynamicsValues.forEach(d -> result.add(d.getValue()));
        }
        return result;
    }

    public boolean isValidDynamicValue(String value) {
        if ((dynamicsValues == null) || dynamicsValues.isEmpty()) {
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

    public void setDynamicsValues(Set<PluginParameterValue> pDynamicValues) {
        dynamicsValues = pDynamicValues;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
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
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(name);
        strBuilder.append(" - ");
        strBuilder.append(value);
        strBuilder.append(" - ");
        strBuilder.append(dynamic.toString());
        return strBuilder.toString();
    }

    /**
     * Return 0 if the current {@link PluginParameter} is exactly the same as the given one.
     * @param parameter {@link PluginParameter}
     * @return
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
}
