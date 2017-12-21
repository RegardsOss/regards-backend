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
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link AcquisitionProcessingChain} management service
 *
 * @author Christophe Mertz
 *
 */
public interface IAcquisitionProcessingChainService {

    /**
     * Save a {@link AcquisitionProcessingChain}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain} to save
     * @return the saved {@link AcquisitionProcessingChain}
     */
    AcquisitionProcessingChain save(AcquisitionProcessingChain acqProcessingChain);

    /**
     * Create or update a {@link AcquisitionProcessingChain}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain} to save
     * @return the saved {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain createOrUpdate(AcquisitionProcessingChain acqProcessingChain) throws ModuleException;

    /**
     * Update a {@link AcquisitionProcessingChain}
     * @param chainId the {@link AcquisitionProcessingChain} identifier to update
     * @param acqProcessingChain the {@link AcquisitionProcessingChain} to update
     * @return the updated {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    AcquisitionProcessingChain update(Long chainId, AcquisitionProcessingChain acqProcessingChain)
            throws ModuleException;

    /**
     * Retrieve one specified {@link AcquisitionProcessingChain}
     * @param id {@link AcquisitionProcessingChain}
     * @return a {@link AcquisitionProcessingChain}
     */
    AcquisitionProcessingChain retrieve(Long id);

    /**
     * Retrieve one specified {@link AcquisitionProcessingChain} and load all the properties with a
     * {@link FetchType#LAZY}.
     * @param id {@link AcquisitionProcessingChain}
     * @return a {@link AcquisitionProcessingChain}
     */
    AcquisitionProcessingChain retrieveComplete(Long id);

    /**
     * @return all {@link AcquisitionProcessingChain}
     */
    Page<AcquisitionProcessingChain> retrieveAll(Pageable page);

    /**
     * Delete a {@link AcquisitionProcessingChain}
     * @param id the {@link AcquisitionProcessingChain} identifier
     */
    void delete(Long id);

    /**
     * Delete a {@link AcquisitionProcessingChain}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain} to delete
     */
    void delete(AcquisitionProcessingChain acqProcessingChain);

    /**
     * Find a {@link AcquisitionProcessingChain} by {@link MetaProduct}
     * @param metaProduct the {@link MetaProduct} to find
     * @return the finded {@link AcquisitionProcessingChain}
     */
    AcquisitionProcessingChain findByMetaProduct(MetaProduct metaProduct);

    /**
     * Start the acquisition process for a {@link AcquisitionProcessingChain}
     * @param id the {@link AcquisitionProcessingChain} identifier
     * @return the {@link AcquisitionProcessingChain} has been started
     */
    void run(Long id);

    /**
     * Start the acquisition process for a {@link AcquisitionProcessingChain}
     * @param acqProcessingChain the {@link AcquisitionProcessingChain} to start
     * @return been started
     */
    void run(AcquisitionProcessingChain acqProcessingChain);

    /**
     * Run active chains that is not already running
     */
    void runActiveChains();
}
