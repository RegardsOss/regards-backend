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

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;

/**
 * {@link IngestRequest} repository
 * @author Marc SORDI
 */
@Repository
public interface IIngestRequestRepository extends JpaRepository<IngestRequest, Long> {

    /**
     * WARNING : concurrent access here!
     * Get ingest request by ingest chain and state
     * @param ingestChain ingest chain
     * @param state request state
     * @param pageable page info
     */
    Page<IngestRequest> findPageByMetadataIngestChainAndState(String ingestChain, RequestState state,
            Pageable pageable);

    /**
     * Update state for a collection of requests
     * @param state new state
     * @param ids request identifiers
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE IngestRequest r set r.state = :state where r.id in (:ids)")
    void updateIngestRequestState(@Param("state") RequestState state, @Param("ids") Collection<Long> ids);

    /**
     * Get request by ids
     */
    List<IngestRequest> findByIdIn(Collection<Long> ids);

    /**
     * Check is a request exists with specified request id
     * @param requestId request identifier
     * @return true if at least one request exists
     */
    boolean existsByRequestId(String requestId);
}
