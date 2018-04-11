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
import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * AIP Service interface. Service to handle business around {@link AIPEntity}s
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Handle job event
     */
    void handleJobEvent(JobEvent jobEvent);

    /**
     * Handle AIP event from STORAGE
     */
    void handleAipEvent(AIPEvent aipEvent);

    /**
     * Set the status of the given AIP to {@link SipAIPState#STORE_ERROR}
     */
    void setAipInError(String ipId, IAipState storeError, String failureCause);

    /**
     * Delete the {@link AIPEntity} by his ipId
     */
    void deleteAip(String ipId, String sipIpId, IAipState state);

    /**
     * Set {@link AIPEntity} state to {@link SipAIPState#STORED}
     */
    void setAipToStored(String ipId, IAipState state);

    /**
     * Set {@link AIPEntity} state to {@link SipAIPState#INDEXED}
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

    /**
     * Schedule storage bulk request job according to available AIPs
     */
    void scheduleAIPStorageBulkRequest();

    /**
     * Save AIP
     */
    AIPEntity save(AIPEntity entity);

    /**
     * Get AIP to submit in {@link SipAIPState#SUBMISSION_SCHEDULED} state for specific ingest processing chain
     */
    Set<AIPEntity> findAIPToSubmit(String ingestProcessingChain);
}
