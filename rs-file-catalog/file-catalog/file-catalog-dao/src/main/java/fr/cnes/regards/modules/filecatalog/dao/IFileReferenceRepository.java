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

import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
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
 * @author Thibaud Michaudel
 */
public interface IFileReferenceRepository
    extends JpaRepository<FileReference, Long>, JpaSpecificationExecutor<FileReference> {

    @Query(value = "INSERT INTO ta_file_reference_owner(file_ref_id,owner) VALUES(:id, :owner)", nativeQuery = true)
    @Modifying
    void addOwner(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "delete from ta_file_reference_owner where file_ref_id=:id and owner=:owner", nativeQuery = true)
    @Modifying
    void removeOwner(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "select exists(select 1 from ta_file_reference_owner where file_ref_id=:id and owner=:owner)",
           nativeQuery = true)
    boolean isOwnedBy(@Param("id") Long id, @Param("owner") String owner);

    @Query(value = "select exists(select 1 from ta_file_reference_owner where file_ref_id=:id)", nativeQuery = true)
    boolean hasOwner(@Param("id") Long id);

    Page<FileReference> findByLocationStorage(String storage, Pageable page);

    Set<FileReference> findByLocationStorageAndMetaInfoChecksumIn(String storage, Collection<String> checksums);

    @Query(value = "SELECT owner FROM ta_file_reference_owner WHERE file_ref_id=:id", nativeQuery = true)
    Collection<String> findOwnersById(@Param("id") Long fileRefId);

    Optional<FileReference> findByLocationStorageAndMetaInfoChecksum(String storage, String checksum);

    Page<FileReference> findByLocationStorageAndMetaInfoTypeIn(String storage,
                                                               Collection<String> type,
                                                               Pageable pageable);

    Set<FileReference> findByMetaInfoChecksumIn(Collection<String> checksums);

    Set<FileReference> findByLocationUrlIn(Collection<String> urls);

    Set<FileReference> findByMetaInfoChecksum(String checksum);

    Set<FileReference> findByLocationStorageAndLocationFileArchiveStatus(String storage,
                                                                         FileArchiveStatus fileArchiveStatus);
    
    Set<FileReference> findByLocationFileArchiveStatusAndLocationUrlIn(FileArchiveStatus fileArchiveStatus,
                                                                       Set<String> urls);
}
