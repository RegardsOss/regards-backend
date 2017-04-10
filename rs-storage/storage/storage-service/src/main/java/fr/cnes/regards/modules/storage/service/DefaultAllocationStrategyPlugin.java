/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.List;

import javax.validation.constraints.Size;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.AipType;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.plugins.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageType;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "Sylvain VISSIERE-GUERINET", description = "Default plugin of Allocation Strategy", id = "DefaultAllocationStrategyPlugin", version = "1.0", url = "todo", owner = "CSSI", licence = "GPLV3", contact = "regards@c-s.fr")
// FIXME: quelque chose de bizzare
public class DefaultAllocationStrategyPlugin implements IAllocationStrategy {

    @Override
    public IDataStorage getStorages(final AipType pAipType, final FileType pFileType,
            @Size(min = 1, max = 2) final List<IDataStorage> pStoragesAvailable) {
        if (pStoragesAvailable.size() == 1) {
            return pStoragesAvailable.get(0);
        }
        if (pFileType.equals(FileType.RAWDATA)) {
            // OFFLINE
            return pStoragesAvailable.stream().filter(s -> s.getType().equals(DataStorageType.NEARLINE)).findFirst()
                    .get();
        }
        // online
        return pStoragesAvailable.stream().filter(s -> s.getType().equals(DataStorageType.ONLINE)).findFirst().get();
    }

}
