/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.Collection;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * Default Implementation of IAllocationStrategy.
 * Finds the PluginConfiguration with the highest priority(lowest value of PluginConfiguration#priorityOrder) and associate all the dataFiles to it.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Default plugin of Allocation Strategy",
        id = "DefaultAllocationStrategyPlugin", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class DefaultAllocationStrategyPlugin implements IAllocationStrategy {

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        // first lets get the plugin configuration of type IDataStorage, then lets get only the active ones,
        // eventually order them and choose the one with the highest priority
        PluginConfiguration dataStorageConfToUse = pluginService.getPluginConfigurationsByType(IDataStorage.class)
                .stream().filter(pc -> pc.isActive())
                .sorted(Comparator.comparing(PluginConfiguration::getPriorityOrder)).findFirst()
                .orElseThrow(() -> new MaintenanceException(
                        "There is no active plugin configuration of type IDataStorage"));
        PluginConfiguration dataStorageConfForQuicklook;
        // quicklooks are to be stored online, so lets check if the conf to use is an online data storage, otherwise lets get the first online one
        if (dataStorageConfToUse.getInterfaceNames().contains(IOnlineDataStorage.class)) {
            dataStorageConfForQuicklook = dataStorageConfToUse;
        } else {
            dataStorageConfForQuicklook = pluginService.getPluginConfigurationsByType(IOnlineDataStorage.class).stream()
                    .filter(pc -> pc.isActive()).sorted(Comparator.comparing(PluginConfiguration::getPriorityOrder))
                    .findFirst().orElseThrow(() -> new MaintenanceException(
                            "There is no active plugin configuration of type IOnlineDataStorage to store quicklooks"));
        }
        HashMultimap<Long, StorageDataFile> result = HashMultimap.create(2, dataFilesToHandle.size());

        dataFilesToHandle.forEach(dataFile -> {
            if (dataFile.isQuicklook()) {
                result.put(dataStorageConfForQuicklook.getId(), dataFile);
                return;
            } else {
                result.put(dataStorageConfToUse.getId(), dataFile);
                return;
            }
        });
        return result;
    }

}
