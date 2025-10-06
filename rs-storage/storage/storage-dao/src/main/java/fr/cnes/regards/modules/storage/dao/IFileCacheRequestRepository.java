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

import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileCacheRequest} entities.
 *
 * @author Sébatien Binda
 */
public interface IFileCacheRequestRepository extends JpaRepository<FileCacheRequest, Long> {

    Page<FileCacheRequest> findByStatus(FileRequestStatus status, Pageable page);

    @Query("select storage from FileCacheRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    Set<FileCacheRequest> findByChecksum(String checksum);

    Page<FileCacheRequest> findAllByStorageAndStatus(String storage, FileRequestStatus status, Pageable page);

    Page<FileCacheRequest> findAllByStorageAndStatusAndIdGreaterThan(String storage,
                                                                     FileRequestStatus status,
                                                                     Long maxId,
                                                                     Pageable page);

    Set<FileCacheRequest> findByGroupIds(String groupId);

    Set<FileCacheRequest> findByGroupIdsAndStatus(String groupId, FileRequestStatus status);

    void deleteByStorage(String storageLocationId);

    void deleteByfileReference(FileReference fileReference);

    void deleteByStorageAndStatus(String storageLocationId, FileRequestStatus status);

    @Deprecated
    @Modifying // the query is executed against the database leaving the persistence context outdated.
    @Query("update FileCacheRequest fcr set fcr.status = :status, fcr.errorCause = :errorCause where fcr.id = :id")
    int updateError(@Param("status") FileRequestStatus status,
                    @Param("errorCause") String errorCause,
                    @Param("id") Long id);

    /**
     * Update the {@link FileCacheRequest} identified by the given id with the given status and error cause.<br/>
     * If no {@link FileCacheRequest} is found, no update is executed.</br>
     * Note: Update query is executed without outdating the persistence context.
     *
     * @param id                of the FileCacheRequest to be updated
     * @param fileRequestStatus new status of the FileCacheRequest
     * @param message           new error cause of the FileCacheRequest
     * @return an Optional of the updated {@link FileCacheRequest} or an empty optional.
     */
    default Optional<FileCacheRequest> updateError(Long id, FileRequestStatus fileRequestStatus, String message) {
        final Optional<FileCacheRequest> optRequest = this.findById(id);
        optRequest.ifPresent(request -> {
            request.setErrorCause(message);
            request.setStatus(fileRequestStatus);
            this.save(request);
        });
        return optRequest;
    }

    @Deprecated
    @Modifying // WARNING: the query is executed against the database leaving the persistence context outdated.
    @Query("update FileCacheRequest fcr set fcr.status = :status, fcr.jobId = :jobId where fcr.id = :id")
    int updateStatusAndJobId(@Param("status") FileRequestStatus pending,
                             @Param("jobId") String jobId,
                             @Param("id") Long id);

    /**
     * Update the {@link FileCacheRequest} identified by the given id with the given the status and job id.<br/>
     * If no {@link FileCacheRequest} is found, no update is executed.</br>
     * Note: Update query is executed without outdating the persistence context.
     *
     * @param id                of the FileCacheRequest to be updated
     * @param fileRequestStatus new status of the FileCacheRequest
     * @param jobId             new job id the FileCacheRequest
     * @return an Optional of the updated {@link FileCacheRequest} or an empty optional.
     */
    default Optional<FileCacheRequest> updateStatusAndJobId(Long id,
                                                            FileRequestStatus fileRequestStatus,
                                                            String jobId) {
        final Optional<FileCacheRequest> optRequest = this.findById(id);
        optRequest.ifPresent(request -> {
            request.setJobId(jobId);
            request.setStatus(fileRequestStatus);
            this.save(request);
        });
        return optRequest;
    }

    @Query("select coalesce(sum(fcr.fileSize),0) from FileCacheRequest fcr where fcr.status = 'PENDING'")
    Long getPendingFileSize();

    @Modifying // WARNING: the query is executed against the database leaving the persistence context outdated.
    @Query(value = "DELETE FROM t_file_cache_request cac WHERE cac.status NOT IN :runningStatuses AND cac.id IN (SELECT grp.file_cache_request_id FROM ta_file_cache_request_group_id grp WHERE grp.group_id = :groupId )",
           nativeQuery = true)
    void deleteByGroupIdsAndStatusNotIn(@Param("groupId") String groupId,
                                        @Param("runningStatuses") List<String> runningStatus);

    boolean existsByChecksumAndStatusIn(String checksum, Set<FileRequestStatus> statuses);
}
