package fr.cnes.regards.modules.processing.plugins.exception;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

public class RightsPluginConfigurationNotFoundException extends Exception {

    private final PluginConfiguration pc;

    public RightsPluginConfigurationNotFoundException(PluginConfiguration pc) {
        this.pc = pc;
    }

    public PluginConfiguration getPc() {
        return pc;
    }
}
