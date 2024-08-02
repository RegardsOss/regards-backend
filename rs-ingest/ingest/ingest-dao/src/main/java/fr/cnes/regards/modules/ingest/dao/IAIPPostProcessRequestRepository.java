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

package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * JPA repository to access {@link AIPPostProcessRequest}
 *
 * @author Iliana Ghazali
 */

public interface IAIPPostProcessRequestRepository extends JpaRepository<AIPPostProcessRequest, Long> {

    // find created requests
    default Page<AIPPostProcessRequest> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestState.CREATED, pageRequest);
    }

    // find requests by state
    Page<AIPPostProcessRequest> findAllByState(InternalRequestState step, Pageable page);

    @Modifying
    @Query(value = "delete from t_request where aip_id in :aipIds AND t_request.dtype = '"
                   + RequestTypeConstant.AIP_POST_PROCESS_VALUE
                   + "'", nativeQuery = true)
    void deleteAllByAipIdsIn(@Param("aipIds") List<Long> aipIds);
}
