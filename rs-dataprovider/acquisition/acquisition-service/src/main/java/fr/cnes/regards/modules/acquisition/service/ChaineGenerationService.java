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
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ChaineGenerationService implements IChainGenerationService {

    private final IChainGenerationRepository chainRepository;

    public ChaineGenerationService(IChainGenerationRepository repository) {
        super();
        this.chainRepository = repository;
    }

    @Override
    public ChainGeneration save(ChainGeneration chain) {
        return chainRepository.save(chain);
    }

    @Override
    public List<ChainGeneration> retrieveAll() {
        final List<ChainGeneration> chains = new ArrayList<>();
        chainRepository.findAll().forEach(c -> chains.add(c));
        return chains;
    }

    @Override
    public ChainGeneration retrieve(Long id) {
        return chainRepository.findOne(id);
    }

    @Override
    public void delete(Long id) {
        chainRepository.delete(id);
    }

}
