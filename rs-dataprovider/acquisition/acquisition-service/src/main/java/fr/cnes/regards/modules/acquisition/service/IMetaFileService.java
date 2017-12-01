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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * {@link MetaFile} management service
 * 
 * @author Christophe Mertz
 * 
 */
public interface IMetaFileService {

    /**
     * Create or update a {@link Set} of {@link MetaFile}.<br>
     * If a {@link MetaFile} does not exist in the new {@link Set}, it is delete from the repository.
     * @param newMetaFiles the {@link Set} of {@link MetaFile} to save
     * @param existingMetaFiles the existing {@link Set} of {@link MetaFile} to update
     * @return the saved {@link Set} of {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    Set<MetaFile> createOrUpdate(Set<MetaFile> newMetaFiles, Set<MetaFile> existingMetaFiles) throws ModuleException;

    /**
     * Create or update a {@link Set} of {@link MetaFile}
     * @param metaFiles the {@link Set} of {@link MetaFile} to save
     * @return the saved {@link Set} of {@link ScanDirectory}
     * @throws ModuleException if error occurs!
     */
    Set<MetaFile> createOrUpdate(Set<MetaFile> metaFiles) throws ModuleException;

    /**
     * Create or update a {@link MetaFile}
     * @param metaFile the {@link MetaFile} to save
     * @return the saved {@link Set} of {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    MetaFile createOrUpdate(MetaFile metaFile) throws ModuleException;

    /**
     * Save a {@link MetaFile}
     * @param metaFile the {@link MetaFile} to save
     * @return the saved {@link MetaFile}
     */
    MetaFile save(MetaFile metaFile);

    /**
     * Update a {@link MetaFile}
     * @param metafileId the {@link MetaFile} identifier
     * @param metafile the {@link MetaFile} to update
     * @return the updated {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    MetaFile update(Long metafileId, MetaFile metafile) throws ModuleException;

    /**
     * @return all {@link MetaFile}
     */
    Page<MetaFile> retrieveAll(Pageable page);

    /**
     * Retrieve one specified {@link MetaFile}
     * @param id {@link MetaFile}
     */
    MetaFile retrieve(Long id);

    /**
     * Delete a {@link MetaFile}
     * @param id the {@link MetaFile} identifer of the {@link MetaFile} to delete
     */
    void delete(Long id);

    /**
     * Delete a {@link MetaFile}
     * @param metaFile the {@link MetaFile} to delete
     */
    void delete(MetaFile metaFile);
}
