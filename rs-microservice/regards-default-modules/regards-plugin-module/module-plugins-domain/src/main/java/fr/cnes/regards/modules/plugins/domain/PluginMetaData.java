/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.hateoas.Identifiable;

/**
 * Class Plugin
 *
 * Plugin meta-data representation
 *
 * @author cmertz
 */
public class PluginMetaData implements Identifiable<Long> {

    /**
     * Internal identifier
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * The plugin identifier
     */
    private String pluginId;

    /**
     * The plugin class
     */
    private Class<?> pluginClass;

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
    private List<String> parameters;

    public Long getId() {
        return this.id;
    }

    public void setId(Long pId) {
        this.id = pId;
    }

    public String getPluginId() {
        return this.pluginId;
    }

    public void setPluginId(String pPluginId) {
        this.pluginId = pPluginId;
    }

    public Class<?> getPluginClass() {
        return this.pluginClass;
    }

    public void setClass(Class<?> pPluginClass) {
        this.pluginClass = pPluginClass;
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

    public List<String> getParameters() {
        return this.parameters;
    }

    public void setParameters(List<String> pParameters) {
        this.parameters = pParameters;
    }

}
