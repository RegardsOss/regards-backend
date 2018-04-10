/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Interface for {@link CachedFile}s entities management.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public interface ICachedFileService {

    /**
     * Schedule job to restore files from nearline data storages if needed
     * @param nearlineFiles files to be restored
     * @param cacheExpirationDate date until which files should be kept into the cache
     * @return already available or in error files
     */
    CoupleAvailableError restore(Set<StorageDataFile> nearlineFiles, OffsetDateTime cacheExpirationDate);

    /**
     * Handle a successful restoration of a file from a data storage
     * @param data
     * @param restorationPath
     */
    void handleRestorationSuccess(StorageDataFile data, Path restorationPath);

    /**
     * Handle a failed restoration of a file from a data storage
     * @param data
     */
    void handleRestorationFailure(StorageDataFile data);

    /**
     * Retrieve a {@link CachedFile} if exists and is AVAILABLED.
     * @param pChecksum Checksum of the requested file.
     * @return {@link CachedFile} or empty if it does not exists or is not available.
     */
    Optional<CachedFile> getAvailableCachedFile(String pChecksum);

    /**
     * Purge cache
     */
    void purge();

    /**
     * Restore all files waiting for restoring
     */
    void restoreQueued();

    void processEvent(TenantConnectionReady event);
}
