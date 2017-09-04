/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;

/**
 * Those plugins are meant to decide which plugin IDataStorage should be use for a given aip and file.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Interface for all AllocationStrategy plugin")
public interface IAllocationStrategy {

    IDataStorage getStorage(EntityType pAipType, FileType pFileType, List<IDataStorage> pStoragesAvailable);

    Multimap<PluginConfiguration, DataFile> dispatch(Collection<DataFile> dataFilesToHandle);
}
