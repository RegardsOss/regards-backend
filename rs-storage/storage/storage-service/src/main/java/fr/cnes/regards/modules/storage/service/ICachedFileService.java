/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
     * Asynchronous job scheduling. Allows us to speed up process & keep same logic.
     * Method used to set the tenant for the transaction manager for {@link #doScheduleRestorationAsync(OffsetDateTime, Set)} call
     * @param cacheExpirationDate
     * @param toRetrieve
     * @param tenant
     */
    void scheduleRestorationAsync(OffsetDateTime cacheExpirationDate, Set<StorageDataFile> toRetrieve, String tenant);

    /**
     * Actually do job scheduling
     * @param cacheExpirationDate
     * @param toRetrieve
     */
    void doScheduleRestorationAsync(OffsetDateTime cacheExpirationDate, Set<StorageDataFile> toRetrieve);

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
     * @return number of purged files
     */
    int purge();

    /**
     * Restore all files waiting for restoring
     * @return number of restoration scheduled
     */
    int restoreQueued();

    void processEvent(TenantConnectionReady event);
}
