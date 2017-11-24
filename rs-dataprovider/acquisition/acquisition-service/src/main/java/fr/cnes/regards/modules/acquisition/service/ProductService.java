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
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    private final IProductRepository productRepository;

    public ProductService(IProductRepository repository) {
        this.productRepository = repository;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product retrieve(Long id) {
        return productRepository.findOne(id);
    }

    @Override
    public Product retrieve(String productName) {
        return productRepository.findCompleteByProductName(productName);
    }

    @Override
    public Page<Product> retrieveAll(Pageable page) {
        return productRepository.findAll(page);
    }

    @Override
    public void delete(Long id) {
        productRepository.delete(id);
    }

    @Override
    public void delete(Product product) {
        productRepository.delete(product);
    }

    @Override
    public Set<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public Set<Product> findBySendedAndStatusIn(Boolean sended, ProductStatus... status) {
        return productRepository.findBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductStatus... status) {
        return productRepository.findDistinctIngestChainBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductStatus... status) {
        return productRepository.findDistinctSessionByIngestChainAndSendedAndStatusIn(ingestChain, sended, status);
    }

    @Override
    public Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session,
            Boolean sended, Pageable pageable, ProductStatus... status) {
        return productRepository.findAllByIngestChainAndSessionAndSendedAndStatusIn(ingestChain, session, sended,
                                                                                    pageable, status);
    }

}
