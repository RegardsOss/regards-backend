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
package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;

/**
 * JPA Interface to access {@link CachedFile}s entities.
 *
 * @author Sylvain VISSIERE-GUERINET
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
     * Get all {@link CachedFile}s for the given {@link String}s of checksums.
     * @param checksums {@link String}s
     * @return {@link CachedFile}s
     */
    List<CachedFile> findAllByChecksumInOrderByLastRequestDateAsc(Set<String> checksums);

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

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState}.
     * @param pQueued {@link CachedFileState}
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findAllByState(CachedFileState pQueued);

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState} ordered by last request date
     * @param pQueued
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByStateOrderByLastRequestDateAsc(CachedFileState pQueued);

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState} ordered by last request date
     * @param pQueued
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByStateOrderByLastRequestDateDesc(CachedFileState pQueued);

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState} and {@link OffsetDateTime} last request date before the given one
     * ordered by last request date.
     * @param pQueued
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByStateAndLastRequestDateBeforeOrderByLastRequestDateAsc(CachedFileState pQueued,
            OffsetDateTime pLastRequestDate);

    /**
     * Retrieve all {@link CachedFile}s with expiration date before the given {@link OffsetDateTime}
     * @param pEpirationDate {@link OffsetDateTime}
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByExpirationBefore(OffsetDateTime pEpirationDate);
}
