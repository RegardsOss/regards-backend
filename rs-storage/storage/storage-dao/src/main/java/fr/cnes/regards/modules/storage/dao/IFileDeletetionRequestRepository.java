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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;

/**
 * JPA Repository to handle access to {@link FileDeletionRequest} entities.
 *
 * @author Sébatien Binda
 *
 */
public interface IFileDeletetionRequestRepository extends JpaRepository<FileDeletionRequest, Long> {

    Optional<FileDeletionRequest> findByFileReferenceId(Long fileReferenceId);

    Set<FileDeletionRequest> findByFileReferenceIdIn(Set<Long> fileReferenceIds);

    Page<FileDeletionRequest> findByStorage(String storage, Pageable page);

    @Query("select storage from FileDeletionRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    Set<FileDeletionRequest> findByGroupId(String groupId);

    Set<FileDeletionRequest> findByGroupIdAndStatus(String groupId, FileRequestStatus error);

    Page<FileDeletionRequest> findByStatus(FileRequestStatus status, Pageable page);

    Page<FileDeletionRequest> findByStatusAndSessionOwnerAndSession(FileRequestStatus status,
            String sessionOwner, String session, Pageable page);

    Page<FileDeletionRequest> findByStorageAndStatus(String storage, FileRequestStatus status, Pageable page);

    Page<FileDeletionRequest> findByStorageAndStatusAndIdGreaterThan(String storage, FileRequestStatus status,
            Long maxId, Pageable page);

    boolean existsByGroupId(String groupId);

    boolean existsByGroupIdAndStatusNot(String groupId, FileRequestStatus error);

    Long countByStorageAndStatus(String storage, FileRequestStatus status);

    void deleteByStorage(String storageLocationId);

    void deleteByStorageAndStatus(String storageLocationId, FileRequestStatus fileRequestStatus);

    @Modifying
    @Query("update FileDeletionRequest fdr set fdr.status = :status where fdr.id = :id")
    int updateStatus(@Param("status") FileRequestStatus status, @Param("id") Long id);

    @Modifying
    @Query("update FileDeletionRequest fcr set fcr.status = :status, fcr.errorCause = :errorCause where fcr.id = :id")
    int updateError(@Param("status") FileRequestStatus status, @Param("errorCause") String errorCause,
            @Param("id") Long id);

    @Modifying
    @Query("update FileDeletionRequest fdr set fdr.status = :status, fdr.jobId = :jobId where fdr.id = :id")
    int updateStatusAndJobId(@Param("status") FileRequestStatus pending, @Param("jobId") String jobId,
            @Param("id") Long id);

    boolean existsByStorageAndStatusIn(String storage, Collection<FileRequestStatus> status);

    boolean existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(String storage, String checksum,
            Set<FileRequestStatus> ruuninstatus);

    boolean existsByFileReferenceMetaInfoChecksumAndStatusIn(String checksum, Set<FileRequestStatus> ruuninstatus);

    Set<FileDeletionRequest> findByFileReferenceMetaInfoChecksumIn(Set<String> checksums);

    Optional<FileDeletionRequest> findByStorageAndFileReferenceMetaInfoChecksum(String checksum, String storage);

}
