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

import java.util.List;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 * 
 */
public interface IMetaProductService {

    MetaProduct save(MetaProduct metaProduct);

    /**
     * @return all {@link MetaProduct}
     */
    List<MetaProduct> retrieveAll();

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
     * Retrieve one specified {@link MetaProduct} with all TODO
     * @param id {@link MetaProduct}
     */
    MetaProduct retrieveComplete(Long id);

    void delete(Long id);
}
