/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.modules.storagelight.domain.database.CachedFile;

/**
 * JPA Interface to access {@link CachedFile}s entities.
 *
 * @author Sébastien Binda
 */
public interface ICachedFileRepository extends JpaRepository<CachedFile, Long> {

    /**
     * Get all {@link CachedFile}s for the given {@link String}s of checksums.
     * @param checksums {@link String}s
     * @return {@link CachedFile}s
     */
    Set<CachedFile> findAllByChecksumIn(Set<String> checksums);

    /**
     * Retrieve a {@link CachedFile} by his checksum
     * @param checksum
     * @return {@link Optional} {@link CachedFile}
     */
    Optional<CachedFile> findOneByChecksum(String checksum);

    /**
     * Remove a {@link CachedFile} by his checksum.
     * @param checksum {@link String}
     */
    void removeByChecksum(String checksum);

    @Query("select coalesce(sum(cf.fileSize), 0) from CachedFile cf")
    Long getTotalFileSize();

    /**
     * Retrieve all {@link CachedFile}s with expiration date before the given {@link OffsetDateTime}
     * @param pEpirationDate {@link OffsetDateTime}
     * @return {@link Set}<{@link CachedFile}
     */
    Page<CachedFile> findByExpirationBefore(OffsetDateTime pEpirationDate, Pageable pageable);
}
