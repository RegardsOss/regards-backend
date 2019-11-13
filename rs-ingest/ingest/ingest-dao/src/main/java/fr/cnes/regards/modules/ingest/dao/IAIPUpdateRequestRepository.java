/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link AIPUpdateRequest} repository
 * @author Léo Mieulet
 */
public interface IAIPUpdateRequestRepository extends JpaRepository<AIPUpdateRequest, Long> {

    Page<AbstractRequest> findAll(Specification<AbstractRequest> searchAllByFilters, Pageable pageable);

    boolean existsBySessionOwnerAndSession(String sessionOwner, String session);

    default Page<AIPUpdateRequest> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestStep.CREATED, pageRequest);
    }

    default List<AIPUpdateRequest> findRunningRequestAndAipIdIn(List<Long> aipIds) {
        return findAllByAipIdInAndState(aipIds, InternalRequestStep.RUNNING);
    }

    Page<AIPUpdateRequest> findAllByState(InternalRequestStep step, Pageable page);

    List<AIPUpdateRequest> findAllByAipIdIn(List<Long> aipIds);


    Page<AIPUpdateRequest> findAllByStateAndAipIdIn(InternalRequestStep step, List<Long> aipIds, Pageable page);

    List<AIPUpdateRequest> findAllByAipIdInAndState(List<Long> aipIds, InternalRequestStep state);
//   TODO List<AIPUpdateRequest> findDistinctByAipIdAndAipIdInAndState(List<Long> aipIds, InternalRequestStep state);
}
