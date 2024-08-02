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
package fr.cnes.regards.modules.workermanager.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.workermanager.domain.database.LightRequest;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;

/**
 * Repository to access {@link Request}
 *
 * @author Sébastien Binda
 * @author Théo Lasserre
 */
@Repository
public interface IRequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    @Query("select requestId from Request where requestId in :requestIds")
    List<String> findRequestIdByRequestIdIn(@Param("requestIds") Collection<String> requestIds);

    List<Request> findByRequestIdIn(Collection<String> requestIds);

    List<Request> findByIdIn(Collection<Long> ids);

    Optional<Request> findOneByRequestId(String requestId);

    Collection<Request> findByStatus(RequestStatus status);

    default Page<LightRequest> findAllLight(Specification<Request> requestSpecification, Pageable pageable) {
        Page<Request> requests = findAll(requestSpecification, pageable);
        return requests.map(LightRequest::new);
    }

    /**
     * Hibernate/Jpa does not permit anymore to return an Optional&lt;LightRequest> if LightRequest isn't an Entity
     */
    default Optional<LightRequest> findLightByRequestId(String requestId) {
        Optional<Request> requestOpt = findOneByRequestId(requestId);
        return requestOpt.map(LightRequest::new);
    }


    long countByStepWorkerTypeAndStatus(String workerType, RequestStatus requestStatus);

    @Modifying
    @Query("update Request request set request.status = :newStatus where request.id in :ids ")
    void updateStatus(@Param("newStatus") RequestStatus requestState, @Param("ids") Set<Long> ids);
}
