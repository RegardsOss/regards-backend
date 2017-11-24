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

import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 * 
 */
public interface IChainGenerationService {

    ChainGeneration save(ChainGeneration chain);

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

    void delete(Long id);
    
    void delete(ChainGeneration chainGeneration);

    ChainGeneration findByMetaProduct(MetaProduct metaProduct);

    Set<ChainGeneration> findByActiveTrueAndRunningFalse();

    boolean run(Long id);

    boolean run(ChainGeneration chain);
}
