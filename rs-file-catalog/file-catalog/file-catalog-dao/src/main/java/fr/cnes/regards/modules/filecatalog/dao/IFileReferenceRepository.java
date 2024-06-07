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

import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    Set<FileReference> findByLocationStorageAndMetaInfoChecksumIn(String storage, List<String> checksums);

}
