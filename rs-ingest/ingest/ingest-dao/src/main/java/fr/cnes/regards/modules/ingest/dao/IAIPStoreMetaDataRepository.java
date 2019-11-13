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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@link AIPStoreMetaDataRequest} repository
 * @author Léo Mieulet
 */
@Repository
public interface IAIPStoreMetaDataRepository extends JpaRepository<AIPStoreMetaDataRequest, Long> {

    default Page<AIPStoreMetaDataRequest> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestStep.CREATED, pageRequest);
    }

    Page<AIPStoreMetaDataRequest> findAllByState(InternalRequestStep step, Pageable page);

    Page<AbstractRequest> findAll(Specification<AbstractRequest> searchAllByFilters, Pageable pageable);
}
