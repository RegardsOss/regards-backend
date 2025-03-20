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
package fr.cnes.regards.modules.crawler.dao;

import fr.cnes.regards.modules.crawler.domain.EntityEventRequest;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link fr.cnes.regards.modules.crawler.domain.EntityEventRequest}s
 *
 * @author Thibaud Michaudel
 **/
public interface IEntityEventRequestRepository extends JpaRepository<EntityEventRequest, String> {

    Page<EntityEventRequest> findByStatusOrderByUrnAsc(Pageable pageable, EntityEventRequestStatus status);

    List<EntityEventRequest> findByUrnInAndStatusNot(List<String> urns, EntityEventRequestStatus status);

    default List<EntityEventRequest> findByUrnInAndStatusNotToDo(List<String> urns) {
        return findByUrnInAndStatusNot(urns, EntityEventRequestStatus.TO_DO);
    }

    default Page<EntityEventRequest> findByStatusToDoOrderByUrnAsc(Pageable pageable) {
        return findByStatusOrderByUrnAsc(pageable, EntityEventRequestStatus.TO_DO);
    }

    @Modifying
    @Query("UPDATE EntityEventRequest r SET r.status = :status WHERE r.id = :requestId")
    void updateRequestStatus(@Param("requestId") Long requestId, @Param("status") EntityEventRequestStatus status);

    default void retryRequest(Long requestId) {
        updateRequestStatus(requestId, EntityEventRequestStatus.TO_DO);
    }

    default void runRequest(Long requestId) {
        updateRequestStatus(requestId, EntityEventRequestStatus.RUNNING);
    }

    default void scheduleRequest(Long requestId) {
        updateRequestStatus(requestId, EntityEventRequestStatus.SCHEDULED);
    }

    default void requestFailed(Long requestId) {
        updateRequestStatus(requestId, EntityEventRequestStatus.FAILED);
    }

    void deleteById(Long requestId);
}
