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

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * {@link MetaFile} service
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class MetaFileService implements IMetaFileService {

    private final IMetaFileRepository metaFileRepository;

    private final IScanDirectoryService scanDirectoryService;

    public MetaFileService(IMetaFileRepository repository, IScanDirectoryService scanDirectoryService) {
        super();
        this.metaFileRepository = repository;
        this.scanDirectoryService = scanDirectoryService;
    }

    @Override
    public MetaFile save(MetaFile metaFile) {
        return metaFileRepository.save(metaFile);
    }

    @Override
    public MetaFile update(Long metafileId, MetaFile metafile) throws ModuleException {
        if (!metafileId.equals(metafile.getId())) {
            throw new EntityInconsistentIdentifierException(metafileId, metafile.getId(), metafile.getClass());
        }
        if (!metaFileRepository.exists(metafileId)) {
            throw new EntityNotFoundException(metafileId, ChainGeneration.class);
        }

        return createOrUpdate(metafile);
    }

    @Override
    public Set<MetaFile> createOrUpdate(Set<MetaFile> metaFiles) throws ModuleException {
        return createOrUpdate(metaFiles, null);
    }

    @Override
    public Set<MetaFile> createOrUpdate(Set<MetaFile> newMetaFiles, Set<MetaFile> existingMetaFiles)
            throws ModuleException {
        for (MetaFile metaFile : newMetaFiles) {
            createOrUpdate(metaFile);
        }

        deletUnusedMetaFiles(newMetaFiles, existingMetaFiles);

        return newMetaFiles;
    }

    @Override
    public MetaFile createOrUpdate(MetaFile metaFile) throws ModuleException {
        if (metaFile == null) {
            return null;
        }

        metaFile.setScanDirectories(scanDirectoryService.createOrUpdate(metaFile.getScanDirectories()));

        if (metaFile.getId() == null) {
            // It is a new MetaFile --> create a new
            metaFile.setScanDirectories(scanDirectoryService.createOrUpdate(metaFile.getScanDirectories()));
            return this.save(metaFile);
        } else {
            MetaFile existingMetaFile = this.retrieve(metaFile.getId());
            metaFile.setScanDirectories(scanDirectoryService.createOrUpdate(metaFile.getScanDirectories(),
                                                                            existingMetaFile.getScanDirectories()));
            if (existingMetaFile.equals(metaFile)) {
                // it is the same --> just return it
                return metaFile;
            } else {
                // it is different --> update it
                return this.save(metaFile);
            }
        }
    }

    private void deletUnusedMetaFiles(Set<MetaFile> newMetaFiles, Set<MetaFile> existingMetaFiles) {
        if (existingMetaFiles == null) {
            return;
        }

        // It is a modification
        Set<MetaFile> toDelete = new HashSet<>();

        for (MetaFile aScanDir : existingMetaFiles) {
            boolean isPresent = false;
            for (MetaFile aNewScanDir : newMetaFiles) {
                if (!isPresent) {
                    isPresent = aNewScanDir.getId().equals(aScanDir.getId());
                }
            }
            if (!isPresent) {
                // the existing scan dir does not exist in the new Set of scan dir
                toDelete.add(aScanDir);
            }
        }

        // delete the scan dir not found in the new Set of scan dir
        for (MetaFile aScanDir : toDelete) {
            metaFileRepository.delete(aScanDir);
        }
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
