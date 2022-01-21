/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;

/**
 * Ingest processing service interface
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public interface IIngestProcessingChainService {

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
