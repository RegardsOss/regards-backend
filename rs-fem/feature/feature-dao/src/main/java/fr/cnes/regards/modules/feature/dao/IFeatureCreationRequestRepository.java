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

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface IFeatureCreationRequestRepository extends IAbstractFeatureRequestRepository<FeatureCreationRequest> {

    Set<FeatureCreationRequest> findByGroupIdIn(Set<String> groupIds);

    Page<FeatureCreationRequest> findByStep(FeatureRequestStep step, Pageable page);

    @Query("select urn from FeatureCreationRequest where urn in :urnList")
    Set<FeatureUniformResourceName> findUrnByUrnIn(@Param("urnList") Collection<FeatureUniformResourceName> urnList);

    /**
     * Get a limited number of {@link ILightFeatureCreationRequest} ready to be handled by a job  ordered by priority
     * and date.
     * A creation request cannot be scheduled if one is already scheduled with same provider id.
     *
     * @param size           maximum number of request to return
     * @param delayInSeconds delay in seconds from now of returned requests
     */
    default List<ILightFeatureCreationRequest> findRequestsToSchedule(int delayInSeconds, int size) {
        OffsetDateTime now = OffsetDateTime.now();
        return doFindRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED,
                                        now,
                                        now.minusSeconds(delayInSeconds),
                                        PageRequest.ofSize(size));

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
         request.requestId as requestId,
         request.metadata as metadata
        FROM FeatureCreationRequest request
        WHERE NOT EXISTS (
            SELECT scheduledRequest.providerId 
            FROM FeatureCreationRequest scheduledRequest
            WHERE scheduledRequest.step = 'LOCAL_SCHEDULED'
            AND scheduledRequest.providerId = request.providerId
        )
        AND request.step = :step
        AND request.registrationDate <= :delay
        AND request.requestDate <= :now
        ORDER BY request.priority desc, request.requestDate
        """)
    List<ILightFeatureCreationRequest> doFindRequestsToSchedule(@Param("step") FeatureRequestStep step,
                                                                @Param("now") OffsetDateTime now,
                                                                @Param("delay") OffsetDateTime delay,
                                                                Pageable pageLimit);

    List<FeatureCreationRequest> findAllByIdIn(Iterable<Long> ids);

    @Override
    List<FeatureCreationRequest> findAllById(Iterable<Long> longs);

    @Modifying
    @Query(value = "UPDATE t_feature SET feature = jsonb_set(feature, CAST('{last}' AS text[]), CAST(CAST(:last AS text) AS jsonb)), last_update = :now  WHERE urn IN :urns",
           nativeQuery = true)
    void updateLastByUrnIn(@Param("last") boolean last, @Param("now") Timestamp now, @Param("urns") Set<String> urns);

    Long deleteByFeatureEntityIn(Collection<FeatureEntity> features);
}
