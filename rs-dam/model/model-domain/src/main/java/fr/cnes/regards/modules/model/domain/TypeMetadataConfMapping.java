package fr.cnes.regards.modules.model.domain;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

import java.util.Set;

/**
 * POJO allowing us to map which plugin configurations and plugin metadata can be mapped to which attribute type
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class TypeMetadataConfMapping {

    /**
     * The attribute type
     */
    private PropertyType attrType;

    /**
     * The plugin configurations
     */
    private Set<PluginConfiguration> pluginConfigurations;

    /**
     * the plugin meta data
     */
    private Set<PluginMetaData> pluginMetaDatas;

    /**
     * Default constructor
     */
    public TypeMetadataConfMapping() {
    }

    /**
     * Constructor setting the parameters as attribute
     */
    public TypeMetadataConfMapping(PropertyType attrType,
                                   Set<PluginConfiguration> pluginConfigurations,
                                   Set<PluginMetaData> pluginMetaData) {
        this.attrType = attrType;
        this.pluginConfigurations = pluginConfigurations;
        this.pluginMetaDatas = pluginMetaData;
    }

    /**
     * @return the attribute type
     */
    public PropertyType getAttrType() {
        return attrType;
    }

    /**
     * Set the attribute type
     */
    public void setAttrType(PropertyType attrType) {
        this.attrType = attrType;
    }

    /**
     * @return the plugin configuration
     */
    public Set<PluginConfiguration> getPluginConfigurations() {
        return pluginConfigurations;
    }

    /**
     * Set the plugin configurations
     */
    public void setPluginConfigurations(Set<PluginConfiguration> pluginConfigurations) {
        this.pluginConfigurations = pluginConfigurations;
    }

    /**
     * @return the plugin metadata
     */
    public Set<PluginMetaData> getPluginMetaDatas() {
        return pluginMetaDatas;
    }

    /**
     * Set the plugin meta data
     */
    public void setPluginMetaDatas(Set<PluginMetaData> pluginMetaDatas) {
        this.pluginMetaDatas = pluginMetaDatas;
    }
}
