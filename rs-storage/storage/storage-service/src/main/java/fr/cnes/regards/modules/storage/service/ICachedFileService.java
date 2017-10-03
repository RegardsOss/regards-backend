/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

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
    CoupleAvailableError restore(Set<DataFile> nearlineFiles, OffsetDateTime cacheExpirationDate);

    /**
     * Handle a successful restoration of a file from a data storage
     * @param data
     * @param restorationPath
     */
    void handleRestorationSuccess(DataFile data, Path restorationPath);

    /**
     * Handle a failed restoration of a file from a data storage
     * @param data
     */
    void handleRestorationFailure(DataFile data);
}
