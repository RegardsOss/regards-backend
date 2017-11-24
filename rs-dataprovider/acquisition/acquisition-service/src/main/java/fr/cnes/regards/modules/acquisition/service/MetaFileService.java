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
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class MetaFileService implements IMetaFileService {

    private final IMetaFileRepository metaFileRepository;

    public MetaFileService(IMetaFileRepository repository) {
        super();
        this.metaFileRepository = repository;
    }

    @Override
    public MetaFile save(MetaFile metaFile) {
        return metaFileRepository.save(metaFile);
    }

    @Override
    public MetaFile retrieve(Long id) {
        return metaFileRepository.findOne(id);
    }

    @Override
    public Page<MetaFile> retrieveAll(Pageable page) {
        return metaFileRepository.findAll(page);
    }

    @Override
    public void delete(Long id) {
        metaFileRepository.delete(id);
    }

    @Override
    public void delete(MetaFile metaFile) {
        metaFileRepository.delete(metaFile);
    }

}
