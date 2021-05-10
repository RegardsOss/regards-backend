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
package fr.cnes.regards.framework.modules.session.management.dao;

import fr.cnes.regards.framework.modules.session.management.domain.Session;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Session}
 * @author Iliana Ghazali
 */
@Repository
public interface ISessionManagerRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    int MAX_SESSION_NAMES_RESULTS = 10;

    Optional<Session> findBySourceAndName(String source, String sessionName);

    Page<Session> findByLastUpdateDateBefore(OffsetDateTime startClean, Pageable pageable);

    void deleteBySourceAndName(String source, String session);

    @Query(value = "select distinct name from t_session_manager where lower(name) like lower(?1) ORDER BY name LIMIT "
            + "?2",
            nativeQuery = true)
    Set<String> internalFindAllSessionsNames(String name, int nbResults);

    @Query(value = "select distinct name from t_session_manager ORDER BY name LIMIT ?1", nativeQuery = true)
    Set<String> internalFindAllSessionsNames(int nbResults);

    /**
     * Used to discover session names using an ilike filter
     * @author lmieulet
     * @param name the session name, can be empty
     * @return a subset of all session names matching
     */
    default Set<String> findAllSessionsNames(String name) {
        if ((name != null) && !name.isEmpty()) {
            return internalFindAllSessionsNames(name + "%", MAX_SESSION_NAMES_RESULTS);
        } else {
            return internalFindAllSessionsNames(MAX_SESSION_NAMES_RESULTS);
        }
    }
}