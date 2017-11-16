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

import java.util.ArrayList;
import java.util.List;

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
    public List<Product> retrieveAll() {
        final List<Product> products = new ArrayList<>();
        productRepository.findAll().forEach(c -> products.add(c));
        return products;
    }

    @Override
    public void delete(Long id) {
        this.productRepository.delete(id);
    }

    @Override
    public Product retrieve(String productName) {
        return productRepository.findCompleteByProductName(productName);
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public List<Product> findBySavedAndStatusIn(Boolean saved, ProductStatus... status) {
        return productRepository.findBySavedAndStatusIn(saved, status);
    }

}
