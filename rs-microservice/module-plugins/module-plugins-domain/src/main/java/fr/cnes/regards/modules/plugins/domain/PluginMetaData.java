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
    private Long id_;

    /**
     * 
     */
    private String metaDataId_;

    /**
     * The plugin class
     */
    private Class<?> pluginClass_;

    /**
     * The author of the plugin
     */
    private String author_;

    /**
     * The version of the plugin
     */
    private String version_;

    /**
     * The description of the plugin
     */
    private String description_;

    /**
     * The parameters of the plugin
     */
    private List<String> parameters_;

    /**
     * Get method.
     *
     * @return the Id
     */
    public Long getId() {
        return this.id_;
    }

    /**
     * Set method.
     *
     * @param pId
     *            the Id to set
     */
    public void setId(Long pId) {
        this.id_ = pId;
    }

    /**
     * Get method.
     * 
     * @return the meta data id
     */
    public String getMetaDataId() {
        return this.metaDataId_;
    }

    /**
     * Set method.
     * 
     * @param pMetaDataId
     *            the metadataId to set
     */
    public void setMetaDataId(String pMetaDataId) {
        this.metaDataId_ = pMetaDataId;
    }

    /**
     * Get method.
     *
     * @return the plugin class
     */
    public Class<?> getPluginClass() {
        return this.pluginClass_;
    }

    /**
     * Set method.
     *
     * @param pPluginClass
     *            the class which implements the plugin
     */
    public void setClass(Class<?> pPluginClass) {
        this.pluginClass_ = pPluginClass;
    }

    /**
     * Get method.
     *
     * @return the author
     */
    public String getAuthor() {
        return this.author_;
    }

    /**
     * Set method.
     *
     * @param pAuthor
     *            the author to set
     */
    public void setAuthor(String pAuthor) {
        this.author_ = pAuthor;
    }

    /**
     * Get method.
     *
     * @return the version
     */
    public String getVersion() {
        return this.version_;
    }

    /**
     * Set method.
     *
     * @param pVersion
     *            the version to set
     */
    public void setVersion(String pVersion) {
        this.version_ = pVersion;
    }

    /**
     * Get method.
     *
     * @return the description
     */
    public String getDescription() {
        return this.description_;
    }

    /**
     * Set method.
     *
     * @param pDescription
     *            the description to set
     */
    public void setDescription(String pDescription) {
        this.description_ = pDescription;
    }

    /**
     * Get method.
     *
     * @return the parameters
     */
    public List<String> getParameters() {
        return this.parameters_;
    }

    /**
     * Set method.
     *
     * @param pParameters
     *            the parameters to set
     */
    public void setParameters(List<String> pParameters) {
        this.parameters_ = pParameters;
    }

}
