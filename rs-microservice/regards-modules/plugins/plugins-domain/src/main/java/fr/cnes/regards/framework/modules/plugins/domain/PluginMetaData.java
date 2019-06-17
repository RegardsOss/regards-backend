/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * @author Christophe Mertz
 */
public class PluginMetaData implements Comparable<PluginMetaData> {

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
     * The plugin markdown description, an optional detailed human readable description.
     */
    private String markdown;

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
     * license of the plugin.
     */
    private String license;

    /**
     * The parameters of the plugin
     */
    private List<PluginParameterType> parameters = new ArrayList<>();

    /**
     * Constructor initializing a plugin metadata from a plugin annotation
     */
    public PluginMetaData(Plugin plugin) {
        author = plugin.author();
        pluginId = plugin.id();
        version = plugin.version();
        description = plugin.description();
        url = plugin.url();
        contact = plugin.contact();
        owner = plugin.owner();
        license = plugin.license();
    }

    /**
     * Default constructor
     */
    public PluginMetaData() {

    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
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
     */
    public void setInterfaceNames(Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the contact
     */
    public String getContact() {
        return contact;
    }

    /**
     * Set the contact
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
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the license
     */
    public String getLicense() {
        return license;
    }

    /**
     * Set the licence
     */
    public void setLicence(String license) {
        this.license = license;
    }

    /**
     * @return the plugin parameter types
     */
    public List<PluginParameterType> getParameters() {
        return parameters;
    }

    /**
     * Set the plugin parameter types
     */
    public void setParameters(List<PluginParameterType> parameters) {
        if (parameters == null) {
            this.parameters.clear();
        } else {
            this.parameters = parameters;
        }
    }

    @Override
    public String toString() {
        String buf = pluginId + " : " + pluginClassName + " : " + version;
        return buf;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    @Override
    public int compareTo(PluginMetaData o) {
        return this.pluginId.compareToIgnoreCase(o.pluginId);
    }
}
