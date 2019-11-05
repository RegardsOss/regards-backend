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
package fr.cnes.regards.modules.acquisition.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;

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
     * Retrieve all processing chains
     * @return all chains fully loaded
     */
    List<AcquisitionProcessingChain> getFullChains() throws ModuleException;

    /**
     * Retrieve all processing chains by page
     * @return all chains fully loaded
     */
    Page<AcquisitionProcessingChain> getFullChains(Pageable pageable) throws ModuleException;

    /**
     * Update an existing processing chain
     * @param processingChain the updated processing chain
     * @return updated processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Create a new acquisition processing chain
     * @param processingChain the processing chain
     * @return registered processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Patch an existing processing chain with new values for active and state
     *
     * @param chainId
     * @param payload
     * @return
     */
    AcquisitionProcessingChain patchStateAndMode(Long chainId, UpdateAcquisitionProcessingChain payload)
            throws ModuleException;

    /**
     * Patch several existing processing chain with provided values for active and state
     * @param payload
     * @return
     */
    List<AcquisitionProcessingChain> patchChainsStateAndMode(UpdateAcquisitionProcessingChains payload)
            throws ModuleException;

    /**
     * Delete an inactive processing chain according to its identifier
     * @param id {@link AcquisitionProcessingChain} identifier
     * @throws ModuleException if error occurs.
     */
    void deleteChain(Long id) throws ModuleException;

    /**
     * Lock processing chain
     * @param id {@link AcquisitionProcessingChain} identifier
     */
    void lockChain(Long id);

    /**
     * Unlock processing chain
     * @param id {@link AcquisitionProcessingChain} identifier
     */
    void unlockChain(Long id);

    /**
     * Start all automatic chains according to several conditions
     */
    void startAutomaticChains();

    /**
     * Start a chain manually
     * @param processingChainId identifier of the chain to start
     * @param session optional, replace the name of the acquisition session
     * @return started processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain startManualChain(Long processingChainId, Optional<String> session)
            throws ModuleException;

    /**
     * Stop a chain regardless of its mode.
     * @param processingChainId identifier of the chain to stop
     * @throws ModuleException if error occurs!
     */
    void stopChainJobs(Long processingChainId) throws ModuleException;

    /**
     * Check if a chain is stopped and cleaned
     * @param processingChainId identifier of the stopping chain
     * @return true if all jobs are stopped and related products are cleaned
     * @throws ModuleException if error occurs!
     */
    boolean isChainJobStoppedAndCleaned(Long processingChainId) throws ModuleException;

    /**
     * Stop a chain and clean all inconsistencies after all jobs are aborted
     * @param processingChainId identifier of the chain to stop
     * @return processing chain
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain stopAndCleanChain(Long processingChainId) throws ModuleException;

    /**
     * Delete all products of the given processing chain and session
     * @param processingChainLabel
     * @param session
     * @throws ModuleException
     */
    void deleteSessionProducts(String processingChainLabel, String session) throws ModuleException;

    /**
     * Delete all products of the given processing chain
     * @param processingChainLabel
     * @throws ModuleException
     */
    void deleteProducts(String processingChainLabel) throws ModuleException;

    /**
     * Scan and register detected files for specified {@link AcquisitionProcessingChain}
     * @param processingChain processing chain
     * @throws ModuleException if error occurs!
     */
    void scanAndRegisterFiles(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Register multiple files in one transaction
     * @param filePaths paths of the files to register
     * @param info related file info
     * @param scanningDate reference date used to launch scan plugin
     * @param updateFileInfo does the fileInfo last modification date should be updated
     *                       with the file last modification date
     * @return number of registered files
     */
    int registerFiles(List<Path> filePaths, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate,
            boolean updateFileInfo) throws ModuleException;

    /**
     * Register multiple files in one transaction
     * @param filePaths paths of the files to register
     * @param info related file info
     * @param scanningDate reference date used to launch scan plugin
     * @param updateFileInfo does the fileInfo last modification date should be updated
     *                       with the file last modification date
     * @param limit maximum number of files to register
     * @return number of registered files
     */
    RegisterFilesResponse registerFiles(Iterator<Path> filePaths, AcquisitionFileInfo info,
            Optional<OffsetDateTime> scanningDate, boolean updateFileInfo, int limit) throws ModuleException;

    /**
     * Register a new file in one transaction
     * @param filePath path of the file to register
     * @param info related file info
     * @param scanningDate reference date used to launch scan plugin
     * @param updateFileInfo does the fileInfo last modification date should be updated
     *                       with the file last modification date
     * @return true if really registered
     */
    boolean registerFile(Path filePath, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate,
            boolean updateFileInfo) throws ModuleException;

    /**
     * Manage new registered file : prepare or fulfill products and schedule SIP generation as soon as possible
     */
    void manageRegisteredFiles(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Same action as {@link #manageRegisteredFiles(AcquisitionProcessingChain)} but in a new transaction and by page
     */
    boolean manageRegisteredFilesByPage(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Restart jobs in {@link ProductSIPState#SCHEDULED_INTERRUPTED} for processing chain
     */
    void restartInterruptedJobs(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Retry SIP generation for products in {@link ProductSIPState#GENERATION_ERROR} or
     *  {@link ProductSIPState#INGESTION_FAILED}
     */
    void retrySIPGeneration(AcquisitionProcessingChain processingChain);

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
     * Handle {@link fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob} errors
     */
    void handleProductAcquisitionError(JobInfo jobInfo);
}
