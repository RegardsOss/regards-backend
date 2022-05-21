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

import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
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
 * JPA Repository to handle access to {@link FileCopyRequest} entities.
 *
 * @author Sébatien Binda
 */
public interface IFileCopyRequestRepository
    extends JpaRepository<FileCopyRequest, Long>, JpaSpecificationExecutor<FileCopyRequest> {

    Page<FileCopyRequest> findByStatus(FileRequestStatus status, Pageable page);

    Page<FileCopyRequest> findByStatusAndIdGreaterThan(FileRequestStatus status, Long id, Pageable page);

    Set<FileCopyRequest> findByMetaInfoChecksum(String checksum);

    Optional<FileCopyRequest> findOneByMetaInfoChecksumAndFileCacheGroupId(String checksum, String groupId);

    Optional<FileCopyRequest> findOneByFileStorageGroupId(String groupId);

    Optional<FileCopyRequest> findOneByMetaInfoChecksumAndStorage(String checksum, String storage);

    boolean existsByGroupId(String groupId);

    Set<FileCopyRequest> findByGroupId(String groupId);

    boolean existsByGroupIdAndStatusNot(String groupId, FileRequestStatus error);

    @Modifying
    @Query("update FileCopyRequest fcr set fcr.status = :status, fcr.errorCause = :errorCause where fcr.id = :id")
    int updateError(@Param("status") FileRequestStatus status,
                    @Param("errorCause") String errorCause,
                    @Param("id") Long id);

    void deleteByStorageAndStatus(String storageLocationId, FileRequestStatus status);

    void deleteByStorage(String storageLocationId);

    boolean existsByStorageAndStatusIn(String storageId, Collection<FileRequestStatus> status);

    boolean existsByMetaInfoChecksumAndStatusIn(String checksum, Collection<FileRequestStatus> status);

    boolean existsByMetaInfoChecksumInAndStatusIn(Collection<String> cheksums,
                                                  Collection<FileRequestStatus> runningStatus);

}
