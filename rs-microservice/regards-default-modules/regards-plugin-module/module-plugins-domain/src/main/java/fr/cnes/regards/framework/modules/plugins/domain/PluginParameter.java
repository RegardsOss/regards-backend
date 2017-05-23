/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;

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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

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
    @Type(type = "text")
    private String value;

    /**
     * {@link PluginConfiguration} parameter is optional This is used when a plugin paramter leads to a plugin
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
     * The list of values for a dynamic parameters
     */
    @ElementCollection
    @CollectionTable(name = "t_plugin_param_dyn_value", joinColumns = @JoinColumn(name = "id"),
            foreignKey = @ForeignKey(name = "fk_plugin_param_dyn_value_param_id"))
    private List<PluginDynamicValue> dynamicsValues;

    /**
     * Default constructor
     */
    public PluginParameter() {
        super();
        name = "undefined";
    }

    /**
     * Constructor
     *
     * @param pName the parameter name
     * @param pValue the parameter value
     */
    public PluginParameter(final String pName, final String pValue) {
        super();
        name = pName;
        value = pValue;
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

    public final String getValue() {
        return value;
    }

    public final void setValue(String pValue) {
        value = pValue;
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

    public List<PluginDynamicValue> getDynamicsValues() {
        return dynamicsValues;
    }

    public List<String> getDynamicsValuesAsString() {
        final List<String> result = new ArrayList<>();
        if ((dynamicsValues != null) && !dynamicsValues.isEmpty()) {
            dynamicsValues.forEach(d -> result.add(d.getValue()));
        }
        return result;
    }

    public void setDynamicsValues(List<PluginDynamicValue> pDynamicValues) {
        dynamicsValues = pDynamicValues;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
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

}
