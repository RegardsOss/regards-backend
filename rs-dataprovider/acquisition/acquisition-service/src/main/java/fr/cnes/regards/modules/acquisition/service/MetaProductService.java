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
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *{@link MetaProduct} service
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class MetaProductService implements IMetaProductService {

    private final IMetaProductRepository metaProductRepository;

    private final IMetaFileService metaFileService;

    public MetaProductService(IMetaProductRepository repository, IMetaFileService metaFileService) {
        super();
        this.metaProductRepository = repository;
        this.metaFileService = metaFileService;
    }

    @Override
    public MetaProduct save(MetaProduct metaProduct) {
        return metaProductRepository.save(metaProduct);
    }

    @Override
    public MetaProduct update(Long metaproductId, MetaProduct metaProduct) throws ModuleException {
        if (!metaproductId.equals(metaProduct.getId())) {
            throw new EntityInconsistentIdentifierException(metaproductId, metaProduct.getId(), metaProduct.getClass());
        }
        if (!metaProductRepository.exists(metaproductId)) {
            throw new EntityNotFoundException(metaproductId, ChainGeneration.class);
        }
        return createOrUpdateMetaProduct(metaProduct);
    }

    @Override
    public MetaProduct createOrUpdateMetaProduct(MetaProduct metaProduct) throws ModuleException {
        if (metaProduct == null) {
            return null;
        }

        metaProduct.setMetaFiles(metaFileService.createOrUpdate(metaProduct.getMetaFiles()));

        if (metaProduct.getId() == null) {
            // It is a new MetaProduct --> create a new
            return this.save(metaProduct);
        } else {
            MetaProduct existingMetaProduct = this.retrieve(metaProduct.getId());

            if (existingMetaProduct.equals(metaProduct)) {
                // it is the same --> just return it
                return metaProduct;
            } else {
                // it is different --> update it
                return this.save(metaProduct);
            }
        }
    }

    @Override
    public MetaProduct retrieve(Long id) {
        return metaProductRepository.findOne(id);
    }

    @Override
    public MetaProduct retrieve(String label) {
        return metaProductRepository.findByLabel(label);
    }

    @Override
    public Page<MetaProduct> retrieveAll(Pageable page) {
        return metaProductRepository.findAll(page);
    }

    @Override
    public MetaProduct retrieveComplete(Long id) {
        return metaProductRepository.findCompleteById(id);
    }

    @Override
    public void delete(Long id) {
        metaProductRepository.delete(id);
    }

    @Override
    public void delete(MetaProduct metaProduct) {
        metaProductRepository.delete(metaProduct);
    }

}
