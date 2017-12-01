package fr.cnes.regards.modules.storage.service.ready;

import java.util.List;

import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.ready.IModuleReady;
import fr.cnes.regards.framework.module.ready.ModuleReadiness;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.IAllocationStrategy;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;

/**
 * Allows to know if module storage is ready or not.
 * <ul>
 *     <li>At least one allocation strategy is configured and active</li>
 *     <li>At least one DataStorage is configured and active</li>
 * </ul>
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class StorageModuleReady implements IModuleReady {

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    @Override
    public ModuleReadiness isReady() {
        boolean ready = true;
        List<String> reasons = Lists.newArrayList();
        //lets check allocation strategy
        long numberAllocationStrategy = pluginService.getPluginConfigurationsByType(IAllocationStrategy.class).stream()
                .filter(pc -> pc.isActive()).count();
        if (numberAllocationStrategy != 1) {
            reasons.add(
                    "There should be one and only one Allocation Strategy configured and active in the system. There is currently: "
                            + numberAllocationStrategy);
            ready = false;
        }
        // check data storage
        long numberDataStorage = pluginService.getPluginConfigurationsByType(IDataStorage.class).stream()
                .filter(pc -> pc.isActive()).count();
        if (numberDataStorage <= 0) {
            reasons.add("There should be at least one DataStorage configured and active in the system.");
            ready = false;
        }
        return new ModuleReadiness(ready, reasons);
    }
}
