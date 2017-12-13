/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import java.net.URL;
import java.nio.file.Path;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * The ProgressManager is used by {@link INearlineDataStorage} plugins to notidy the upper service of storage action results :
 * <ul>
 * <li>Storage succeed {@link #storageSucceed}</li>
 * <li>Storage failed {@link #storageFailed}</li>
 * <li>Restoration succeed {@link #restoreSucceed}</li>
 * <li>Restoration failed {@link #restoreFailed}</li>
 * <li>Deletion succeed {@link #deletionSucceed}</li>
 * <li>Deletion failed {@link #deletionFailed}</li>
 * </ul>
 * @author Sébastien Binda
 */
public interface IProgressManager {

    /**
     * Notify system that the given {@link StorageDataFile} is stored.
     * @param dataFile {@link StorageDataFile} stored.
     * @param storedUrl {@link URL} new URL of the successfuly stored file.
     */
    public void storageSucceed(StorageDataFile storedDataFile, URL storedUrl, Long storedFileSize);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be stored.
     * @param dataFile {@link StorageDataFile} not stored.
     * @param cause {@link String} error message.
     */
    public void storageFailed(StorageDataFile dataFileFailed, String cause);

    /**
     * Notify system that the given {@link StorageDataFile} is deleted.
     * @param dataFile {@link StorageDataFile} deleted.
     */
    public void deletionSucceed(StorageDataFile dataFileDeleted);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be deleted.
     * @param dataFile {@link StorageDataFile} not deleted.
     * @param cause {@link String} error message.
     */
    public void deletionFailed(StorageDataFile dataFileFailed, String failureCause);

    /**
     * Notify system that the given {@link StorageDataFile} is restored.
     * @param dataFile {@link StorageDataFile} restored.
     * @param storedUrl {@link Path} of the restored file.
     */
    public void restoreSucceed(StorageDataFile dataFile, Path restoredFilePath);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be restored.
     * @param dataFile {@link StorageDataFile} not restored.
     * @param cause {@link String} error message.
     */
    public void restoreFailed(StorageDataFile dataFile, String failureCause);

}
