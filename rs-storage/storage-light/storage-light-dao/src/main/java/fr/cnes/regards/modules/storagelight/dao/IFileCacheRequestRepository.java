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
package fr.cnes.regards.modules.storagelight.dao;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;

/**
 * @author sbinda
 *
 */
public interface IFileCacheRequestRepository extends JpaRepository<FileCacheRequest, Long> {

    Page<FileCacheRequest> findByStatus(FileRequestStatus status, Pageable page);

    @Query("select storage from FileCacheRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    Optional<FileCacheRequest> findByChecksum(String checksum);

    Page<FileCacheRequest> findAllByStorage(String storage, Pageable page);

    @Modifying
    @Query("update FileCacheRequest fcr set fcr.status = :status where fcr.id = :id")
    int updateStatus(@Param("status") FileRequestStatus status, @Param("id") Long id);

    @Query("select coalesce(sum(fcr.fileSize),0) from FileCacheRequest fcr where fcr.status = 'PENDING'")
    Long getPendingFileSize();

}
