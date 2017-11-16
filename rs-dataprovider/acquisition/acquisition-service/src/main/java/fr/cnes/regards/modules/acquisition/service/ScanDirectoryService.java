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
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ScanDirectoryService implements IScanDirectoryService {

    private final IScanDirectoryRepository scandirRepository;

    public ScanDirectoryService(IScanDirectoryRepository repository) {
        super();
        this.scandirRepository = repository;
    }

    @Override
    public ScanDirectory save(ScanDirectory scanDir) {
        return scandirRepository.save(scanDir);
    }

    @Override
    public ScanDirectory retrieve(Long id) {
        return scandirRepository.findOne(id);
    }

    @Override
    public List<ScanDirectory> retrieveAll() {
        final List<ScanDirectory> chains = new ArrayList<>();
        scandirRepository.findAll().forEach(c -> chains.add(c));
        return chains;
    }

    @Override
    public void delete(Long id) {
        this.scandirRepository.delete(id);
    }

}
