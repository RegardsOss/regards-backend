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
package fr.cnes.regards.modules.feature.dao;

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
     * or to an existing {@link ILightFeatureCreationRequest} and with it step set to LOCAL_SCHEDULED an ordered by
     * priority and date.
     *
     * @param size           maximum number of request to return
     * @param delayInSeconds delay in seconds from now of returned requests
     */
    default List<ILightFeatureUpdateRequest> findRequestsToSchedule(int delayInSeconds, int size) {
        OffsetDateTime now = OffsetDateTime.now();
        return doFindRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED,
                                        now,
                                        List.of(FeatureRequestStep.LOCAL_SCHEDULED,
                                                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED),
                                        now.minusSeconds(delayInSeconds),
                                        Pageable.ofSize(size));
    }

    @Query("""
        SELECT
          request.providerId as providerId,
          request.urn as urn,
          request.id as id,
          request.groupId as groupId,
          request.errors as errors,
          request.requestOwner as requestOwner,
          request.state as state,
          request.priority as priority,
          request.step as step,
          request.registrationDate as registrationDate,
          request.requestDate as requestDate,
          request.requestId as requestId
        FROM FeatureUpdateRequest request
        WHERE NOT EXISTS (
            SELECT scheduledRequest.urn
            FROM FeatureUpdateRequest scheduledRequest
            WHERE scheduledRequest.step in (:blocking_steps)
            AND scheduledRequest.urn = request.urn
        )
        AND NOT EXISTS (
            SELECT urn
            FROM FeatureCreationRequest
            WHERE urn = request.urn
        )
        AND request.step = :step
        AND request.registrationDate <= :delay
        AND request.requestDate <= :now
        ORDER BY request.priority desc, request.requestDate
        """)
    List<ILightFeatureUpdateRequest> doFindRequestsToSchedule(@Param("step") FeatureRequestStep step,
                                                              @Param("now") OffsetDateTime now,
                                                              @Param("blocking_steps")
                                                              List<FeatureRequestStep> blockingSteps,
                                                              @Param("delay") OffsetDateTime delay,
                                                              Pageable pageLimit);
}
