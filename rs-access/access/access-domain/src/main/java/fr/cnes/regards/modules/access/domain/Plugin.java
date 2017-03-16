/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class Plugin {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxProjectSequence")
    private Long id;

    private String name;

    private String description;

    private PluginType pluginType;

    private String mainJsfFile;

    /**
     * Default constructor
     */
    public Plugin() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pName
     *            the name of the plugin
     * @param pDescription
     *            the description of the plugin
     * @param pPluginType
     *            the {@link PluginType}
     * @param pMainJsfFile
     *            the main Javascript file of the plugin
     */
    public Plugin(String pName, String pDescription, PluginType pPluginType, String pMainJsfFile) {
        super();
        name = pName;
        description = pDescription;
        pluginType = pPluginType;
        mainJsfFile = pMainJsfFile;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public PluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(PluginType pPluginType) {
        pluginType = pPluginType;
    }

    public String getMainJsfFile() {
        return mainJsfFile;
    }

    public void setMainJsfFile(String pMainJsfFile) {
        mainJsfFile = pMainJsfFile;
    }

}
