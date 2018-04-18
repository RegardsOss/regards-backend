/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

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
     * @param storedDataFile {@link StorageDataFile} stored.
     * @param storedUrl {@link URL} new URL of the successfully stored file.
     * @param storedFileSize file size fo the stored file
     * @param sourceUrls former urls of the data file,
     */
    public void storageSucceed(StorageDataFile storedDataFile, URL storedUrl, Long storedFileSize);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be stored.
     * @param dataFileFailed {@link StorageDataFile} not stored.
     * @param failedUrl url storage error
     * @param cause {@link String} error message.
     */
    public void storageFailed(StorageDataFile dataFileFailed, Optional<URL> failedUrl, String cause);

    /**
     * Notify system that the given {@link StorageDataFile} is deleted.
     * @param dataFileDeleted {@link StorageDataFile} deleted.
     * @param deletedUrl deleted url
     */
    public void deletionSucceed(StorageDataFile dataFileDeleted, URL deletedUrl);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be deleted.
     * @param dataFileFailed {@link StorageDataFile} not deleted.
     * @param failedUrl url deletion error
     * @param failureCause {@link String} error message.
     */
    public void deletionFailed(StorageDataFile dataFileFailed, Optional<URL> failedUrl, String failureCause);

    /**
     * Notify system that the given {@link StorageDataFile} is restored.
     * @param dataFile {@link StorageDataFile} restored.
     * @param restoredFromUrl url restoration
     * @param restoredFilePath {@link Path} of the restored file.
     */
    public void restoreSucceed(StorageDataFile dataFile, URL restoredFromUrl, Path restoredFilePath);

    /**
     * Notify the system that the given {@link StorageDataFile} couldn't be restored.
     * @param dataFile {@link StorageDataFile} not restored.
     * @param failedUrl url restoration error
     * @param failureCause {@link String} error message.
     */
    public void restoreFailed(StorageDataFile dataFile, Optional<URL> failedUrl, String failureCause);

}
