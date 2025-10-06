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
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FileReferenceRequestAggregation} entities.
 *
 * @author Olivier Navarro
 */
public interface IFileReferenceRequestRepository extends JpaRepository<FileReferenceRequestAggregation, Long> {

    Set<FileReferenceRequestAggregation> findByMetaInfoChecksumAndStorage(String checksum, String storage);

    Set<FileReferenceRequestAggregation> findByGroupIds(String groupId);

    Page<FileReferenceRequestAggregation> findAllByStorageAndStatusAndIdGreaterThan(String storage,
                                                                                    FileRequestStatus status,
                                                                                    Long id,
                                                                                    Pageable page);

    Page<FileReferenceRequestAggregation> findAllByStorageAndStatusAndOwnersInAndIdGreaterThan(String storage,
                                                                                               FileRequestStatus status,
                                                                                               Collection<String> owners,
                                                                                               Long id,
                                                                                               Pageable page);

    @Query("select storage from FileReferenceRequestAggregation where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    default Optional<FileReferenceRequestAggregation> updateStatusAndJobId(Long id,
                                                                           FileRequestStatus fileRequestStatus,
                                                                           String jobId) {
        final Optional<FileReferenceRequestAggregation> optRequest = this.findById(id);
        optRequest.ifPresent(request -> {
            request.setJobId(jobId);
            request.setStatus(fileRequestStatus);
            this.save(request);
        });
        return optRequest;
    }

    default Optional<FileReferenceRequestAggregation> updateStatus(Long id, FileRequestStatus status) {
        final Optional<FileReferenceRequestAggregation> optRequest = this.findById(id);
        optRequest.ifPresent(request -> {
            request.setStatus(status);
            this.save(request);
        });
        return optRequest;
    }

    default Optional<FileReferenceRequestAggregation> updateError(Long id,
                                                                  FileRequestStatus fileRequestStatus,
                                                                  String message) {
        final Optional<FileReferenceRequestAggregation> optRequest = this.findById(id);
        optRequest.ifPresent(request -> {
            request.setErrorCause(message);
            request.setStatus(fileRequestStatus);
            this.save(request);
        });
        return optRequest;
    }

    Page<FileReferenceRequestAggregation> findByStatus(FileRequestStatus status, Pageable page);

    boolean existsByStorageAndMetaInfoChecksumAndStatusIn(String storage,
                                                          String checksum,
                                                          Set<FileRequestStatus> statuses);

    Optional<FileReferenceRequestAggregation> findByStorageAndMetaInfoChecksum(String storage, String checksum);

    List<FileReferenceRequestAggregation> findByGroupIdsAndStatusNotIn(String groupId,
                                                                       Collection<FileRequestStatus> statuses);
}
