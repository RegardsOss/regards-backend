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

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * DAO to access {@link AIP} entities by requesting {@link AIPEntity}.
 * The {@link AIP} are built from the {@link AIPEntity} with json deserialization.
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public interface IAIPDao {

    /**
     * Create or update an {@link AIP}
     * @param toSave     {@link AIP}
     * @param aipSession {@link AIPSession} related AIPSession to this AIP
     * @return saved {@link AIP}
     */
    AIP save(AIP toSave, AIPSession aipSession);

    /**
     * Retrieve all existing {@link AIP}s with given {@link AIPState} state.
     * @param state    {@link AIPState} state requested.
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
     * last event occurred after the given date
     */
    Page<AIP> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Retrieve a page of aip which state is the one provided and contains at least one of the provided tags
     * @param state
     * @param tags
     * @param pageable
     * @return a page of aip which state is the one provided and contains at least one of the provided tags
     */
    Page<AIP> findAllByStateAndTagsIn(AIPState state, Set<String> tags, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given starting ipId {@link String}
     * @param ipIdWithoutVersion starting ipId {@link String}
     * @return {@link AIP}s
     */
    Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion);

    /**
     * Retrieve all aips which state is the one given
     * @param state
     * @return aips which state is the requested one
     */
    Set<AIP> findAllByStateService(AIPState state);

    /**
     * Retrieve a single aip according to its ip id
     * @param ipId
     * @return an optional wrapping the aip to avoid nulls
     */
    Optional<AIP> findOneByIpId(String ipId);

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
     * @param ipIds
     * @return aips which ip id is one of the requested
     */
    Set<AIP> findAllByIpIdIn(Collection<String> ipIds);

    /**
     * Retrieve all aips which are tagged with the given tag
     * @param tag
     * @return aip tagged by tag
     */
    Set<AIP> findAllByTags(String tag);

    /**
     * Retrieve all aips which sip ip id is the given one
     * @param sipIpId
     * @return aips which sip ip id matches
     */
    Set<AIP> findAllBySipId(String sipIpId);

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
     * @param query     A query specification
     * @param pPageable
     * @return
     */
    Page<AIP> findAll(Specification<AIPEntity> query, Pageable pPageable);

    /**
     * Retrieve all aips
     * @return aips
     */
    Set<AIP> findAll(Specification<AIPEntity> query);

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
    Stream<UniformResourceName> findUrnsByIpIdIn(Collection<String> ipIds);

}
