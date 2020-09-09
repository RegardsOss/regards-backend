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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.ingest.domain.dump.LastDump;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;

/**
 * {@link LastDump} repository
 * @author Iliana Ghazali
 */
@Repository
public interface IAIPSaveMetadataRepositoryRefactor extends JpaRepository<AIPSaveMetadataRequestRefactor, Long> {

    default Page<AIPSaveMetadataRequestRefactor> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestState.CREATED, pageRequest);
    }

    Page<AIPSaveMetadataRequestRefactor> findAllByState(InternalRequestState step, Pageable page);

    List<AIPSaveMetadataRequestRefactor> findAllByAipIdIn(List<Long> aips);

}

