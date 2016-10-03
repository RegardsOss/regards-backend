/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

/**
 * Class Plugin
 *
 * Plugin meta-data representation
 *
 * @author cmertz
 */
public class PluginMetaData {

    private String id_;

    private Class<?> pluginClass_;

    private String author_;

    private String version_;

    private String description_;

    private List<String> parameters_;

    /**
     * Get method.
     *
     * @return the Id
     * @since 1.0
     */
    public String getId() {
        return id_;
    }

    /**
     * Set method.
     *
     * @param pId
     *            the Id to set
     * @since 1.0
     */
    public void setId(String pId) {
        id_ = pId;
    }

    /**
     * Get method.
     *
     * @return the plugin class
     * @since 1.0
     */
    public Class<?> getPluginClass() {
        return pluginClass_;
    }

    /**
     * Set method.
     *
     * @param pPluginClass
     *            the class which implements the plugin
     * @since 1.0
     */
    public void setClass(Class<?> pPluginClass) {
        pluginClass_ = pPluginClass;
    }

    /**
     * Get method.
     *
     * @return the author
     * @since 1.0
     */
    public String getAuthor() {
        return author_;
    }

    /**
     * Set method.
     *
     * @param pAuthor
     *            the author to set
     * @since 1.0
     */
    public void setAuthor(String pAuthor) {
        author_ = pAuthor;
    }

    /**
     * Get method.
     *
     * @return the version
     * @since 1.0
     */
    public String getVersion() {
        return version_;
    }

    /**
     * Set method.
     *
     * @param pVersion
     *            the version to set
     * @since 1.0
     */
    public void setVersion(String pVersion) {
        version_ = pVersion;
    }

    /**
     * Get method.
     *
     * @return the description
     * @since 1.0
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Set method.
     *
     * @param pDescription
     *            the description to set
     * @since 1.0
     */
    public void setDescription(String pDescription) {
        description_ = pDescription;
    }

    /**
     * Get method.
     *
     * @return the parameters
     * @since 1.0
     */
    public List<String> getParameters() {
        return parameters_;
    }

    /**
     * Set method.
     *
     * @param pParameters
     *            the parameters to set
     * @since 1.0
     */
    public void setParameters(List<String> pParameters) {
        parameters_ = pParameters;
    }

}
