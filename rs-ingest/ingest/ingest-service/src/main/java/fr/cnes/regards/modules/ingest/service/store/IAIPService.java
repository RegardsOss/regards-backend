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
package fr.cnes.regards.modules.ingest.service.store;

import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
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
     * Set the status of the given AIP to given one
     */
    void setAipInError(UniformResourceName aipId, IAipState storeError, String failureCause, SIPState sipState);

    /**
     * Delete the {@link AIPEntity} by his ipId
     */
    void deleteAip(UniformResourceName aipId, UniformResourceName sipId, IAipState state);

    /**
     * Set {@link AIPEntity} state to give none
     */
    void setAipToStored(UniformResourceName aipId, IAipState state);

    /**
     * Set {@link AIPEntity} state to {@link SipAIPState#INDEXED}
     * @return {@link AIPEntity} updated
     */
    AIPEntity setAipToIndexed(AIPEntity aip);

    /**
     * Set {@link AIPEntity} state to {@link SipAIPState#INDEX_ERROR}
     * @return {@link AIPEntity} updated
     */
    AIPEntity setAipToIndexError(AIPEntity aip);

    /**
     * Search for a {@link AIPEntity} by its ipId
     */
    Optional<AIPEntity> searchAip(UniformResourceName aipId);

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

    /**
     * Look for sips in state {@link fr.cnes.regards.modules.ingest.domain.entity.SIPState#TO_BE_DELETED} and
     * ask to rs-storage to delete them per page of 100.
     */
    void askForAipsDeletion();

    /**
     * Reactivate AIP submission for AIP and its SIP in submission error
     */
    void retryAipSubmission(String sessionId) throws ModuleException;
}
