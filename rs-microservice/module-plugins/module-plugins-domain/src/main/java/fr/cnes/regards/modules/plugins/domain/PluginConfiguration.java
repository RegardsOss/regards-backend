/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.springframework.hateoas.Identifiable;;

/**
 * Class PluginConfiguration
 *
 * Plugin configuration contains a unique Id, plugin meta-data and parameters.
 *
 */
@Entity
@Table(name = "T_PLUGIN_CONFIGURATION", indexes = {
        @Index(name = "IDX_PLUGIN_CONFIGURATION", columnList = "pluginId") })
public class PluginConfiguration implements Identifiable<Long> {

    /**
     * Internal identifier
     */
    @Id
    @GeneratedValue
    private Long id;
    
    /**
     * Unique identifier of the plugin. This id is the id defined in the "@Plugin" annotation of the plugin
     * implementation class
     */
    private String pluginId_;

    /**
     * Label to identify the configuration.
     */
    private String label_;

    /**
     * Version of the plugin configuration. Equals to the plugin version_ when configurer. This attribute is used to
     * check if the saved configuration plugin version_ differs from the loaded plugin
     */
    private String version_;

    /**
     * Priority order of the plugin.
     */
    private Integer priorityOrder_;

    /**
     * Configuration parameters_ of the plugin.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "TA_PLUGIN_PARAMETERS_VALUE", joinColumns = {
            @JoinColumn(name = "PLUGIN_ID", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "PARAMETER_ID", referencedColumnName = "id") })
    private List<PluginParameter> parameters_;

    /**
     * Default constructor
     *
     */
    public PluginConfiguration() {
        super();
    }

    /**
     * 
     * @param pPluginMetaData
     *            : the plugin's metadata
     * @param pLabel
     *            : the label
     * @param pParameters
     *            : the list of parameters_
     * @param pOrder
     *            : the order
     */
    public PluginConfiguration(final PluginMetaData pPluginMetaData, final String pLabel,
            final List<PluginParameter> pParameters, final int pOrder) {
        super();
        pluginId_ = pPluginMetaData.getMetaDataId();
        version_ = pPluginMetaData.getVersion();
        parameters_ = pParameters;
        priorityOrder_ = pOrder;
        label_ = pLabel;
    }

    /**
     *
     * Plugin Parameter getter
     *
     * @param pParameterName
     *            : the parameter
     * 
     * @return the parameter's value
     */
    public final String getParameterValue(String pParameterName) {
        String value = null;
        if (parameters_ != null) {
            for (final PluginParameter parameter : parameters_) {
                if (parameter.getName().equals(pParameterName)) {
                    value = parameter.getValue();
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Get method.
     *
     * @return the label
     */
    public final String getLabel() {
        return label_;
    }

    /**
     * Set method.
     *
     * @param pLabel
     *            the label_ to set
     */
    public final void setLabel(String pLabel) {
        label_ = pLabel;
    }

    /**
     * Get method.
     *
     * @return the version_
     */
    public final String getVersion() {
        return version_;
    }

    /**
     * Set method.
     *
     * @param pVersion
     *            the version_ to set
     */
    public final void setVersion(String pVersion) {
        version_ = pVersion;
    }

    /**
     * Get method.
     *
     * @return the pLuginId
     */
    public final String getPluginId() {
        return pluginId_;
    }

    /**
     * Set method.
     *
     * @param pPluginId
     *            the pLuginId to set
     */
    public final void setPluginId(String pPluginId) {
        pluginId_ = pPluginId;
    }

    /**
     * Get method.
     *
     * @return the order
     */
    public final Integer getPriorityOrder() {
        return priorityOrder_;
    }

    /**
     * Set method.
     *
     * @param pOrder
     *            the order to set
     */
    public final void setPriorityOrder(Integer pOrder) {
        priorityOrder_ = pOrder;
    }

    /**
     * Get method.
     *
     * @return the parameters_
     */
    public final List<PluginParameter> getParameters() {
        return parameters_;
    }

    /**
     * Set method.
     *
     * @param pParameters
     *            the parameters_ to set
     */
    public final void setParameters(List<PluginParameter> pParameters) {
        parameters_ = pParameters;
    }

    @Override
    public Long getId() {
        return this.id;
    }


}
