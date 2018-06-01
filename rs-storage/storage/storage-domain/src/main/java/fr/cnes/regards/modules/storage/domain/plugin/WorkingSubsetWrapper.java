package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class WorkingSubsetWrapper<T extends IWorkingSubset> {

    private final Set<T> workingSubsets = new HashSet<>();

    private final Map<StorageDataFile, String> rejectedDataFileMap = Maps.newHashMap();

    /**
     * Method to be used to indicate that a file has been rejected during the call to {@link IDataStorage#prepare(Collection, DataStorageAccessModeEnum)}
     * @param storageDataFile the data file
     * @param reason the reason of the rejection
     */
    public void addRejectedDataFile(StorageDataFile storageDataFile, String reason) {
        rejectedDataFileMap.put(storageDataFile, reason);
    }

    /**
     * Return which storage data file has been rejected for which reason
     * @return which storage data file has been rejected for which reason as a map
     */
    public Map<StorageDataFile, String> getRejectedDataFiles() {
        return rejectedDataFileMap;
    }

    public Set<T> getWorkingSubSets() {
        return workingSubsets;
    }
}
