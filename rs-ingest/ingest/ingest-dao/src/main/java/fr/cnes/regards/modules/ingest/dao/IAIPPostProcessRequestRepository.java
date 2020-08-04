/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;

/**
 * JPA repository to access {@link AIPPostProcessRequest}
 * @author Iliana Ghazali
 */

public interface IAIPPostProcessRequestRepository extends JpaRepository<AIPPostProcessRequest,Long> {

    // find created requests
    default Page<AIPPostProcessRequest> findWaitingRequest(Pageable pageRequest){
        return findAllByState(InternalRequestState.CREATED, pageRequest);
    }
    // find requests by state
    Page<AIPPostProcessRequest> findAllByState(InternalRequestState step, Pageable page);

    // get requests by id
    @EntityGraph(attributePaths = "aips")
    List<AIPPostProcessRequest> findByIdIn(Collection<Long> ids);

    //find request corresponding to an AIP id
    @Query(value="SELECT aip_id FROM t_request WHERE aip_id = (:id)")
    AIPPostProcessRequest findRequestByAipId(@Param("id") String aipId);


}
