/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.AipType;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.plugins.datastorage.IDataStorage;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Interface for all AllocationStrategy plugin")
public interface IAllocationStrategy {

    IDataStorage getStorages(AipType pAipType, FileType pFileType, List<IDataStorage> pStoragesAvailable);

}
