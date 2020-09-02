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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 *
 * @author Marc SORDI
 *
 */
@Repository
public interface IFeatureUpdateRequestRepository extends IAbstractFeatureRequest {

    /**
     * Retrieve update requests to process sorted by request date.<br/>
     * Sorting requests is useful to manage several update requests on a same target entity!
     */
    List<FeatureUpdateRequest> findAllByIdInOrderByRequestDateAsc(Set<Long> ids);


    /**
     * Get {@link ILightFeatureUpdateRequest} with a {@link Feature} urn not assigned to an other {@link ILightFeatureUpdateRequest}
     * with it step set to LOCAL_SCHEDULED an ordered by registration date and before a delay
     * @param now current date we will not schedule future requests
     * @param page contain the number of {@link ILightFeatureUpdateRequest} to return
     * @param delay we want {@link ILightFeatureUpdateRequest} with registration date before this delay
     * @return list of {@link ILightFeatureUpdateRequest}
     */
    @Query("select request.providerId as providerId, request.urn as urn, request.id as id, request.groupId as groupId,"
            + " request.errors as errors, request.requestOwner as requestOwner, request.state as state, request.priority as priority,"
            + " request.step as step, request.registrationDate as registrationDate, request.requestDate as requestDate,"
            + " request.requestId as requestId from FeatureUpdateRequest request where request.urn not in ("
            + " select scheduledRequest.urn from FeatureUpdateRequest scheduledRequest"
            + " where scheduledRequest.step = 'LOCAL_SCHEDULED') and request.step = :step and request.registrationDate <= :delay and request.requestDate <= :now order by request.priority, request.requestDate ")
    Page<ILightFeatureUpdateRequest> findRequestsToSchedule(@Param("step") FeatureRequestStep step,
            @Param("now") OffsetDateTime now, Pageable page, @Param("delay") OffsetDateTime delay);

    /**
     * Update {@link ILightFeatureUpdateRequest} step
     * @param step new {@link FeatureRequestStep}
     * @param ids id of {@link ILightFeatureUpdateRequest} to update
     */
    @Modifying
    @Query("update FeatureUpdateRequest fur set fur.step = :newStep where fur.id in :ids ")
    void updateStep(@Param("newStep") FeatureRequestStep step, @Param("ids") Set<Long> ids);

    @Modifying
    @Query("update FeatureUpdateRequest fur set fur.state = :newState where fur.id in :ids ")
    void updateState(@Param("newState") RequestState requestState, @Param("ids") Set<Long> ids);
}
