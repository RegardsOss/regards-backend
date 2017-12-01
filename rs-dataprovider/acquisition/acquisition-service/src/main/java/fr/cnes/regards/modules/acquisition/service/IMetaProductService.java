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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link MetaProduct} management service
 * 
 * @author Christophe Mertz
 * 
 */
public interface IMetaProductService {

    /**
     * Create or update a {@link MetaProduct}
     * @param chain {@link ChainGeneration}
     * @throws ModuleException 
     */
    MetaProduct createOrUpdateMetaProduct(MetaProduct metaProduct) throws ModuleException;

    MetaProduct save(MetaProduct metaProduct);

    MetaProduct update(Long metaproductId, MetaProduct metaproduct) throws ModuleException;

    /**
     * @return all {@link MetaProduct}
     */
    Page<MetaProduct> retrieveAll(Pageable page);

    /**
     * Retrieve one specified {@link MetaProduct}
     * @param id {@link MetaProduct}
     */
    MetaProduct retrieve(Long id);

    /**
     * Retrieve one specified {@link MetaProduct}
     * @param label {@link MetaProduct}
     */
    MetaProduct retrieve(String label);

    /**
     * Retrieve one specified {@link MetaProduct} with all LAZU attributes
     * @param id {@link MetaProduct}
     */
    MetaProduct retrieveComplete(Long id);

    void delete(Long id);

    void delete(MetaProduct metaProduct);
}
