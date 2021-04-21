/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageMonitoringAggregation;

/**
 * JPA Repository to handle access to {@link FileReference} entities.
 *
 * @author Sébatien Binda
 *
 */
public interface IFileReferenceRepository
        extends JpaRepository<FileReference, Long>, JpaSpecificationExecutor<FileReference> {

    Page<FileReference> findByLocationStorage(String storage, Pageable page);

    Optional<FileReference> findByLocationStorageAndMetaInfoChecksum(String storage, String checksum);

    Page<FileReference> findByLocationStorageAndMetaInfoTypeIn(String storage, Collection<String> type,
            Pageable pageable);

    Set<FileReference> findByMetaInfoChecksum(String checksum);

    Set<FileReference> findByMetaInfoChecksumIn(Collection<String> checksums);

    @Query("select fr.location.storage as storage, sum(fr.metaInfo.fileSize) as usedSize, count(*) as numberOfFileReference, max(fr.id) as lastFileReferenceId"
            + " from FileReference fr group by storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation();

    @Query("select fr.location.storage as storage, sum(fr.metaInfo.fileSize) as usedSize, count(*) as numberOfFileReference, max(fr.id) as lastFileReferenceId"
            + " from FileReference fr where fr.id > :id group by fr.location.storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation(@Param("id") Long fromFileReferenceId);

    Long countByLocationStorage(String storage);

}
