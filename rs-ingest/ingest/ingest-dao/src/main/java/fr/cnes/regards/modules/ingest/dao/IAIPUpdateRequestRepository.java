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
package fr.cnes.regards.modules.ingest.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

/**
 * {@link AIPUpdateRequest} repository
 * @author Léo Mieulet
 */
public interface IAIPUpdateRequestRepository extends JpaRepository<AIPUpdateRequest, Long> {

    default Page<AIPUpdateRequest> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestState.CREATED, pageRequest);
    }

    default List<AIPUpdateRequest> findRunningRequestAndAipIdIn(List<Long> aipIds) {
        return findAllAipDistinctByAipIdInAndState(aipIds, InternalRequestState.RUNNING.name());
    }

    Page<AIPUpdateRequest> findAllByState(InternalRequestState step, Pageable page);

    List<AIPUpdateRequest> findAllByAipIdIn(List<Long> aipIds);

    /**
     * Retrieve only one request for each AIP matching provided criteria
     * @param aipIds
     * @param state
     * @return a list of AIPUpdateRequest with corresponding AIP loaded
     */
    @Query(value = "SELECT DISTINCT ON (t_request.aip_id) * FROM t_request  inner join t_aip on t_request.aip_id=t_aip.id "
            + "WHERE t_request.aip_id IN (:ids) AND t_request.state = :state AND t_request.dtype = '"
            + RequestTypeConstant.UPDATE_VALUE + "'", nativeQuery = true)
    List<AIPUpdateRequest> findAllAipDistinctByAipIdInAndState(@Param("ids") List<Long> aipIds,
            @Param("state") String state);
}