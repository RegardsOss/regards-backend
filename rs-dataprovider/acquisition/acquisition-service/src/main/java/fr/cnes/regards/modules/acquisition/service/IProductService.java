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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.exception.SIPGenerationException;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;

/**
 *
 * @author Christophe Mertz
 *
 */
public interface IProductService {

    Product save(Product product);

    /**
     * After product SIP generation, save the product state and submit its SIP in the SIP data flow (within the same transaction)
     */
    Product saveAndSubmitSIP(Product product, AcquisitionProcessingChain acquisitionChain)
            throws SIPGenerationException;

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
     * Retrieve a collection of product by names
     * @param productNames list of all product names
     */
    Set<Product> retrieve(Collection<String> productNames) throws ModuleException;

    /**
     * Delete one specified {@link Product}
     * @param id {@link Product}
     */
    void delete(Long id);

    /**
     * Delete one specified {@link Product}
     * @param product {@link Product} to delete
     */
    void delete(AcquisitionProcessingChain chain, Product product);

    /**
     * Delete products
     * @param chain
     * @param session
     * @return number of deleted products
     */
    long deleteBySession(AcquisitionProcessingChain chain, String session);

    /**
     *
     * @param chain
     * @return number of deleted products
     */
    long deleteByProcessingChain(AcquisitionProcessingChain chain);

    /**
     * @return page of products related to specified
     *         acquisition chain.
     */
    Page<Product> findChainProducts(AcquisitionProcessingChain chain, Pageable pageable);

    /**
     * Schedule {@link Product} SIP generations
     * @param products products for which SIP generation has to be scheduled
     * @param chain related chain reference
     */
    JobInfo scheduleProductSIPGenerations(Set<Product> products, AcquisitionProcessingChain chain);

    /**
     * Count number of products associated to the given {@link AcquisitionProcessingChain} and in the given state
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productStates {@link ProductState}s
     * @return number of matching {@link Product}
     */
    long countByChainAndStateIn(AcquisitionProcessingChain processingChain, List<ProductState> productStates);

    /**
     * Count number of products associated to the given {@link AcquisitionProcessingChain} and in the given state
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productSipStates {@link ProductState}s
     * @return number of matching {@link Product}
     */
    long countByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            List<ISipState> productSipStates);

    /**
     * Count number of generation job that is actually running
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productSipState {@link ISipState}s
     */
    long countSIPGenerationJobInfoByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            ISipState productSipState);

    /**
     * Link acquired files to theirs products creating or updating them.<br/>
     * If product is completed or finished, a SIP generation job is scheduled.
     *
     * @param processingChain the related {@link AcquisitionProcessingChain}
     * @return the existing {@link Product} corresponding to the product name
     */
    Set<Product> linkAcquisitionFilesToProducts(AcquisitionProcessingChain processingChain,
            List<AcquisitionFile> validFiles) throws ModuleException;

    /**
     * Handle product {@link fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob} failure
     */
    void handleSIPGenerationError(JobInfo jobInfo);

    /**
     * Handle successful SIP submission
     */
    void handleIngestedSIPSuccess(Collection<RequestInfo> infos);

    /**
     * Handle failure SIP submission
     */
    void handleIngestedSIPFailed(Collection<RequestInfo> infos);

    /**
     * Count number of {@link Product} associated to the given {@link AcquisitionProcessingChain}
     * @param chain {@link AcquisitionProcessingChain}
     * @return number of {@link Product}
     */
    long countByChain(AcquisitionProcessingChain chain);

    /**
     * Search for {@link Product} entities matching parameters
     * @param state {@link ProductState}s
     * @param sipState {@link SIPState}s
     * @param productName {@link String}
     * @param session {@link String}
     * @param processingChainId {@likn Long} id of {@link AcquisitionProcessingChain}
     * @param from {@link OffsetDateTime}
     * @param pageable
     * @return {@link Product}s
     */
    Page<Product> search(List<ProductState> state, List<ISipState> sipState, String productName, String session,
            Long processingChainId, OffsetDateTime from, Boolean noSession, Pageable pageable);

    /**
     * Search for a {@link Product} by his name
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

    /**
     * Restart SIP generation jobs for interrupted product processes
     */
    boolean restartInterruptedJobsByPage(AcquisitionProcessingChain processingChain);

    /**
     * Retry SIP generation jobs for products in {@link ProductSIPState#GENERATION_ERROR}
     */
    boolean retrySIPGenerationByPage(AcquisitionProcessingChain processingChain);

    /**
     * Change sip states by acquisition chain
     */
    void updateSipStates(AcquisitionProcessingChain processingChain, ISipState fromStatus, ISipState toStatus);

    /**
     * Update a list of products by their names
     */
    void updateSipStatesByProductNameIn(ISipState state, Set<String> productNames);

    /**
     * Manage product state of updated products and schedule them for SIP generation as soon as possible
     */
    void manageUpdatedProducts(AcquisitionProcessingChain processingChain);

    /**
     * Same action as {@link #manageUpdatedProducts(AcquisitionProcessingChain)} but in a new transaction and by page
     * @return whether there is a product page remaining to managed
     */
    boolean manageUpdatedProductsByPage(AcquisitionProcessingChain processingChain);

    /**
     * Save success and errors products in DB and submit success ones to ingest microservice for ingestion
     * @param processingChain
     * @param success
     * @param errors
     */
    void handleGeneratedProducts(AcquisitionProcessingChain processingChain, Set<Product> success, Set<Product> errors);

}
