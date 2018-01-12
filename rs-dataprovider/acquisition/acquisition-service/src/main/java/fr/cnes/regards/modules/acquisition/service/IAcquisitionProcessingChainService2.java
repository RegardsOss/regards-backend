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

import javax.persistence.FetchType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link AcquisitionProcessingChain2} management service
 *
 * @author Christophe Mertz
 *
 */
public interface IAcquisitionProcessingChainService2 {

    /**
     * Save a {@link AcquisitionProcessingChain2}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain2} to save
     * @return the saved {@link AcquisitionProcessingChain2}
     */
    AcquisitionProcessingChain2 save(AcquisitionProcessingChain2 acqProcessingChain);

    /**
     * Create or update a {@link AcquisitionProcessingChain2}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain2} to save
     * @return the saved {@link AcquisitionProcessingChain2}
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain2 createOrUpdate(AcquisitionProcessingChain2 acqProcessingChain) throws ModuleException;

    /**
     * Update a {@link AcquisitionProcessingChain2}
     * @param chainId the {@link AcquisitionProcessingChain2} identifier to update
     * @param acqProcessingChain the {@link AcquisitionProcessingChain2} to update
     * @return the updated {@link AcquisitionProcessingChain2}
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain2 update(Long chainId, AcquisitionProcessingChain2 acqProcessingChain)
            throws ModuleException;

    /**
     * Retrieve one specified {@link AcquisitionProcessingChain2}
     * @param id {@link AcquisitionProcessingChain2}
     * @return a {@link AcquisitionProcessingChain2}
     */
    AcquisitionProcessingChain2 retrieve(Long id);

    /**
     * Retrieve one specified {@link AcquisitionProcessingChain2} and load all the properties with a
     * {@link FetchType#LAZY}.
     * @param id {@link AcquisitionProcessingChain2}
     * @return a {@link AcquisitionProcessingChain2}
     */
    AcquisitionProcessingChain2 retrieveComplete(Long id);

    /**
     * @return all {@link AcquisitionProcessingChain2}
     */
    Page<AcquisitionProcessingChain2> retrieveAll(Pageable page);

    /**
     * Delete a {@link AcquisitionProcessingChain2}
     * @param id the {@link AcquisitionProcessingChain2} identifier
     */
    void delete(Long id);

    /**
     * Delete a {@link AcquisitionProcessingChain2}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain2} to delete
     */
    void delete(AcquisitionProcessingChain2 acqProcessingChain);

    /**
     * Find a {@link AcquisitionProcessingChain2} by {@link MetaProduct}
     * @param metaProduct the {@link MetaProduct} to find
     * @return the finded {@link AcquisitionProcessingChain2}
     */
    AcquisitionProcessingChain2 findByMetaProduct(MetaProduct metaProduct);

    /**
     * Start the acquisition process for a {@link AcquisitionProcessingChain2}
     * @param id the {@link AcquisitionProcessingChain2} identifier
     * @return the {@link AcquisitionProcessingChain2} has been started
     */
    void run(Long id);

    /**
     * Start the acquisition process for a {@link AcquisitionProcessingChain2}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain2} to start
     * @return been started
     */
    void run(AcquisitionProcessingChain2 acqProcessingChain);

    /**
     * Run active chains that is not already running
     */
    void runActiveChains();
}
