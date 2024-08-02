/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.modules.storage.domain.database.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link StorageLocation} entities.
 *
 * @author Sébatien Binda
 */
public interface IStorageLocationRepository
    extends JpaRepository<StorageLocation, Long>, JpaSpecificationExecutor<StorageLocation> {

    Optional<StorageLocation> findByName(String name);

    Set<StorageLocation> findByNumberOfPendingFilesGreaterThan(Long minimumNumberOfPendingFiles);

    default Set<StorageLocation> findStorageWithPendingActionRemaining() {
        return findByNumberOfPendingFilesGreaterThanOrPendingActionRemaining(0L, true);
    }

    Set<StorageLocation> findByNumberOfPendingFilesGreaterThanOrPendingActionRemaining(Long minimumNumberOfPendingFiles,
                                                                                       boolean pendingActionRemaining);

    void deleteByName(String name);

    void deleteByPendingActionRemainingFalse();

    /**
     * Reinitialize all storage location by :<ul>
     * <li>Delete all location without pending remaining action</li>
     * <li>Reset to 0 location with pending remaining action</li>
     * </ul>
     */
    default void resetAll(OffsetDateTime resetDate) {
        // First delete all location without any remaining information
        deleteByPendingActionRemainingFalse();
        // Then reset all other ones to initial state and keeping information about global remaining pending action.
        // The remaining pending action does not rely on file reference associated so if if value is true, a pending
        // action needs to be run and we cannot lose this information.
        findAll().forEach(loc -> {
            loc.setLastUpdateDate(resetDate);
            loc.setNumberOfPendingFiles(0L);
            loc.setNumberOfReferencedFiles(0L);
            loc.setTotalSizeOfReferencedFilesInKo(0L);
        });
    }
}
