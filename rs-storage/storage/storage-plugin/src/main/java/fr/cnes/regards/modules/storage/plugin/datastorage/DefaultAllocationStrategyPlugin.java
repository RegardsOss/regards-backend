/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.util.Collection;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

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

    @Autowired
    private IPluginService pluginService;

    @Override
    public Multimap<PluginConfiguration, DataFile> dispatch(Collection<DataFile> dataFilesToHandle) {
        //first lets get the plugin configuration of type IDataStorage, then lets get only the active ones,
        // eventually order them and choose the one with the highest priority
        PluginConfiguration dataStorageConfToUse = pluginService.getPluginConfigurationsByType(IDataStorage.class)
                .stream().filter(pc -> pc.isActive())
                .sorted(Comparator.comparing(PluginConfiguration::getPriorityOrder)).findFirst().orElseThrow(
                        () -> new MaintenanceException("There is no active plugin configuration of type IDataStorage"));
        HashMultimap<PluginConfiguration, DataFile> result = HashMultimap.create(1, dataFilesToHandle.size());
        result.putAll(dataStorageConfToUse, dataFilesToHandle);
        return result;
    }

}
