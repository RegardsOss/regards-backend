/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Plugin configuration contains a unique Id, plugin meta-data and parameters.
 *
 */
@Entity
@Table(name = "T_PLUGIN_CONFIGURATION",
        indexes = { @Index(name = "IDX_PLUGIN_CONFIGURATION", columnList = "pluginId") })
@SequenceGenerator(name = "pluginConfSequence", initialValue = 1, sequenceName = "SEQ_PLUGIN_CONF")
public class PluginConfiguration implements IIdentifiable<Long> {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginConfSequence")
    private Long id;

    /**
     * Unique identifier of the plugin. This id is the id defined in the "@Plugin" annotation of the plugin
     * implementation class.
     */
    private String pluginId;

    /**
     * Label to identify the configuration.
     */
    private String label;

    /**
     * Version of the plugin configuration. Is set with the plugin version. This attribute is used to check if the saved
     * configuration plugin version differs from the loaded plugin.
     */
    private String version;

    /**
     * Priority order of the plugin.
     */
    private Integer priorityOrder;

    /**
     * The plugin configuration is active.
     */
    private Boolean active;

    /**
     * The plugin class name
     */
    private String pluginClassName;

    /**
     * Configuration parameters of the plugin
     */
    @ElementCollection
    @CollectionTable(
            joinColumns = @JoinColumn(name = "ID", foreignKey = @javax.persistence.ForeignKey(name = "FK_PARAM_ID")))
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PluginParameter> parameters;

    /**
     * Default constructor
     */
    public PluginConfiguration() {
        super();
    }

    /**
     * A constructor with {@link PluginMetaData} and list of {@link PluginParameter}.
     *
     * @param pPluginMetaData
     *            the plugin's metadata
     * @param pLabel
     *            the label
     * @param pParameters
     *            the list of parameters
     * @param pOrder
     *            the order
     */
    public PluginConfiguration(final PluginMetaData pPluginMetaData, final String pLabel,
            final List<PluginParameter> pParameters, final int pOrder) {
        super();
        pluginId = pPluginMetaData.getPluginId();
        version = pPluginMetaData.getVersion();
        pluginClassName = pPluginMetaData.getPluginClassName();
        parameters = pParameters;
        priorityOrder = pOrder;
        label = pLabel;
        active = Boolean.TRUE;
    }

    /**
     * A constructor with {@link PluginMetaData}.
     * 
     * @param pPluginMetaData
     *            the plugin's metadata
     * @param pLabel
     *            the label
     * @param pOrder
     *            the order
     */
    public PluginConfiguration(final PluginMetaData pPluginMetaData, final String pLabel, final int pOrder) {
        super();
        pluginId = pPluginMetaData.getPluginId();
        version = pPluginMetaData.getVersion();
        pluginClassName = pPluginMetaData.getPluginClassName();
        priorityOrder = pOrder;
        label = pLabel;
        active = Boolean.TRUE;
    }

    /**
     * Return the value of a specific parameter
     *
     * @param pParameterName
     *            the parameter to get the value
     * @return the value of the parameter
     */
    public final String getParameterValue(String pParameterName) {
        String value = null;
        if (parameters != null) {
            final Optional<PluginParameter> pluginParameter = parameters.stream()
                    .filter(s -> s.getName().equals(pParameterName)).findFirst();
            if (pluginParameter.isPresent()) {
                value = pluginParameter.get().getValue();
            }
        }
        return value;
    }

    /**
     * Return the value of a specific parameter {@link PluginConfiguration}
     *
     * @param pParameterName
     *            the parameter to get the value
     * @return the value of the parameter
     */
    public final PluginConfiguration getParameterConfiguration(String pParameterName) {
        PluginConfiguration value = null;
        if (parameters != null) {
            final Optional<PluginParameter> pluginParameter = parameters.stream()
                    .filter(s -> s.getName().equals(pParameterName)).findFirst();
            if (pluginParameter.isPresent()) {
                value = pluginParameter.get().getPluginConfiguration();
            }
        }
        return value;
    }

    public final String getLabel() {
        return label;
    }

    public final void setLabel(String pLabel) {
        label = pLabel;
    }

    public final String getVersion() {
        return version;
    }

    public final void setVersion(String pVersion) {
        version = pVersion;
    }

    public final String getPluginId() {
        return pluginId;
    }

    public final void setPluginId(String pPluginId) {
        pluginId = pPluginId;
    }

    public final Integer getPriorityOrder() {
        return priorityOrder;
    }

    public final void setPriorityOrder(Integer pOrder) {
        priorityOrder = pOrder;
    }

    public final List<PluginParameter> getParameters() {
        return parameters;
    }

    public final void setParameters(List<PluginParameter> pParameters) {
        parameters = pParameters;
    }

    public Boolean isActive() {
        return active;
    }

    public void setIsActive(Boolean pIsActive) {
        this.active = pIsActive;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }
}
