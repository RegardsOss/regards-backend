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
package fr.cnes.regards.modules.ingest.service.chain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.aip.AIP;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Ingest processing service interface
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public interface IIngestProcessingService {

    /**
     * Schedule {@link IngestProcessingJob}s for all {@link SIPEntity} with {@link SIPState#CREATED} Status.
     */
    void ingest();

    /**
     * Really build ingestion job and schedule it in transaction.
     */
    void scheduleIngestProcessingJob(Set<Long> entityIdsToProcess, String processingChain);

    /**
     *  {@link ISIPService} delegated method, save and publish entity
     */
    SIPEntity updateSIPEntity(SIPEntity sip);

    /**
     * After AIP(s) generation, save the context and submit AIP(s) in the AIP data flow (within the same transaction)
     */
    SIPEntity saveAndSubmitAIP(SIPEntity entity, List<AIP> aips) throws EntityNotFoundException;

    /**
     * Return {@link SIPEntity} for the given id
     */
    SIPEntity getSIPEntity(Long id) throws EntityNotFoundException;

    /**
     *
     * Return all {@link SIPEntity}s for the given ids
     */
    Set<SIPEntity> getAllSipEntities(Set<Long> ids);

    /**
     * Create AIP
     */
    AIPEntity createAIP(Long sipEntityId, AIP aip) throws EntityNotFoundException;

    /**
     * Create a new {@link IngestProcessingChain}
     * @param newChain {@link IngestProcessingChain}
     * @return created {@link IngestProcessingChain}
     */
    IngestProcessingChain createNewChain(IngestProcessingChain newChain) throws ModuleException;

    /**
     * Create a new {@link IngestProcessingChain}
     * @param input JSON file containing an {@link IngestProcessingChain}
     * @return the newly created {@link IngestProcessingChain}
     */
    IngestProcessingChain createNewChain(InputStream input) throws ModuleException;

    /**
     * Export specified processing chain as JSON file
     * @param name processing chain name
     * @param os output stream
     */
    void exportProcessingChain(String name, OutputStream os) throws ModuleException, IOException;

    /**
     * Update a {@link IngestProcessingChain}
     * @param chainToUpdate {@link IngestProcessingChain}
     * @return updated {@link IngestProcessingChain}
     */
    IngestProcessingChain updateChain(IngestProcessingChain chainToUpdate) throws ModuleException;

    /**
     * Delete a {@link IngestProcessingChain}
     * @param name {@link String}
     */
    void deleteChain(String name) throws ModuleException;

    /**
     * Search for existing {@link IngestProcessingChain} with optional search criterion.
     */
    Page<IngestProcessingChain> searchChains(String name, Pageable pageable);

    /**
     * Get all tenant processing chains
     */
    List<IngestProcessingChain> findAll();

    /**
     * Retrieve a {@link IngestProcessingChain} by name
     * @param name {@link String}
     * @return IngestProcessingChain
     */
    IngestProcessingChain getChain(String name) throws ModuleException;

    /**
     * Check chain with specified name exists
     * @param name chain name
     * @return true is exists
     */
    boolean existsChain(String name);

    /**
     * Initialize default configuration
     */
    void initDefaultServiceConfiguration() throws ModuleException;
}
