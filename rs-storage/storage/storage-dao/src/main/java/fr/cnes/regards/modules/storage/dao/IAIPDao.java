/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     * Update specific state and retry field of existing aip.
     * @param aip
     */
    void updateAIPStateAndRetry(AIP aip);

    /**
     * Retrieve all existing {@link AIP}s with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByState(AIPState state, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given starting ipId {@link String}
     * @param aipIdWithoutVersion starting aipId {@link String}
     * @param page
     * @return {@link AIP}s
     */
    Page<AIP> findAllByIpIdStartingWith(String aipIdWithoutVersion, Pageable page);

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
    Page<AIP> findAllByStateInService(Collection<AIPState> states, Pageable page);

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
    Page<AIP> findAllByTag(String tag, Pageable page);

    /**
     * Retrieve all aips which sip ip id is the given one
     * @param sipId
     * @return aips which sip ip id matches
     */
    Set<AIP> findAllBySipId(String sipId);

    /**
     * Allow to make a research
     */
    Page<AIP> findAll(String query, Pageable pageable);

    /**
     * Count number of {@link AIP} associated to a given session
     * @param sessionId
     * @return number of {@link AIP}
     */
    long countBySessionId(String sessionId);

    long countByQuery(String sqlQuery);

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
