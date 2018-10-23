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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * Repository handling JPA representation of AIP.
 * @author Sylvain Vissiere-Guerinet
 */
public interface IAIPEntityRepository extends JpaRepository<AIPEntity, Long> {

    /**
     * Switch state for a given session
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "update {h-schema}t_aip set state = ?1, retry= ?2 where aip_id= ?3", nativeQuery = true)
    void updateAIPStateAndRetry(String state, boolean retry, String aipId);

    /**
     * Find a page of aips which state is the provided one
     * @return a page of aips which state is the provided one
     */
    Page<AIPEntity> findAllByState(AIPState state, Pageable pageable);

    /**
     * Find first 100 entities in specified state
     */
    List<AIPEntity> findFirst100ByState(AIPState state);

    /**
     * Find a page of aips which state is the provided one
     * @return a page of aips which state is the provided one
     */
    Page<AIPEntity> findAllByStateIn(AIPState state, Pageable pageable);

    /**
     * Find all aips which state is one of the provided one
     * @return aips which state is one of the provided one
     */
    Page<AIPEntity> findAllByStateIn(Collection<AIPState> states, Pageable pageable);

    /**
     * Retrieve all aips which ip id starts with the provided string
     * @return aips respecting the constraints
     */
    Page<AIPEntity> findAllByAipIdStartingWith(String urnWithoutVersion, Pageable page);

    /**
     * Retrieve an aip by its ip id
     * @return requested aip
     */
    Optional<AIPEntity> findOneByAipId(String aipId);

    /**
     * Retrieve id by its associated aipId
     */
    @Query(value = "select id from {h-schema}t_aip where aip_id= ?1", nativeQuery = true)
    Optional<Long> findIdByAipId(String aipId);

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
     * @return a aip page
     */
    @Query(value = "select * from {h-schema}t_aip where json_aip->'properties'->'pdi'->'contextInformation'->'tags' @> to_jsonb(?1)  ORDER BY ?#{#pageable}",
            countQuery = "select count(*) from {h-schema}t_aip where json_aip->'properties'->'pdi'->'contextInformation'->'tags' @> to_jsonb(?1)",
            nativeQuery = true)
    Page<AIPEntity> findAllByTag(String tag, Pageable page);

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
