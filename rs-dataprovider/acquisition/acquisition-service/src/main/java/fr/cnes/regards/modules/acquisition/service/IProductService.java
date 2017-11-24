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

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;

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
    Product retrieve(String productName);

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

    Set<Product> findByStatus(ProductStatus status);

    Set<Product> findBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductStatus... status);

    Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session, Boolean sended,
            Pageable pageable, ProductStatus... status);

}
