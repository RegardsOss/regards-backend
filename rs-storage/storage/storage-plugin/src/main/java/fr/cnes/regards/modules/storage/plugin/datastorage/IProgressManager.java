/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.net.URL;
import java.nio.file.Path;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

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
     * Notify system that the given {@link DataFile} is stored.
     * @param dataFile {@link DataFile} stored.
     * @param storedUrl {@link URL} new URL of the successfuly stored file.
     */
    public void storageSucceed(DataFile storedDataFile, URL storedUrl, Long storedFileSize);

    /**
     * Notify the system that the given {@link DataFile} couldn't be stored.
     * @param dataFile {@link DataFile} not stored.
     * @param cause {@link String} error message.
     */
    public void storageFailed(DataFile dataFileFailed, String cause);

    /**
     * Notify system that the given {@link DataFile} is deleted.
     * @param dataFile {@link DataFile} deleted.
     */
    public void deletionSucceed(DataFile dataFileDeleted);

    /**
     * Notify the system that the given {@link DataFile} couldn't be deleted.
     * @param dataFile {@link DataFile} not deleted.
     * @param cause {@link String} error message.
     */
    public void deletionFailed(DataFile dataFileFailed, String failureCause);

    /**
     * Notify system that the given {@link DataFile} is restored.
     * @param dataFile {@link DataFile} restored.
     * @param storedUrl {@link Path} of the restored file.
     */
    public void restoreSucceed(DataFile dataFile, Path restoredFilePath);

    /**
     * Notify the system that the given {@link DataFile} couldn't be restored.
     * @param dataFile {@link DataFile} not restored.
     * @param cause {@link String} error message.
     */
    public void restoreFailed(DataFile dataFile, String failureCause);

}
