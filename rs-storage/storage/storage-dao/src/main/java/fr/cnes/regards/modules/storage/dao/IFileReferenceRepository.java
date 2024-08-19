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

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.StoragePendingFilesAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileReference} entities.
 *
 * @author Sébatien Binda
 */
public interface IFileReferenceRepository
    extends JpaRepository<FileReference, Long>, JpaSpecificationExecutor<FileReference> {

    Page<FileReference> findByLocationStorage(String storage, Pageable page);

    Optional<FileReference> findByLocationStorageAndMetaInfoChecksum(String storage, String checksum);

    Set<FileReference> findByLocationStorageAndMetaInfoChecksumIn(String storage, Collection<String> checksums);

    Page<FileReference> findByLocationStorageAndMetaInfoTypeIn(String storage,
                                                               Collection<String> type,
                                                               Pageable pageable);

    Set<FileReference> findByMetaInfoChecksum(String checksum);

    Set<FileReference> findByLocationUrlIn(Collection<String> urls);

    Set<FileReference> findByMetaInfoChecksumIn(Collection<String> checksums);

    @Query(
        "SELECT fr.location.storage AS storage, sum(fr.metaInfo.fileSize) AS usedSize, count(1) AS numberOfFileReference, max(fr.id) AS lastFileReferenceId"
        + " FROM FileReference fr GROUP BY fr.location.storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation();

    @Query(
        "SELECT fr.location.storage AS storage, sum(fr.metaInfo.fileSize) AS usedSize, count(1) AS numberOfFileReference, max(fr.id) AS lastFileReferenceId"
        + " FROM FileReference fr WHERE fr.id > :id GROUP BY fr.location.storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation(@Param("id") Long FROMFileReferenceId);

    @Query("SELECT fr.location.storage AS storage, count(1) AS numberOfPendingReferences"
           + " FROM FileReference fr WHERE fr.location.pendingActionRemaining = true GROUP BY fr.location.storage")
    Collection<StoragePendingFilesAggregation> getPendingFilesAggregation();

    @Query(value = "INSERT INTO ta_file_reference_owner(file_ref_id,owner) VALUES(:id, :owner)", nativeQuery = true)
    @Modifying
    void addOwner(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "DELETE FROM ta_file_reference_owner WHERE file_ref_id=:id AND owner=:owner", nativeQuery = true)
    @Modifying
    void removeOwner(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "SELECT exists(SELECT 1 FROM ta_file_reference_owner WHERE file_ref_id=:id AND owner=:owner)",
           nativeQuery = true)
    boolean isOwnedBy(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "SELECT owner FROM ta_file_reference_owner WHERE file_ref_id=:id", nativeQuery = true)
    Collection<String> findOwnersById(@Param("id") Long fileRefId);

    @Query(value = "SELECT exists(SELECT 1 FROM ta_file_reference_owner WHERE file_ref_id=:id)", nativeQuery = true)
    boolean hasOwner(@Param("id") Long id);

    FileReference findOneById(Long id);

    Set<FileReference> findByLocationStorageAndLocationPendingActionRemaining(String storage,
                                                                              boolean pendingActionRemaining);

    Set<FileReference> findByLocationPendingActionRemainingAndLocationUrlIn(boolean pendingActionRemaining,
                                                                            Set<String> urls);
}
