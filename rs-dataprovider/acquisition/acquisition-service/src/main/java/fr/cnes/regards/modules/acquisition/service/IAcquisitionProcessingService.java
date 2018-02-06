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
package fr.cnes.regards.modules.acquisition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionJobReport;

/**
 * Acquisition processing service interface
 *
 * @author Marc Sordi
 *
 */
public interface IAcquisitionProcessingService {

    /**
     * List all acquisition chains
     * @param pageable pagination filter
     * @return list of all acquisition chains
     * @throws ModuleException if error occurs!
     */
    Page<AcquisitionProcessingChain> getAllChains(Pageable pageable) throws ModuleException;

    /**
     * Retrieve a processing chain according to its identifier.
     * @param id {@link AcquisitionProcessingChain} identifier
     * @return {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs.
     */
    AcquisitionProcessingChain getChain(Long id) throws ModuleException;

    /**
     * Create a new acquisition processing chain
     * @param processingChain the processing chain
     * @return registered processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Update an existing processing chain
     * @param processingChain the updated processing chain
     * @return updated processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Lock processing chain
     * @param processingChain processing chain to lock
     * @throws ModuleException if error occurs!
     */
    void lockChain(AcquisitionProcessingChain processingChain);

    /**
     * Unlock processing chain
     * @param processingChain processing chain to unlock
     * @throws ModuleException if error occurs!
     */
    void unlockChain(AcquisitionProcessingChain processingChain);

    /**
     * Start all automatic chains according to several conditions
     * @throws ModuleException if error occurs!
     */
    void startAutomaticChains();

    /**
     * Start a chain manually
     * @param processingChainId identifier of the chain to start
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain startManualChain(Long processingChainId) throws ModuleException;

    /**
     * Scan and register detected files for specified {@link AcquisitionProcessingChain}
     * @param processingChain processing chain
     * @throws ModuleException if error occurs!
     */
    void scanAndRegisterFiles(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Validate {@link AcquisitionFileState#IN_PROGRESS} files for specified {@link AcquisitionProcessingChain}
     * @param processingChain processing chain
     * @throws ModuleException if error occurs!
     */
    void validateFiles(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Build products according to {@link AcquisitionFileState#VALID} files for specified
     * {@link AcquisitionProcessingChain}
     * @param processingChain processing chain
     * @throws ModuleException if error occurs!
     */
    void buildProducts(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Build summaries list of {@link AcquisitionProcessingChain}s.
     * Each summary allow to monitor chain progress.
     * @param label {@link String} optional search parameter on {@link AcquisitionProcessingChain}s label
     * @param running {@link Boolean} optional search parameter on {@link AcquisitionProcessingChain}s running
     * @throws ModuleException
     */
    Page<AcquisitionProcessingChainMonitor> buildAcquisitionProcessingChainSummaries(String label, Boolean running,
            AcquisitionProcessingChainMode mode, Pageable pageable) throws ModuleException;

    /**
     * Update a report related to a starting job
     * @param jobReport job report to update
     */
    void reportJobStarted(AcquisitionJobReport jobReport);

    /**
     * Update a report related to a stopping job
     * @param jobReport job report to update
     */
    void reportJobStopped(AcquisitionJobReport jobReport);
}
