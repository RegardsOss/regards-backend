/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.session.manager.domain.SourceStepAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SourceStepAggregation}
 *
 * @author Iliana Ghazali
 */
@Repository
public interface ISourceManagerStepAggregationRepository extends JpaRepository<SourceStepAggregation, Long> {

    @Modifying
    @Query(value = "delete from t_source_step_aggregation ssa where ssa.id in "
                   + "(select assoc.id from t_source_step_aggregation assoc left join t_source_manager s on assoc.source_name = s.name where s.nb_sessions = ?1)",
           nativeQuery = true)
    void deleteBySourcesNbSessions(long nbSessions);

    default void deleteByEmptySources() {
        deleteBySourcesNbSessions(0L);
    }

}
