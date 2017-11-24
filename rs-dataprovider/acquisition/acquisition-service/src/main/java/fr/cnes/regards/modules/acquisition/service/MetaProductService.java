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
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class MetaProductService implements IMetaProductService {

    private final IMetaProductRepository metaProductRepository;

    public MetaProductService(IMetaProductRepository repository) {
        super();
        this.metaProductRepository = repository;
    }

    @Override
    public MetaProduct save(MetaProduct metaProduct) {
        return metaProductRepository.save(metaProduct);
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
