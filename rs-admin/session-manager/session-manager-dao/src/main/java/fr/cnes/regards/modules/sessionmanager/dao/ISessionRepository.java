/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.sessionmanager.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.sessionmanager.domain.Session;

/**
 * Repository to access {@link Session} entities .
 * @author Léo Mieulet
 */
public interface ISessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    int MAX_SESSION_RESULTS = 10;

    /**
     * Return a session corresponding to search filters
     * We use this function internally to unflag previous session of the same source having isLatest to true
     * Can't use here SessionSpecification, as it uses ilike on string filters
     * @param source the exact session source name
     * @return
     */
    Optional<Session> findOneBySourceAndIsLatestTrue(String source);

    Optional<Session> findOneBySourceAndName(String source, String name);

    Page<Session> findAllBySourceOrderByLastUpdateDateDesc(String source, Pageable page);

    @Modifying
    @Query(value = "update {h-schema}t_session set is_latest=(:isLatest) where source=(:source)", nativeQuery = true)
    int updateSourceSessionsIsLatest(@Param("source") String source, @Param("isLatest") boolean isLatest);

    /**
     * Used to discover session names using an ilike filter
     * @param name the session name, can be empty
     * @return a subset of all session names matching
     */
    default List<String> findAllSessionName(String name) {
        if ((name != null) && !name.isEmpty()) {
            String nameToken = new StringBuilder(name).append("%").toString();
            return internalFindAllSessionName(nameToken, MAX_SESSION_RESULTS);
        }
        return internalFindAllSessionName(MAX_SESSION_RESULTS);
    }

    /**
     * Used to discover session sources using an ilike filter
     * @param source the session source, can be empty
     * @return a subset of all session sources matching
     */
    default List<String> findAllSessionSource(String source) {
        if ((source != null) && !source.isEmpty()) {
            String nameToken = new StringBuilder(source).append("%").toString();
            return internalFindAllSessionSource(nameToken, MAX_SESSION_RESULTS);
        }
        return internalFindAllSessionSource(MAX_SESSION_RESULTS);
    }

    @Query(value = "select DISTINCT name from {h-schema}t_session where lower(name) like lower(:name) ORDER BY name ASC LIMIT :results",
            nativeQuery = true)
    List<String> internalFindAllSessionName(@Param("name") String name, @Param("results") int nbResults);

    @Query(value = "select DISTINCT name from {h-schema}t_session ORDER BY name ASC LIMIT :results", nativeQuery = true)
    List<String> internalFindAllSessionName(@Param("results") int nbResults);

    @Query(value = "select DISTINCT source from {h-schema}t_session where lower(source) like lower(:source) ORDER BY source ASC LIMIT :results",
            nativeQuery = true)
    List<String> internalFindAllSessionSource(@Param("source") String source, @Param("results") int nbResults);

    @Query(value = "select DISTINCT source from {h-schema}t_session ORDER BY source ASC LIMIT :results",
            nativeQuery = true)
    List<String> internalFindAllSessionSource(@Param("results") int nbResults);
}
