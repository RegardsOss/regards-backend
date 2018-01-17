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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Acquisition processing service interface
 *
 * @author Marc Sordi
 *
 */
public interface IAcquisitionProcessingService {

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
     * Start all automatic chains according to several conditions
     * @throws ModuleException if error occurs!
     */
    void startAutomaticChains();

    /**
     * Start a chain manually
     * @param processingChainId identifier of the chain to start
     * @throws ModuleException if error occurs!
     */
    void startManualChain(Long processingChainId) throws ModuleException;

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
}
