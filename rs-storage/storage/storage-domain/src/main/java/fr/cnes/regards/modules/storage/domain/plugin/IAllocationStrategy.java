/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;

import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Those plugins are meant to decide which plugin IDataStorage should be use for a given aip and file.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Interface for all AllocationStrategy plugin")
@FunctionalInterface
public interface IAllocationStrategy {

    /**
     * Given some DataFiles, dispatch them to the right DataStorage
     * @param dataFilesToHandle
     * @return Multimap associating DataFiles to their respecting IDataStorage
     */
    Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle);
}
