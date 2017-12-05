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

import javax.persistence.FetchType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link ChainGeneration} management service
 * 
 * @author Christophe Mertz
 * 
 */
public interface IChainGenerationService {

    /**
     * Save a {@link ChainGeneration}
     * @param chain the {@link ChainGeneration} to save
     * @return the saved {@link ChainGeneration}
     */
    ChainGeneration save(ChainGeneration chain);

    /**
     * Update a {@link ChainGeneration}
     * @param chainId the {@link ChainGeneration} identifier to update
     * @param chain the {@link ChainGeneration} to update
     * @return the updated {@link ChainGeneration}
     * @throws ModuleException if error occurs!
     */
    ChainGeneration update(Long chainId, ChainGeneration chain) throws ModuleException;

    /**
     * Retrieve one specified {@link ChainGeneration}
     * @param id {@link ChainGeneration}
     * @return a {@link ChainGeneration}
     */
    ChainGeneration retrieve(Long id);

    /**
     * Retrieve one specified {@link ChainGeneration} and load all the properties with a {@link FetchType#LAZY}.
     * @param id {@link ChainGeneration}
     * @return a {@link ChainGeneration}
     */
    ChainGeneration retrieveComplete(Long id);

    /**
     * @return all {@link ChainGeneration}
     */
    Page<ChainGeneration> retrieveAll(Pageable page);

    /**
     * Delete a {@link ChainGeneration}
     * @param id the {@link ChainGeneration} identifier 
     */
    void delete(Long id);

    /**
     * Delete a {@link ChainGeneration}
     * @param chainGeneration the {@link ChainGeneration} to delete
     */
    void delete(ChainGeneration chainGeneration);

    /**
     * Find a {@link ChainGeneration} by {@link MetaProduct}
     * @param metaProduct the {@link MetaProduct} to find
     * @return the finded {@link ChainGeneration}
     */
    ChainGeneration findByMetaProduct(MetaProduct metaProduct);

    /**
     * Find a {@link Set} of {@link ChainGeneration} that are active and not running
     * @return the finded {@link Set} of {@link ChainGeneration}
     */
    Set<ChainGeneration> findByActiveTrueAndRunningFalse();

    /**
     * Start the acquisition process for a {@link ChainGeneration}
     * @param id the {@link ChainGeneration} identifier 
     * @return the {@link ChainGeneration} has been started
     */
    boolean run(Long id);

    /**
     * Start the acquisition process for a {@link ChainGeneration}
     * @param chain the {@link ChainGeneration} to start
     * @return been started
     */
    boolean run(ChainGeneration chain);
}
