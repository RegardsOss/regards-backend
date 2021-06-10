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
package fr.cnes.regards.framework.modules.session.manager.dao;

import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Source}
 *
 * @author Iliana Ghazali
 */
@Repository
public interface ISourceManagerRepository extends JpaRepository<Source, Long>, JpaSpecificationExecutor<Source> {

    int MAX_SOURCES_NAMES_RESULTS = 10;

    Optional<Source> findByName(String name);

    void deleteByNbSessions(long noSession);

    void deleteByName(String source);

    @Query(value = "select distinct name from t_source_manager where lower(name) like lower(?1) ORDER BY name LIMIT "
            + "?2", nativeQuery = true)
    Set<String> internalFindAllSourcesNames(String name, int nbResults);

    @Query(value = "select distinct name from t_source_manager ORDER BY name LIMIT ?1", nativeQuery = true)
    Set<String> internalFindAllSourcesNames(int nbResults);

    /**
     * Used to discover sources names using an ilike filter
     *
     * @param name the source name, can be empty
     * @return a subset of all sources names matching
     * @author lmieulet
     */
    default Set<String> findAllSourcesNames(String name) {
        if ((name != null) && !name.isEmpty()) {
            return internalFindAllSourcesNames(name + "%", MAX_SOURCES_NAMES_RESULTS);
        } else {
            return internalFindAllSourcesNames(MAX_SOURCES_NAMES_RESULTS);
        }
    }
}