package fr.cnes.regards.modules.storage.service.ready;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.ready.IModuleReady;
import fr.cnes.regards.framework.module.ready.ModuleReadiness;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.domain.ready.StorageReadySpecifications;

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
public class StorageModuleReady implements IModuleReady<StorageReadySpecifications> {

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    @Override
    public ModuleReadiness<StorageReadySpecifications> isReady() {
        boolean ready = true;
        List<String> reasons = Lists.newArrayList();
        //lets check allocation strategy
        Set<PluginConfiguration> strategies = pluginService.getPluginConfigurationsByType(IAllocationStrategy.class)
                .stream().filter(pc -> pc.isActive()).collect(Collectors.toSet());
        if (strategies.size() != 1) {
            reasons.add("There should be one and only one Allocation Strategy configured and active in the system. There is currently: "
                    + strategies.size());
            ready = false;
        }

        // check data storage
        Set<PluginConfiguration> dataStorages = pluginService.getPluginConfigurationsByType(IDataStorage.class).stream()
                .filter(pc -> pc.isActive()).collect(Collectors.toSet());
        if (dataStorages.isEmpty()) {
            reasons.add("There should be at least one ONLINE DataStorage configured and active in the system.");
            ready = false;
        }
        // check security delegation
        long numberSecurityDelegation = pluginService.getPluginConfigurationsByType(ISecurityDelegation.class).stream()
                .filter(pc -> pc.isActive()).count();
        if (numberSecurityDelegation != 1) {
            reasons.add("There should be one and only one Security Delegation configured and active in the system. There is currently: "
                    + numberSecurityDelegation);
            ready = false;
        }

        String allocationStrategy = null;
        if (strategies.stream().findFirst().isPresent()) {
            allocationStrategy = strategies.stream().findFirst().get().getLabel();
        }
        List<String> storages = dataStorages.stream().map(ds -> ds.getLabel()).collect(Collectors.toList());

        return new ModuleReadiness<StorageReadySpecifications>(ready, reasons,
                new StorageReadySpecifications(allocationStrategy, storages));
    }
}
