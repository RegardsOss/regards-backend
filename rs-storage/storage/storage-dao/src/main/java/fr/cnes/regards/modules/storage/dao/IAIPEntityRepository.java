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

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * Repository handling JPA representation of AIP.
 * @author Sylvain Vissiere-Guerinet
 */
public interface IAIPEntityRepository extends JpaRepository<AIPEntity, Long> {

    /**
     * Find a page of aips which state is the provided one
     * @return a page of aips which state is the provided one
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<AIPEntity> findAllWithLockByState(AIPState state, Pageable pageable);

    /**
     * Find a page of aips which state is the provided one
     * @return a page of aips which state is the provided one
     */
    Page<AIPEntity> findAllByStateIn(AIPState state, Pageable pageable);

    /**
     * Find all aips which state is one of the provided one
     * @return aips which state is one of the provided one
     */
    Set<AIPEntity> findAllByStateIn(AIPState... states);

    /**
     * Retrieve a page of aip which state is the one provided and contains the provided tags and which last event
     * occurred after the given date
     * @return a page of aip which state is the one provided and contains the provided tags and which last event
     *         occurred after the given date
     */
    @Query(value = "select * from {h-schema}t_aip where json_aip->'properties'->'pdi'->'contextInformation'->'tags' @> jsonb_build_array(:tags) "
            + "AND state=:state AND date > :lastUpdate " + "ORDER BY aip_id DESC \n-- #pageable\n", nativeQuery = true)
    Page<AIPEntity> findAllByStateAndTagsInAndLastEventDateAfter(@Param("state") String state,
            @Param("tags") Set<String> tags, @Param("lastUpdate") Timestamp fromLastUpdateDate, Pageable pageable);

    default Page<AIPEntity> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable) {
        // use the right converter for OffsetDateTime
        Timestamp date = new OffsetDateTimeAttributeConverter().convertToDatabaseColumn(fromLastUpdateDate);
        return findAllByStateAndTagsInAndLastEventDateAfter(state.getName(), tags, date, pageable);
    }

    /**
     * Retrieve all aips which ip id starts with the provided string
     * @return aips respecting the constraints
     */
    @Query("from AIPEntity aip where aip.aipId LIKE :urnWithoutVersion%")
    Set<AIPEntity> findAllByAipIdStartingWith(@Param("urnWithoutVersion") String urnWithoutVersion);

    /**
     * Retrieve an aip by its ip id
     * @return requested aip
     */
    Optional<AIPEntity> findOneByAipId(String aipId);

    /**
     * Retrieve all aips which ip id is one of the provided ones
     * @return all aips which respects the constraints
     */
    Set<AIPEntity> findAllByAipIdIn(Collection<String> aipIds);

    /**
     * Retrieve all aips which ip id is one of the provided ones
     * No entity graph specified
     * @return a Stream
     */
    Stream<AIPEntity> findByAipIdIn(Collection<String> aipIds);

    /**
     * Retrieve all aips which are tagged by the provided tag
     * @return aips which respects the constraints
     */
    @Query(value = "select * from {h-schema}t_aip where json_aip->'properties'->'pdi'->'contextInformation'->'tags' @> to_jsonb(:tag)",
            nativeQuery = true)
    Set<AIPEntity> findAllByTags(@Param("tag") String tag);

    /**
     * Retrieve all aips which sip id is the provided one
     * @return aips which respects the constraints
     */
    Set<AIPEntity> findAllBySipId(String sipId);

    /**
     * Retrieve page of aips which sip id is the provided one
     * @return a page of aip respecting the constraints
     */
    Page<AIPEntity> findAllBySipId(String sipId, Pageable pageable);

    Page<AIPEntity> findAllByStateAndLastEventDateAfter(AIPState state, OffsetDateTime fromLastUpdateDate,
            Pageable pageable);

    /**
     * Count number of {@link AIPEntity} associated to a given session
     * @param sessionId
     * @return number of {@link AIPEntity}
     */
    long countBySessionId(String sessionId);

    /**
     * Count number of {@link AIPEntity} associated to a given session and in a specific given {@link AIPState}
     * @param sessionId
     * @return number of {@link AIPEntity}
     */
    long countBySessionIdAndStateIn(String sessionId, Collection<AIPState> states);

    Collection<AIPEntity> findAllBySipIdIn(Collection<String> sipIds);

    Page<AIPEntity> findPageBySipIdIn(Collection<String> sipIds, Pageable page);
}
