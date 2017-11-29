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

    /**
     * Constructor initializing a plugin metadata from a plugin annotation
     * @param plugin
     */
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

    /**
     * Default constructor
     */
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

    /**
     * @return the interface names
     */
    public Set<String> getInterfaceNames() {
        if (interfaceNames == null) {
            interfaceNames = Sets.newHashSet();
        }
        return interfaceNames;
    }

    /**
     * Set the interface names
     * @param pInterfaceNames
     */
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

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the url
     * @param pUrl
     */
    public void setUrl(String pUrl) {
        url = pUrl;
    }

    /**
     * @return the contact
     */
    public String getContact() {
        return contact;
    }

    /**
     * Set the contact
     * @param pContact
     */
    public void setContact(String pContact) {
        contact = pContact;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Set the owner
     * @param pOwner
     */
    public void setOwner(String pOwner) {
        owner = pOwner;
    }

    /**
     * @return the licence
     */
    public String getLicence() {
        return licence;
    }

    /**
     * Set the licence
     * @param pLicence
     */
    public void setLicence(String pLicence) {
        licence = pLicence;
    }

    /**
     * @return the plugin parameter types
     */
    public List<PluginParameterType> getParameters() {
        return parameters;
    }

    /**
     * Set the plugin parameter types
     * @param pParameters
     */
    public void setParameters(List<PluginParameterType> pParameters) {
        if (pParameters == null) {
            this.parameters.clear();
        } else {
            parameters = pParameters;
        }
    }
    
    @Override
    public String toString() {
        StringBuffer strBuffer = new StringBuffer(pluginId);
        strBuffer.append(" : ");
        strBuffer.append(pluginClassName);
        strBuffer.append(" : ");
        strBuffer.append(version);
        return strBuffer.toString();
    }

}
