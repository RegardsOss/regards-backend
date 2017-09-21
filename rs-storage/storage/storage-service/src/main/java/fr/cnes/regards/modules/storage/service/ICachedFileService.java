package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * Service managing the file cache system.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ICachedFileService {

    /**
     * schedule job to restore files from nearline data storages if needed
     * @param nearlineFiles files to be restored
     * @param cacheExpirationDate date until which files should be kept into the cache
     * @return already available or in error files
     */
    CoupleAvailableError restore(Set<DataFile> nearlineFiles, OffsetDateTime cacheExpirationDate);

    /**
     * handle a successful restoration of a file from a data storage
     * @param data
     * @param restorationPath
     */
    void handleRestorationSuccess(DataFile data, Path restorationPath);

    /**
     * handle a failed restoration of a file from a data storage
     * @param data
     */
    void handleRestorationFailure(DataFile data);
}
