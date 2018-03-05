package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;

/**
 * Default multiple allocation strategy, allocate each file to all specified data storage.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Allocation Strategy plugin that allocates files to multiple data storage",
        id = "DefaultMultipleAllocationStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class DefaultMultipleAllocationStrategy implements IAllocationStrategy {

    public static final String DATA_STORAGE_IDS_PARAMETER_NAME = "DATA_STORAGE_IDS";

    @PluginParameter(name = DATA_STORAGE_IDS_PARAMETER_NAME,
            description = "Ids of data storage configuration on which files will ALL be stored. Don't forget to include at least one ONLINE data storage for quicklooks",
            label = "data storage ids")
    private Set<Long> dataStorageIds;

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        Multimap<Long, StorageDataFile> dispatched = HashMultimap
                .create(dataStorageIds.size(), dataFilesToHandle.size());
        for (Long dataStorageId : dataStorageIds) {
            for (StorageDataFile dataFile : dataFilesToHandle) {
                dataFile.increaseNotYetStoredBy();
                dispatched.put(dataStorageId, dataFile);
            }
        }
        return dispatched;
    }
}
