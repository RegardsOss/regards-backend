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

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IFeatureRequestToSchedule;
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
    default List<IFeatureRequestToSchedule> findRequestsToSchedule(int delayInSeconds, int size) {
        OffsetDateTime now = OffsetDateTime.now();
        return doFindRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED.toString(),
                                        now,
                                        now.minusSeconds(delayInSeconds),
                                        PageRequest.ofSize(size));

    }

    /**
     * Native query to retrieve creation requests information with :
     * - No other creation request with the same providerId in running states
     * Those verification are made to avoid processing two create request on the same product at the same time.
     */
    @Query(value = """
        SELECT request.id AS id,
               request.provider_id AS providerId,
               request.priority AS priorityLevel,
               request.session_name AS session,
               request.session_owner AS sessionOwner
        FROM (
            SELECT request2.id,
                   request2.provider_id,
                   request2.priority,
                   request2.session_name,
                   request2.session_owner
            FROM t_feature_request request2
            WHERE request2.step = :step
            AND request2.request_type = 'CREATION'
            AND request2.request_date < :now
            AND request2.registration_date <= :delay
            ORDER BY request2.priority desc, request2.request_date
        ) request
        WHERE
          NOT EXISTS(
            SELECT 1 FROM t_feature_request req
            WHERE
              req.step = 'LOCAL_SCHEDULED'
              AND req.provider_id = request.provider_id
              AND req.request_type = 'CREATION'
            LIMIT 1
          )
        """, nativeQuery = true)
    List<IFeatureRequestToSchedule> doFindRequestsToSchedule(@Param("step") String step,
                                                             @Param("now") OffsetDateTime now,
                                                             @Param("delay") OffsetDateTime delay,
                                                             Pageable pageLimit);

    @Modifying
    @Query(value = "UPDATE t_feature SET feature = jsonb_set(feature, CAST('{last}' AS text[]), CAST(CAST(:last AS text) AS jsonb)), last_update = :now  WHERE urn IN :urns",
           nativeQuery = true)
    void updateLastByUrnIn(@Param("last") boolean last, @Param("now") Timestamp now, @Param("urns") Set<String> urns);

    Long deleteByFeatureEntityIn(Collection<FeatureEntity> features);
}
