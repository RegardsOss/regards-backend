/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * List<Long> wrapper for PluginParameter of {@link AIPMiscAllocationStratey}
 * @author Sébastien Binda
 */
public class PluginConfigurationIdentifiersWrapper {

    @PluginParameter(label = "Plugin configuration indentifiers")
    private List<Long> pluginConfIdentifiers = new ArrayList<>();

    public PluginConfigurationIdentifiersWrapper(List<Long> pluginConfIdentifiers) {
        super();
        this.pluginConfIdentifiers = pluginConfIdentifiers;
    }

    public void setPluginConfIdentifiers(List<Long> pluginConfIdentifiers) {
        this.pluginConfIdentifiers = pluginConfIdentifiers;
    }

    public List<Long> getPluginConfIdentifiers() {
        return pluginConfIdentifiers;
    }
}
