/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
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
    Set<CachedFile> findByState(CachedFileState pQueued);

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState} ordered by last request date
     * @param pQueued
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByStateOrderByLastRequestDateAsc(CachedFileState pQueued);

    /**
     * Retrieve all {@link CachedFile}s for the given {@link CachedFileState} and {@link OffsetDateTime} last request date before the given one
     * ordered by last request date.
     * @param pQueued
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByStateAndLastRequestDateBeforeOrderByLastRequestDateAsc(CachedFileState pQueued, OffsetDateTime pLastRequestDate);

    /**
     * Retrieve all {@link CachedFile}s with expiration date before the given {@link OffsetDateTime}
     * @param pEpirationDate {@link OffsetDateTime}
     * @return {@link Set}<{@link CachedFile}
     */
    Set<CachedFile> findByExpirationBefore(OffsetDateTime pEpirationDate);
}
