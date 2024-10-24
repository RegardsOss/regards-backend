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
package fr.cnes.regards.modules.file.packager.dao;

import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository for {@link PackageReference}
 *
 * @author Thibaud Michaudel
 **/
public interface PackageReferenceRepository extends JpaRepository<PackageReference, Long> {

    Optional<PackageReference> findOneByStorageAndStorageSubdirectoryAndStatus(String storage,
                                                                               String storePath,
                                                                               PackageReferenceStatus status);

    /**
     * Set given status to all packages older than the given date.
     */
    @Modifying
    @Query("UPDATE PackageReference p SET p.status = :status WHERE p.creationDate < :oldestDate")
    void updateOldPackagesStatus(@Param("oldestDate") OffsetDateTime oldestDate,
                                 @Param("status") PackageReferenceStatus status);

    /**
     * Set status {@link PackageReferenceStatus#TO_STORE} to all packages older than the given date.
     */
    default void closeAllOldPackages(OffsetDateTime oldestDate) {
        updateOldPackagesStatus(oldestDate, PackageReferenceStatus.TO_STORE);
    }

    /**
     * Set given checksum to the package with the given id
     */
    @Modifying
    @Query("UPDATE PackageReference p SET p.checksum = :checksum WHERE p.id = :id")
    void updatePackageChecksum(@Param("id") Long id, @Param("checksum") String checksum);

    /**
     * Set given error and status to the package with the given id
     */
    @Modifying
    @Query("UPDATE PackageReference p SET p.status = :status, p.errorCause = :error WHERE p.id = :id")
    void updatePackageStatus(@Param("id") Long id,
                             @Param("error") String error,
                             @Param("status") PackageReferenceStatus status);

    /**
     * Set given error and {@link PackageReferenceStatus#STORE_ERROR} to the package with the given id
     */
    default void updatePackageError(Long id, String error) {
        updatePackageStatus(id, error, PackageReferenceStatus.STORE_ERROR);
    }
}
