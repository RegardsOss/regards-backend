/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import fr.cnes.regards.modules.storage.plugin.validation.FileSize;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class DataStorageInfo {

    private Long pluginId;

    private String label;

    private String description;

    @FileSize
    private String totalSize;

    @FileSize
    private String usedSize;

    public DataStorageInfo(Long pPluginId, String pLabel, String pDescription, @FileSize String pTotalSize,
            @FileSize String pUsedSize) {
        super();
        pluginId = pPluginId;
        label = pLabel;
        description = pDescription;
        totalSize = pTotalSize;
        usedSize = pUsedSize;
    }

    public Long getPluginId() {
        return pluginId;
    }

    public void setPluginId(Long pPluginId) {
        pluginId = pPluginId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String pLabel) {
        label = pLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(@FileSize String pTotalSize) {
        totalSize = pTotalSize;
    }

    public String getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(@FileSize String pUsedSize) {
        usedSize = pUsedSize;
    }

}
