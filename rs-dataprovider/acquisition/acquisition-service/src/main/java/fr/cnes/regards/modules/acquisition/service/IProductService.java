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

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.job.SIPSubmissionJob;

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
     * Retrieve one specified {@link Product}
     * @param id {@link Product}
     */
    Product retrieve(Long id);

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
     * Calcul the {@link ProductState} :
     *
     * <li>{@link ProductState#ACQUIRING} : the initial state, at least a mandatory file is missing</br>
     * </br>
     *
     * <li>{@link ProductState#COMPLETED} : all mandatory files is acquired</br>
     * </br>
     *
     * <li>{@link ProductState#FINISHED} : the mandatory and optional files are acquired</br>
     * </br>
     *
     * <li>{@link ProductState#SAVED} : the {@link Product} is saved by the microservice Ingest</br>
     * </br>
     *
     * <li>{@link ProductState#ERROR} : the {@link Product} is in error</br>
     * </br>
     *
     * @param product the {@link Product}
     */
    void computeProductStatus(Product product);

    /**
     * Get the {@link Product} corresponding to the productName and calculate the {@link ProductState}.<br>
     * If it does not exists, create this {@link Product}. Create or update the product in database.
     *
     * @param session the current session
     * @param acqFile the {@link AcquisitionFile} to add to the {@link Product}
     * @param productName the {@link Product} name
     * @param metaProduct the {@link MetaProduct} of the {@link Product}
     * @param ingestChain the current ingest processing chain
     * @return the existing {@link Product} corresponding to the product name
     */
    Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain) throws ModuleException;

    /**
     * @param ingestChain ingest processing chain name
     * @param session ingest session name
     * @return the first page of products with state {@link ProductSIPState#SUBMISSION_SCHEDULED}
     *
     */
    Page<Product> findProductsToSubmit(String ingestChain, String session);

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
}
