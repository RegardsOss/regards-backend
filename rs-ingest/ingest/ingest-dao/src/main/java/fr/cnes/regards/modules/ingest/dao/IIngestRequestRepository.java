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
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;

/**
 * {@link IngestRequest} repository
 * @author Marc SORDI
 */
@Repository
public interface IIngestRequestRepository extends JpaRepository<IngestRequest, Long> {

    /**
     * Get request by ids
     */
    @EntityGraph(attributePaths = "aips")
    List<IngestRequest> findByIdIn(Collection<Long> ids);

    /**
     * Find request by remote group id (i.e. remote request id) and retrieve linked AIPs
     */
    default Optional<IngestRequest> findOneWithAIPs(String remoteStepGroupId) {
        List<IngestRequest> ingestRequests = findAll(IngestRequestSpecifications
                .searchByRemoteStepId(remoteStepGroupId));
        Optional<IngestRequest> result = Optional.empty();
        if (!ingestRequests.isEmpty()) {
            result = Optional.ofNullable(ingestRequests.get(0));
        }
        return result;
    }

    /**
     * Find request by remote group id (i.e. remote request id)
     */
    default Optional<IngestRequest> findOne(String remoteStepGroupId) {
        return findOne(IngestRequestSpecifications.searchByRemoteStepId(remoteStepGroupId));
    }

    /**
     * Internal method used to retrieve IngestRequest using a specification with linked AIPs
     */
    @EntityGraph(attributePaths = "aips")
    List<IngestRequest> findAll(Specification<IngestRequest> spec);

    /**
     * Internal method used to retrieve IngestRequest using a specification
     */
    Optional<IngestRequest> findOne(Specification<IngestRequest> spec);

    List<IngestRequest> findAllByAipsIn(AIPEntity aipEntity);

    List<IngestRequest> findAllByAipsIdIn(List<Long> aipIds);
}
