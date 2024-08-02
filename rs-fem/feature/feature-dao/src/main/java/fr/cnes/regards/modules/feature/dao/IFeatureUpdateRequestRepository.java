/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.IFeatureRequestToSchedule;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Repository
public interface IFeatureUpdateRequestRepository extends IAbstractFeatureRequestRepository<FeatureUpdateRequest> {

    Set<FeatureUpdateRequest> findByGroupIdIn(Set<String> groupIds);

    /**
     * Retrieve update requests to process sorted by request date.<br/>
     * Sorting requests is useful to manage several update requests on a same target entity!
     */
    List<FeatureUpdateRequest> findAllByIdInOrderByRequestDateAsc(Set<Long> ids);

    /**
     * Get {@link ILightFeatureUpdateRequest} with a {@link Feature} urn not assigned to an other {@link ILightFeatureUpdateRequest}
     * or to an existing {@link ILightFeatureCreationRequest} and with it step set to LOCAL_SCHEDULED an ordered
     * by registration date and before a delay
     */

    /**
     * Get a limited number of {@link ILightFeatureUpdateRequest} ready to be scheduled ordered by priority and date.
     * {@link ILightFeatureUpdateRequest} with a {@link Feature} urn not assigned to an other {@link ILightFeatureUpdateRequest}
     * or to an existing {@link IFeatureRequestToSchedule} and with it step set to LOCAL_SCHEDULED an ordered by
     * priority and date.
     *
     * @param size           maximum number of request to return
     * @param delayInSeconds delay in seconds from now of returned requests
     */
    default List<IFeatureRequestToSchedule> findRequestsToSchedule(int delayInSeconds, int size) {
        OffsetDateTime now = OffsetDateTime.now();
        return doFindRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED.name(),
                                        now,
                                        List.of(FeatureRequestStep.LOCAL_SCHEDULED.name(),
                                                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED.name(),
                                                FeatureRequestStep.REMOTE_STORAGE_REQUESTED.name(),
                                                FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED.name()),
                                        now.minusSeconds(delayInSeconds),
                                        Pageable.ofSize(size));
    }

    /**
     * Native query to retrieve update requests information with :
     * - No other update request with the same urn in running states
     * - No other creation request with the same urn
     * Those verification are made to avoid processing two update request on the same product at the same time
     * or to avoid processing an update request on product under creation.
     */
    @Query(value = """
        SELECT request.id AS id,
               request.provider_id AS providerId,
               request.urn AS urn,
               request.priority AS priorityLevel,
               request.session_name AS session,
                request.session_owner AS sessionOwner
        FROM (
            SELECT request2.id, 
                   request2.provider_id,
                   request2.priority,
                   request2.urn,
                   request2.session_name,
                   request2.session_owner
             FROM t_feature_request request2
             WHERE request2.step = :step
             AND request2.request_type = 'UPDATE'
             AND request2.request_date < :now
             AND request2.registration_date <= :delay
             ORDER BY request2.priority desc, request2.request_date
        ) request
        WHERE
          NOT EXISTS(
            SELECT 1 FROM t_feature_request req
            where
              req.step in (:blocking_steps)
              AND req.urn = request.urn
              AND req.request_type = 'UPDATE'
            LIMIT 1
          )
          AND NOT EXISTS(
            SELECT 1 FROM t_feature_request req
            WHERE
              req.urn = request.urn
              AND req.request_type = 'CREATION'
            LIMIT 1
          )
          AND NOT EXISTS(
            SELECT 1 FROM t_feature_update_dissemination diss
            WHERE diss.feature_urn = request.urn
            LIMIT 1
           )
        """, nativeQuery = true)
    List<IFeatureRequestToSchedule> doFindRequestsToSchedule(@Param("step") String step,
                                                             @Param("now") OffsetDateTime now,
                                                             @Param("blocking_steps") List<String> blockingSteps,
                                                             @Param("delay") OffsetDateTime delay,
                                                             Pageable pageLimit);

}
