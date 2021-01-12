/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureCreationRequest;

@Repository
public interface IFeatureCreationRequestRepository extends IAbstractFeatureRequestRepository<FeatureCreationRequest> {

    Set<FeatureCreationRequest> findByGroupIdIn(Set<String> groupIds);

    Page<FeatureCreationRequest> findByStep(FeatureRequestStep step, Pageable page);

    /**
     * Get a page of {@link ILightFeatureCreationRequest} with specified step.
     * A creation request cannot be scheduled if one is already scheduled with same provider id.
     * @param now current date we not schedule future request
     * @return a list of {@link ILightFeatureCreationRequest}
     */
    @Query("select request.requestOwner as requestOwner, request.state as state, request.priority as priority,"
            + " request.step as step, request.registrationDate as registrationDate, request.requestDate as requestDate,"
            + " request.requestId as requestId, request.providerId as providerId, request.metadata as metadata,"
            + " request.id as id, request.errors as errors, request.groupId as groupId"
            + " from FeatureCreationRequest request where request.providerId not in ("
            + " select scheduledRequest.providerId from FeatureCreationRequest scheduledRequest"
            + " where scheduledRequest.step = 'LOCAL_SCHEDULED') and request.step = :step and request.requestDate <= :now")
    Page<ILightFeatureCreationRequest> findRequestsToSchedule(@Param("step") FeatureRequestStep step,
            @Param("now") OffsetDateTime now, Pageable page);

    List<FeatureCreationRequest> findAllByIdIn(Iterable<Long> ids);

    @Override
    List<FeatureCreationRequest> findAllById(Iterable<Long> longs);

    @Modifying
    @Query(value ="UPDATE t_feature SET feature = jsonb_set(feature, CAST('{last}' AS text[]), CAST(CAST(:last AS text) AS jsonb)) WHERE urn IN :urns", nativeQuery = true)
    void updateLastByUrnIn(@Param("last") boolean last,@Param("urns") Set<String> urns);
}
