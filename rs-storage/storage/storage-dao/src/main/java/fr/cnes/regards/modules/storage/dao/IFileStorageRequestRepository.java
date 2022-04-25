/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileStorageRequest} entities.
 *
 * @author Sébatien Binda
 *
 */
public interface IFileStorageRequestRepository extends JpaRepository<FileStorageRequest, Long> {

    Page<FileStorageRequest> findByStorage(String storage, Pageable pageable);

    Set<FileStorageRequest> findByMetaInfoChecksumAndStorage(String checksum, String storage);

    Page<FileStorageRequest> findAllByStorage(String storage, Pageable page);

    Page<FileStorageRequest> findAllByStorageAndOwnersIn(String storage, Collection<String> owners, Pageable page);

    Set<FileStorageRequest> findByGroupIds(String groupId);

    Set<String> findGroupIdsByGroupIdsIn(Collection<String> groupId);

    Set<FileStorageRequest> findByGroupIdsAndStatus(String groupId, FileRequestStatus error);

    Page<FileStorageRequest> findByOwnersInAndStatus(Collection<String> owners, FileRequestStatus error, Pageable page);

    Page<FileStorageRequest> findAllByStorageAndStatus(String storage, FileRequestStatus status, Pageable page);

    Page<FileStorageRequest> findAllByStorageAndStatusAndIdGreaterThan(String storage, FileRequestStatus status,
            Long id, Pageable page);

    Page<FileStorageRequest> findAllByStorageAndStatusAndOwnersInAndIdGreaterThan(String storage,
            FileRequestStatus status, Collection<String> owners, Long id, Pageable page);

    @Query("select storage from FileStorageRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    @Modifying
    @Query("update FileStorageRequest fcr set fcr.status = :status, fcr.errorCause = :errorCause where fcr.id = :id")
    int updateError(@Param("status") FileRequestStatus status, @Param("errorCause") String errorCause,
            @Param("id") Long id);

    @Modifying
    @Query("update FileStorageRequest fsr set fsr.status = :status, fsr.jobId = :jobId where fsr.id = :id")
    int updateStatusAndJobId(@Param("status") FileRequestStatus status, @Param("jobId") String jobId,
            @Param("id") Long id);

    void deleteByStorage(String storageLocationId);

    void deleteByStorageAndStatus(String storageLocationId, FileRequestStatus fileRequestStatus);

    boolean existsByGroupIdsAndStatusNot(String groupId, FileRequestStatus error);

    Long countByStorageAndStatus(String storage, FileRequestStatus status);

    boolean existsByStorageAndStatusIn(String storageId, Set<FileRequestStatus> status);

    Page<FileStorageRequest> findByStatus(FileRequestStatus delayed, Pageable page);

    Page<FileStorageRequest> findByStatusAndSessionOwnerAndSession(FileRequestStatus error, String source, String session, Pageable pageToRequest);

    boolean existsByStorageAndMetaInfoChecksumAndStatusIn(String storage, String checksum,
            Set<FileRequestStatus> ruuningStatus);

    Set<FileStorageRequest> findByMetaInfoChecksumIn(Set<String> checksums);

    Optional<FileStorageRequest> findByMetaInfoChecksum(String checksum);
}
