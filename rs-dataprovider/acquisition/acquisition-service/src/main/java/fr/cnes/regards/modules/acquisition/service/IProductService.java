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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.job.SIPSubmissionJob;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 *
 * @author Christophe Mertz
 *
 */
public interface IProductService {

    Product save(Product product);

    /**
     * @return all {@link Product}
     */
    Page<Product> retrieveAll(Pageable page);

    /**
     * Load one specified {@link Product}
     * @param id {@link Product}
     */
    Product loadProduct(Long id) throws ModuleException;

    /**
     * Retrieve one specified {@link Product}
     * @param productName a product name
     */
    Product retrieve(String productName) throws ModuleException;

    /**
     * Delete one specified {@link Product}
     * @param id {@link Product}
     */
    void delete(Long id);

    /**
     * Delete one specified {@link Product}
     * @param product {@link Product} to delete
     */
    void delete(Product product);

    /**
     * @return list of {@link ProductState#FINISHED} or {@link ProductState#COMPLETED} products for specified
     *         acquisition chain <b>not already scheduled</b>.
     */
    Set<Product> findChainProductsToSchedule(AcquisitionProcessingChain chain);

    /**
     * Schedule {@link Product} SIP generation
     * @param product product for which SIP generation has to be scheduled
     * @param chain related chain reference
     * @return scheduled {@link JobInfo}
     */
    JobInfo scheduleProductSIPGeneration(Product product, AcquisitionProcessingChain chain);

    Set<Product> findByStatus(ProductState status);

    /**
     * Count number of products associated to the given {@link AcquisitionProcessingChain} and in the given state
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param states {@link ProductState}s
     * @return number of matching {@link Product}
     */
    long countByChainAndStateIn(AcquisitionProcessingChain processingChain, List<ProductState> productStates);

    /**
     * Get the {@link Product} corresponding to the productName and calculate the {@link ProductState}.<br>
     * If it does not exists, create this {@link Product}. Create or update the product in database.
     *
     * @param session the current session
     * @param acqFile the {@link AcquisitionFile} to add to the {@link Product}
     * @param productName the {@link Product} name
     * @param processingChain the related {@link AcquisitionProcessingChain}
     * @return the existing {@link Product} corresponding to the product name
     */
    Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * @param ingestChain ingest processing chain name
     * @param session ingest session name
     * @return the first page of products with state {@link ProductSIPState#SUBMISSION_SCHEDULED}
     *
     */
    Page<Product> findProductsToSubmit(String ingestChain, Optional<String> session);

    /**
     * Schedule {@link SIPSubmissionJob}s according to available SIPs
     */
    void scheduleProductSIPSubmission();

    /**
     * Handle product SIP submission failure
     */
    void handleProductSIPSubmissionFailure(JobEvent jobEvent);

    /**
     * Retry product SIP submission for resetting product SIP state to {@link ProductSIPState#GENERATED}
     */
    void retryProductSIPSubmission();

    /**
     * Handle a SIP event
     * @param event {@link SIPEvent}
     */
    void handleSIPEvent(SIPEvent event);

    /**
     * Count number of {@link Product} associated to the given {@link AcquisitionProcessingChain}
     * @param chain {@link AcquisitionProcessingChain}
     * @return number of {@link Product}
     */
    long countByChain(AcquisitionProcessingChain chain);

    /**
     * Search for {@link Product} entities matching parameters
     * @param state {@link ProductState}
     * @param sipState {@link SIPState}
     * @param productName {@link String}
     * @param session {@link String}
     * @param processingChainId {@likn Long} id of {@link AcquisitionProcessingChain}
     * @param from {@link OffsetDateTime}
     * @param pageable
     * @return {@link Product}s
     */
    Page<Product> search(ProductState state, SIPState sipState, String productName, String session,
            Long processingChainId, OffsetDateTime from, Pageable pageable);

    /**
     * Search for a {@link Product} by his name
     * @param productName
     * @return
     * @throws ModuleException
     */
    Optional<Product> searchProduct(String productName) throws ModuleException;

    /**
     * Stop all product jobs for a specified processing chain
     * @param processingChain related processing chain
     * @throws ModuleException if error occurs!
     */
    void stopProductJobs(AcquisitionProcessingChain processingChain) throws ModuleException;

    /**
     * Check if all product jobs for a specified processing chain are stopped. Unstable product states are rolled back
     * programmatically when related product job is stopped.
     * @param processingChain related processing chain
     * @return true if all jobs are stopped and cleaned
     * @throws ModuleException if error occurs!
     */
    boolean isProductJobStoppedAndCleaned(AcquisitionProcessingChain processingChain) throws ModuleException;

}
