/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

/**
 * Plugin parameter POJO for plugin {@link AIPMiscAllocationStrategyPlugin}
 * @author Sébastien Binda
 */
public class AIPMiscStorageInformation {

    /**
     * Plugin identifier
     */
    private String pluginId;

    /**
     * Disrectory to use
     */
    private String directory;

    public String getPluginId() {
        return pluginId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

}
