package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.util.Set;

import org.assertj.core.util.Sets;

/**
 * Contains data storage information aggregated with some meta data on this data storage: plugin id, plugin description, plugin configuration label.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class PluginStorageInfo {

    private Long pluginConfId;

    private String pluginDesc;

    private String pluginConfLabel;

    private final Set<DataStorageInfo> storageInfos = Sets.newHashSet();

    public PluginStorageInfo(Long pluginConfId, String pluginDesc, String pluginConfLabel) {
        this.pluginConfId = pluginConfId;
        this.pluginDesc = pluginDesc;
        this.pluginConfLabel = pluginConfLabel;
    }

    public Long getPluginConfId() {
        return pluginConfId;
    }

    public void setPluginConfId(Long pluginConfId) {
        this.pluginConfId = pluginConfId;
    }

    public String getPluginDesc() {
        return pluginDesc;
    }

    public void setPluginDesc(String pluginDesc) {
        this.pluginDesc = pluginDesc;
    }

    public String getPluginConfLabel() {
        return pluginConfLabel;
    }

    public void setPluginConfLabel(String pluginConfLabel) {
        this.pluginConfLabel = pluginConfLabel;
    }

    public Set<DataStorageInfo> getStorageInfos() {
        return storageInfos;
    }
}
