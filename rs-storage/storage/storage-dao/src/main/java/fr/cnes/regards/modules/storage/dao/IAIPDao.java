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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

/**
 * DAO to access {@link AIP} entities by requesting {@link AIPEntity}.
 * The {@link AIP} are built from the {@link AIPEntity} with json deserialization.
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public interface IAIPDao {

    /**
     * Create or update an {@link AIP}
     * @param toSave {@link AIP}
     * @param aipSession {@link AIPSession} related AIPSession to this AIP
     * @return saved {@link AIP}
     */
    AIP save(AIP toSave, AIPSession aipSession);

    /**
     * Retrieve all existing {@link AIP}s with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByState(AIPState state, Pageable pageable);

    /**
     * Find {@link AIP} by state in transaction with pessimistic read lock
     * @return a set of products with the above properties
     */
    Page<AIP> findAllWithLockByState(AIPState state, Pageable pageable);

    /**
     * Retrieve a page of aip which state is the one provided and contains at least one of the provided tags and which
     * last event occurred after the given date
     * @param state
     * @param tags
     * @param fromLastUpdateDate
     * @param pageable
     * @return a page of aip which state is the one provided and contains at least one of the provided tags and which
     *         last event occurred after the given date
     */
    Page<AIP> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given starting ipId {@link String}
     * @param aipIdWithoutVersion starting aipId {@link String}
     * @return {@link AIP}s
     */
    Set<AIP> findAllByIpIdStartingWith(String aipIdWithoutVersion);

    /**
     * Retrieve all aips which state is the one given
     * @param state
     * @return aips which state is the requested one
     */
    Page<AIP> findAllByStateService(AIPState state, Pageable page);

    /**
     * Retrieve a single aip according to its ip id
     * @param aipId
     * @return an optional wrapping the aip to avoid nulls
     */
    Optional<AIP> findOneByAipId(String aipId);

    /**
     * Delete all aips from the database
     */
    void deleteAll();

    /**
     * Retrieve all aips which state is one of the provided ones
     * @param states
     * @return aips which state is one of the requested
     */
    Set<AIP> findAllByStateInService(AIPState... states);

    /**
     * Remove the given aip from the database
     * @param aip
     */
    void remove(AIP aip);

    /**
     * Retrieve all aip which ip id is one of the provided ones
     * @param aipIds
     * @return aips which ip id is one of the requested
     */
    Set<AIP> findAllByAipIdIn(Collection<String> aipIds);

    /**
     * Retrieve all aips which are tagged with the given tag
     * @param tag
     * @return aip tagged by tag
     */
    Set<AIP> findAllByTags(String tag);

    /**
     * Retrieve all aips which sip ip id is the given one
     * @param sipId
     * @return aips which sip ip id matches
     */
    Set<AIP> findAllBySipId(String sipId);

    /**
     * Retrieve all aips which state is the one given with lastEventDate above fromLastUpdateDate provided
     * @param state AIP state
     * @param fromLastUpdateDate AIP last update
     * @param pageable
     * @return
     */
    Page<AIP> findAllByStateAndLastEventDateAfter(AIPState state, OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Allow to make a research
     * @param query A SQL query
     * @param pPageable
     * @return
     */
    Page<AIP> findAll(String query, Pageable pPageable);

    /**
     * Count number of {@link AIP} associated to a given session
     * @param sessionId
     * @return number of {@link AIP}
     */
    long countBySessionId(String sessionId);

    /**
     * Count number of {@link AIP} associated to a given session and in a specific given {@link AIPState}
     * @param sessionId session id
     * @return number of {@link AIP}
     */
    long countBySessionIdAndStateIn(String sessionId, Collection<AIPState> states);

    /**
     * Allows to execute some SQL and return a list of string.
     * Used to retrieve entities tags
     * @param query SQL query
     * @return list of string
     */
    List<String> findAllByCustomQuery(String query);

    /**
     * Retrieve all existing IpId from given list
     */
    Stream<UniformResourceName> findUrnsByAipIdIn(Collection<String> aipIds);

    Set<AIP> findAllBySipIdIn(Collection<String> sipIds);

    Page<AIP> findPageBySipIdIn(Collection<String> sipIds, Pageable page);
}
