/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Size;

import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageType;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Default plugin of Allocation Strategy",
        id = "DefaultAllocationStrategyPlugin", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
// FIXME: quelque chose de bizzare
public class DefaultAllocationStrategyPlugin implements IAllocationStrategy {

    @Override
    public IDataStorage getStorage(final EntityType pAipType, final FileType pFileType,
            @Size(min = 1, max = 2) final List<IDataStorage> pStoragesAvailable) {
        //TODO: totally redo this
//        if (pStoragesAvailable.size() == 1) {
//            return pStoragesAvailable.get(0);
//        }
//        if (pFileType.equals(FileType.RAWDATA)) {
//            // OFFLINE
//            return pStoragesAvailable.stream().filter(s -> s.getType().equals(DataStorageType.NEARLINE)).findFirst()
//                    .get();
//        }
//        // online
//        return pStoragesAvailable.stream().filter(s -> s.getType().equals(DataStorageType.ONLINE)).findFirst().get();
        return null;
    }

    @Override
    public Multimap<PluginConfiguration, DataFile> dispatch(Collection<DataFile> dataFilesToHandle) {
        return null;
    }

}
