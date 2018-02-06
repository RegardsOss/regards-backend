/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.store;

import java.util.Optional;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;

/**
 * AIP Service interface. Service to handle business aroud {@link AIPEntity}s
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Send a bulk request to storage microservice with all {@link AIPEntity}s in {@link AIPState#CREATED} state
     */
    void postAIPStorageBulkRequest();

    /**
     * Set the status of the given AIP to {@link AIPState#STORE_ERROR}
     * @param ipId
     * @param storeError
     * @param failureCause
     */
    void setAipInError(String ipId, AIPState storeError, String failureCause);

    /**
     * Delete the {@link AIPEntity} by his ipId
     */
    void deleteAip(String ipId, String sipIpId);

    /**
     * Set {@link AIPEntity} state to {@link AIPState#STORED}
     * @param ipId
     */
    void setAipToStored(String ipId);

    /**
     * Set {@link AIPEntity} state to {@link AIPState#INDEXED}
     * @param ipId
     * @return {@link AIPEntity} updated
     */
    AIPEntity setAipToIndexed(AIPEntity aip);

    /**
     * Search for a {@link AIPEntity} by his ipId
     * @param ipId
     * @return
     */
    Optional<AIPEntity> searchAip(UniformResourceName ipId);
}
