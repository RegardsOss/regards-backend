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
package fr.cnes.regards.modules.filecatalog.dao;

import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.filecatalog.dao.result.RequestAndMaxStatus;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileStorageRequestAggregation} entities.
 *
 * @author Thibaud Michaudel
 **/
public interface IFileStorageRequestAggregationRepository extends JpaRepository<FileStorageRequestAggregation, Long> {

    boolean existsByStorageAndMetaInfoChecksumAndStatusIn(String storage,
                                                          String checksum,
                                                          Set<StorageRequestStatus> runningStatus);

    Page<FileStorageRequestAggregation> findByStatus(StorageRequestStatus delayed, Pageable page);

    @Query("SELECT storage FROM FileStorageRequestAggregation WHERE status = :status")
    Set<String> findStoragesByStatus(@Param("status") StorageRequestStatus status);

    Page<FileStorageRequestAggregation> findAllByStorageAndStatus(String storage,
                                                                  StorageRequestStatus status,
                                                                  Pageable page);

    Page<FileStorageRequestAggregation> findAllByStorageAndStatusAndMetaInfoChecksumIn(String storage,
                                                                                       StorageRequestStatus status,
                                                                                       List<String> checksum,
                                                                                       Pageable page);

    Page<FileStorageRequestAggregation> findAllByStatusOrderByStorageAsc(StorageRequestStatus status, Pageable page);

    Page<FileStorageRequestAggregation> findAllByStatusOrderByStorageAscMetaInfoChecksumAsc(StorageRequestStatus status,
                                                                                            Pageable page);

    /**
     * This request searches for storage requests matching the following conditions:
     * <ul>
     * <li>The storage is the given one.</li>
     * <li>The request is not in {@link StorageRequestStatus#GRANTED GRANTED} status.</li>
     * </ul>
     * <p>
     * Regroup all these requests by checksum and for each group, if one of the requests is in status
     * {@link StorageRequestStatus#TO_HANDLE TO_HANDLE}, return the checksum and the maximum status of the group
     * according to the status ordinal. In nominal cases, this will be either {@link StorageRequestStatus#TO_HANDLE
     * TO_HANDLE}, {@link StorageRequestStatus#HANDLED HANDLED}, or {@link StorageRequestStatus#TO_DELETE TO_DELETE}.
     */
    @Query(
        "SELECT new fr.cnes.regards.modules.filecatalog.dao.result.RequestAndMaxStatus(t.metaInfo.checksum, MAX(t.status)) "
        + "FROM FileStorageRequestAggregation t  "
        + "WHERE t.storage = :storage AND t.status>0 "
        + "GROUP BY t.metaInfo.checksum "
        + "HAVING MIN(t.status) < 2 ")
    Page<RequestAndMaxStatus> findRequestChecksumToHandle(@Param("storage") String storage, Pageable page);

    @Query("UPDATE FileStorageRequestAggregation fr SET fr.status = :status WHERE fr.id IN :ids")
    @Modifying
    void updateStatusByIdIn(@Param("status") StorageRequestStatus status, @Param("ids") List<Long> ids);

    @Query("UPDATE FileStorageRequestAggregation fr SET fr.status = :status WHERE fr.storage = :storage AND "
           + "fr.metaInfo.checksum IN :checksums")
    @Modifying
    void updateStatusByStorageAndMetaInfoChecksumIn(@Param("status") StorageRequestStatus status,
                                                    @Param("storage") String storage,
                                                    @Param("checksums") List<String> checksums);

    @Query(value = "DELETE FROM t_file_storage_request WHERE status = :#{#status.ordinal()}", nativeQuery = true)
    @Modifying
    void deleteAllByStatus(@Param("status") StorageRequestStatus status);

}
