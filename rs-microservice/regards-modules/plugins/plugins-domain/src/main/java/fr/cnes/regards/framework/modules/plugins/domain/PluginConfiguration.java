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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converter.SetStringCsvConverter;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * Plugin configuration contains a unique Id, plugin meta-data and parameters.
 * @author cmertz
 * @author oroussel
 */
@Entity
@Table(name = "t_plugin_configuration",
        indexes = { @Index(name = "idx_plugin_configuration", columnList = "pluginId"),
                @Index(name = "idx_plugin_configuration_label", columnList = "label") },
        uniqueConstraints = @UniqueConstraint(name = "uk_plugin_configuration_label", columnNames = { "label" }))
@SequenceGenerator(name = "pluginConfSequence", initialValue = 1, sequenceName = "seq_plugin_conf")
public class PluginConfiguration implements IIdentifiable<Long> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfiguration.class);

    /**
     * A constant used to define a {@link String} constraint with length 255
     */
    private static final int MAX_STRING_LENGTH = 255;

    /**
     * Unique id
     */
    @ConfigIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginConfSequence")
    private Long id;

    /**
     * Unique identifier of the plugin. This id is the id defined in the "@Plugin" annotation of the plugin
     * implementation class.
     */
    @Column(nullable = false)
    @NotNull
    private String pluginId;

    /**
     * Label to identify the configuration.
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_LENGTH)
    private String label;

    /**
     * Version of the plugin configuration. Is set with the plugin version. This attribute is used to check if the saved
     * configuration plugin version differs from the loaded plugin.
     * <b>DO NOT SET @NotNull even if version must not be null. Validation is done between front and back but at
     * creation version is retrieved from plugin metdata and thus set by back</b>
     */
    @Column(nullable = false, updatable = true)
    private String version;

    /**
     * Priority order of the plugin.
     */
    @NotNull
    @Column(nullable = false, updatable = true)
    private Integer priorityOrder = 0;

    /**
     * The plugin configuration is active.
     */
    private Boolean active = true;

    /**
     * The plugin class name
     */
    private String pluginClassName;

    /**
     * The interfaces, that implements {@link PluginInterface}, implemented by the pluginClassName
     */
    @Column(columnDefinition = "text")
    @Convert(converter = SetStringCsvConverter.class)
    private Set<String> interfaceNames = Sets.newHashSet();;

    /**
     * Configuration parameters of the plugin
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "parent_conf_id", foreignKey = @ForeignKey(name = "fk_plg_conf_param_id"))
    private List<PluginParameter> parameters = Lists.newArrayList();

    /**
     * Icon of the plugin. It must be an URL to a svg file.
     */
    @Column(name = "icon_url")
    private URL iconUrl;

    /**
     * Default constructor
     */
    public PluginConfiguration() {
        super();
    }

    /**
     * A constructor with {@link PluginMetaData}.
     * @param metaData the plugin's metadata
     * @param label the label
     */
    public PluginConfiguration(PluginMetaData metaData, String label) {
        this(metaData, label, Lists.newArrayList(), 0);
    }

    /**
     * A constructor with {@link PluginMetaData} and list of {@link PluginParameter}.
     * @param metaData the plugin's metadata
     * @param label the label
     * @param parameters the list of parameters
     */
    public PluginConfiguration(PluginMetaData metaData, String label, List<PluginParameter> parameters) {
        this(metaData, label, parameters, 0);
    }

    /**
     * A constructor with {@link PluginMetaData} and list of {@link PluginParameter}.
     * @param metaData the plugin's metadata
     * @param label the label
     * @param parameters the list of parameters
     * @param order the order
     */
    public PluginConfiguration(PluginMetaData metaData, String label, List<PluginParameter> parameters, int order) {
        super();
        this.setMetaData(metaData);
        this.parameters = parameters;
        priorityOrder = order;
        this.label = label;
        active = Boolean.TRUE;
    }

    /**
     * A constructor with {@link PluginMetaData}.
     * @param pPluginMetaData the plugin's metadata
     * @param pLabel the label
     * @param pOrder the order
     */
    public PluginConfiguration(PluginMetaData pPluginMetaData, String pLabel, int pOrder) {
        this(pPluginMetaData, pLabel, Lists.newArrayList(), pOrder);
    }

    /**
     * Constructor initializing a new plugin configuration from an other one
     */
    public PluginConfiguration(PluginConfiguration other) {
        active = other.active;
        id = other.id;
        interfaceNames = Sets.newHashSet(other.interfaceNames);
        label = other.label;
        parameters = Lists.newArrayList(other.parameters);
        pluginClassName = other.pluginClassName;
        pluginId = other.pluginId;
        priorityOrder = other.priorityOrder;
        version = other.version;
        iconUrl = other.iconUrl;
    }

    public final void setMetaData(PluginMetaData pluginMetaData) {
        pluginId = pluginMetaData.getPluginId();
        version = pluginMetaData.getVersion();
        pluginClassName = pluginMetaData.getPluginClassName();
        interfaceNames = Sets.newHashSet(pluginMetaData.getInterfaceNames());
    }

    /**
     * Return the value of a specific parameter
     * @param pParameterName the parameter to get the value
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
     * @param parameterName the parameter to get the value
     * @return the stripped value (no enclosing quotes)
     */
    public String getStripParameterValue(String parameterName) {
        String value = null;
        if (parameters != null) {
            Optional<PluginParameter> pluginParameter = parameters.stream()
                    .filter(s -> s.getName().equals(parameterName)).findFirst();
            if (pluginParameter.isPresent()) {
                // Strip quotes using Gson
                return pluginParameter.get().getStripParameterValue();
            }
        }
        return value;
    }

    /**
     * Return the {@link PluginParameter} of a specific parameter
     * @param pParameterName the parameter to get the value
     * @return the {@link PluginParameter}
     */
    public final PluginParameter getParameter(String pParameterName) {
        PluginParameter searchPluginParam = null;
        if (parameters != null) {
            final Optional<PluginParameter> pluginParameter = parameters.stream()
                    .filter(s -> s.getName().equals(pParameterName)).findFirst();
            if (pluginParameter.isPresent()) {
                searchPluginParam = pluginParameter.get();
            }
        }
        return searchPluginParam;
    }

    /**
     * Return the value of a specific parameter {@link PluginConfiguration}
     * @param pParameterName the parameter to get the value
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

    /**
     * Log the {@link PluginParameter} of the {@link PluginConfiguration}.
     */
    public void logParams() {
        LOGGER.info("===> parameters <===");
        LOGGER.info("  ---> number of dynamic parameters : "
                + getParameters().stream().filter(p -> p.isDynamic()).count());

        getParameters().stream().filter(p -> p.isDynamic()).forEach(p -> {
            logParam(p, "  ---> dynamic parameter : ");
        });

        LOGGER.info("  ---> number of no dynamic parameters : "
                + getParameters().stream().filter(p -> !p.isDynamic()).count());
        getParameters().stream().filter(p -> !p.isDynamic()).forEach(p -> {
            logParam(p, "  ---> parameter : ");
        });
    }

    /**
     * Log a {@link PluginParameter}.
     * @param pParam the {@link PluginParameter} to log
     * @param pPrefix a prefix to set in the log
     */
    private void logParam(PluginParameter pParam, String pPrefix) {
        LOGGER.info(pPrefix + pParam.getName() + "-def val:" + pParam.getValue());
        if (!pParam.getDynamicsValuesAsString().isEmpty()) {
            pParam.getDynamicsValuesAsString().forEach(v -> LOGGER.info("     --> val=" + v));
        }
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

    /**
     * This setter <b>must</b> only be used while TESTING
     */
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
        active = pIsActive;
    }

    public void setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    /**
     * @return the interface names
     */
    public Set<String> getInterfaceNames() {
        return interfaceNames;
    }

    /**
     * Set the interface names
     */
    public void setInterfaceNames(Set<String> pInterfaceNames) {
        interfaceNames = pInterfaceNames;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    /**
     * @return the iconUrl
     */
    public URL getIconUrl() {
        return iconUrl;
    }

    /**
     * @param pIconUrl the iconUrl to set
     */
    public void setIconUrl(URL pIconUrl) {
        iconUrl = pIconUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((label == null) ? 0 : label.hashCode());
        result = (prime * result) + ((pluginId == null) ? 0 : pluginId.hashCode());
        return result;
    }

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
        PluginConfiguration other = (PluginConfiguration) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (pluginId == null) {
            if (other.pluginId != null) {
                return false;
            }
        } else if (!pluginId.equals(other.pluginId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PluginConfiguration [id=" + id + ", pluginId=" + pluginId + ", label=" + label + ", version=" + version
                + ", priorityOrder=" + priorityOrder + ", active=" + active + ", pluginClassName=" + pluginClassName
                + ", interfaceName=" + interfaceNames + "]";
    }
}
