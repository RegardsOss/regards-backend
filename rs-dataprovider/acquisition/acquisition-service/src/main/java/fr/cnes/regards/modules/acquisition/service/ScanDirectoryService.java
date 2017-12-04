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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * {@link ScanDirectory} service
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ScanDirectoryService implements IScanDirectoryService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanDirectoryService.class);

    /**
     * {@link ScanDirectory} repository
     */
    private final IScanDirectoryRepository scandirRepository;

    /**
     * The Default constructor 
     * @param repository a bean of {@link IScanDirectoryRepository}
     */
    public ScanDirectoryService(IScanDirectoryRepository repository) {
        super();
        this.scandirRepository = repository;
    }

    @Override
    public ScanDirectory save(ScanDirectory scanDir) {
        return scandirRepository.save(scanDir);
    }

    @Override
    public Set<ScanDirectory> createOrUpdate(Set<ScanDirectory> newScanDirectories) throws ModuleException {
        return createOrUpdate(newScanDirectories, null);
    }

    @Override
    public Set<ScanDirectory> createOrUpdate(Set<ScanDirectory> newScanDirectories,
            Set<ScanDirectory> existingScanDirectories) throws ModuleException {
        for (ScanDirectory scanDir : newScanDirectories) {
            createOrUpdate(scanDir);
        }

        deletUnusedScanDirectories(newScanDirectories, existingScanDirectories);

        return newScanDirectories;
    }

    @Override
    public ScanDirectory createOrUpdate(ScanDirectory scanDirectory) throws ModuleException {
        if (scanDirectory == null) {
            return null;
        }

        if (scanDirectory.getId() == null) {
            // It is a new MetaProduct --> create a new
            return this.save(scanDirectory);
        } else {
            ScanDirectory existingScanDirectory = this.retrieve(scanDirectory.getId());

            if (existingScanDirectory.equals(scanDirectory)) {
                // it is the same --> just return it
                return scanDirectory;
            } else {
                // it is different --> update it
                return this.save(scanDirectory);
            }
        }
    }

    /**
     * Delete from a {@link Set} of {@link ScanDirectory} the {@link ScanDirectory} not present in a {@link Set} of {@link ScanDirectory}.
     * @param newScanDirectories a {@link Set} of {@link ScanDirectory}
     * @param existingScanDirectories a {@link Set} of {@link ScanDirectory}
     */
    private void deletUnusedScanDirectories(Set<ScanDirectory> newScanDirectories,
            Set<ScanDirectory> existingScanDirectories) {

        if (existingScanDirectories == null || existingScanDirectories.isEmpty()) {
            return;
        }

        // It is a modification
        Set<ScanDirectory> toDelete = new HashSet<>();

        for (ScanDirectory aScanDir : existingScanDirectories) {
            boolean isPresent = false;
            for (ScanDirectory aNewScanDir : newScanDirectories) {
                if (!isPresent) {
                    LOGGER.info("new scan dir id:{}  -  existing scan dir id:{}", aNewScanDir.getId(),
                                aScanDir.getId());
                    isPresent = aNewScanDir.getId().equals(aScanDir.getId());
                }
            }
            if (!isPresent) {
                // the existing scan dir does not exist in the new Set of scan dir
                toDelete.add(aScanDir);
            }
        }

        // delete the scan dir not found in the new Set of scan dir
        for (ScanDirectory aScanDir : toDelete) {
            scandirRepository.delete(aScanDir);
        }
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
