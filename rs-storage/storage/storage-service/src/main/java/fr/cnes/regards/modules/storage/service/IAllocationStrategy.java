/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.plugins.datastorage.IDataStorage;

/**
 * Those plugins are meant to decide which plugin IDataStorage should be use for a given aip and file.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Interface for all AllocationStrategy plugin")
public interface IAllocationStrategy {

    IDataStorage getStorage(EntityType pAipType, FileType pFileType, List<IDataStorage> pStoragesAvailable);

}
