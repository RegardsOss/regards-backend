/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * This interface defines operations on BatchEntities in the database.
 *
 * @author gandrieu
 */
@InstanceEntity
@Repository
public interface IBatchEntityRepository extends ReactiveCrudRepository<BatchEntity, UUID> {

    /**
     * We look for executions whose last recorded step is RUNNING, and its difference between recording time
     * and now is greater than the duration declared in the corresponding execution.
     */
    // @formatter:off
    @Query( "WITH counts AS ( " +
            "  SELECT " +
            "    E.batch_id AS batch_id, " +
            "    COUNT(*) AS count_all_execs, " +
            "    COUNT(*) FILTER ( " +
            "      WHERE current_status IN ('FAILURE', 'SUCCESS', 'TIMED_OUT', 'CANCELLED') " +
            "      AND  EXTRACT(EPOCH FROM now()) - EXTRACT(EPOCH FROM E.last_updated) > (:tooOldDuration / 1000) " +
            "    ) AS count_finished_execs " +
            "  FROM public.t_execution as E " +
            "  GROUP BY E.batch_id " +
            ") " +
            "SELECT * FROM public.t_batch AS B " +
            "LEFT JOIN counts ON B.id = counts.batch_id " +
            "WHERE counts.count_all_execs IS NULL" +
            "   OR counts.count_all_execs = counts.count_finished_execs"
    )
    Flux<BatchEntity> getCleanableBatches(long tooOldDuration);

    Flux<BatchEntity> findByProcessBusinessId(UUID processBusinessId);
    // @formatter:on
}
