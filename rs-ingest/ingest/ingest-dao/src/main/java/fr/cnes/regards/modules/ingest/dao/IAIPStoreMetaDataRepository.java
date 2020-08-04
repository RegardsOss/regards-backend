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

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * {@link AIPStoreMetaDataRequest} repository
 * @author Léo Mieulet
 */
@Repository
public interface IAIPStoreMetaDataRepository extends JpaRepository<AIPStoreMetaDataRequest, Long> {

    default Page<AIPStoreMetaDataRequest> findWaitingRequest(Pageable pageRequest) {
        return findAllByState(InternalRequestState.CREATED, pageRequest);
    }

    Page<AIPStoreMetaDataRequest> findAllByState(InternalRequestState step, Pageable page);

    @Query("select chain.preProcessingPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.preProcessingPlugin.id = conf.id")
    List<AIPStoreMetaDataRequest> findAllByAipIdIn(List<Long> aips);
}
