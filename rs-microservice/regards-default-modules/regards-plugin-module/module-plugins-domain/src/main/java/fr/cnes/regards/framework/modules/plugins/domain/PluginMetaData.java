/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Plugin meta-data representation
 *
 * @author Christophe Mertz
 */
public class PluginMetaData {

    /**
     * The plugin class name
     */
    private String pluginClassName;

    /**
     * The interface used by the plugin
     */
    private Set<String> interfaceNames;

    /**
     * The author of the plugin
     */
    private String author;

    /**
     * The plugin identifier
     */
    private String pluginId;

    /**
     * The version of the plugin
     */
    private String version;

    /**
     * The description of the plugin
     */
    private String description;

    /**
     * An URL link to the web site of the plugin.
     */
    private String url;

    /**
     * An email to contact the plugin's author.
     */
    private String contact;

    /**
     * The legal owner of the plugin.
     */
    private String owner;

    /**
     * Licence of the plugin.
     */
    private String licence;

    /**
     * The parameters of the plugin
     */
    private List<PluginParameterType> parameters = new ArrayList<>();

    public PluginMetaData(Plugin plugin) {
        author = plugin.author();
        pluginId = plugin.id();
        version = plugin.version();
        description = plugin.description();
        url = plugin.url();
        contact = plugin.contact();
        owner = plugin.owner();
        licence = plugin.licence();
    }

    public PluginMetaData() {

    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pPluginId) {
        pluginId = pPluginId;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(String pPluginClassName) {
        pluginClassName = pPluginClassName;
    }

    public Set<String> getInterfaceNames() {
        if (interfaceNames == null) {
            interfaceNames = Sets.newHashSet();
        }
        return interfaceNames;
    }

    public void setInterfaceNames(Set<String> pInterfaceNames) {
        interfaceNames = pInterfaceNames;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String pAuthor) {
        author = pAuthor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String pVersion) {
        version = pVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String pUrl) {
        url = pUrl;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String pContact) {
        contact = pContact;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String pOwner) {
        owner = pOwner;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String pLicence) {
        licence = pLicence;
    }

    public List<PluginParameterType> getParameters() {
        return parameters;
    }

    public void setParameters(List<PluginParameterType> pParameters) {
        if (pParameters == null) {
            this.parameters.clear();
        } else {
            parameters = pParameters;
        }
    }

}
