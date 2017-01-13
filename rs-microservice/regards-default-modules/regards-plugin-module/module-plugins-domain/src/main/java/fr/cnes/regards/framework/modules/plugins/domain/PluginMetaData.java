/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.List;

/**
 * Plugin meta-data representation
 *
 * @author Christophe Mertz
 */
public class PluginMetaData {

    /**
     * The plugin identifier
     */
    private String pluginId;

    /**
     * The plugin class name
     */
    private String pluginClassName;

    /**
     * The author of the plugin
     */
    private String author;

    /**
     * The version of the plugin
     */
    private String version;

    /**
     * The description of the plugin
     */
    private String description;

    /**
     * The parameters of the plugin
     */
    private List<PluginParameterType> parameters;

    public String getPluginId() {
        return this.pluginId;
    }

    public void setPluginId(String pPluginId) {
        this.pluginId = pPluginId;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(String pPluginClassName) {
        this.pluginClassName = pPluginClassName;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String pAuthor) {
        this.author = pAuthor;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String pVersion) {
        this.version = pVersion;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String pDescription) {
        this.description = pDescription;
    }

    public List<PluginParameterType> getParameters() {
        return parameters;
    }

    public void setParameters(List<PluginParameterType> pParameters) {
        this.parameters = pParameters;
    }


}
