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

import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * 
 * {@link ScanDirectory} management service
 * 
 * @author Christophe Mertz
 * 
 */
public interface IScanDirectoryService {

    /**
     * Create or update a {@link Set} of {@link ScanDirectory}.<br>
     * If a {@link ScanDirectory} does not exist in the new {@link Set}, it is delete from the repository.
     * @param newScanDirectories the {@link Set} of {@link ScanDirectory} to save
     * @param existingScanDirectories the existing {@link Set} of {@link ScanDirectory} to update
     * @return the saved {@link Set} of {@link ScanDirectory
     * @throws ModuleException if error occurs!
     */
    Set<ScanDirectory> createOrUpdate(Set<ScanDirectory> newScanDirectories, Set<ScanDirectory> existingScanDirectories)
            throws ModuleException;

    /**
     * Create or update a {@link Set} of {@link ScanDirectory}
     * @param newScanDirectories the {@link Set} of {@link ScanDirectory} to save
     * @return the saved {@link Set} of {@link ScanDirectory}
     * @throws ModuleException if error occurs!
     */
    Set<ScanDirectory> createOrUpdate(Set<ScanDirectory> newScanDirectories) throws ModuleException;

    /**
     * Create or update a {@link ScanDirectory}
     * @param scanDirectory the {@link ScanDirectory} to save
     * @return
     * @throws ModuleException if error occurs!
     */
    ScanDirectory createOrUpdate(ScanDirectory scanDirectory) throws ModuleException;

    /**
     * Save a {@link ScanDirectory}
     * @param scanDir the {@link ScanDirectory} to save
     * @return the saved {@link ScanDirectory}
     */
    ScanDirectory save(ScanDirectory scanDir);

    /**
     * @return all {@link ScanDirectory}
     */
    List<ScanDirectory> retrieveAll();

    /**
     * Retrieve one specified {@link ScanDirectory}
     * @param id {@link ScanDirectory}
     */
    ScanDirectory retrieve(Long id);

    /**
     * Delete a {@link ScanDirectory}
     * @param id the {@link ScanDirectory} identifier of the {@link ScanDirectory} to delete
     */
    void delete(Long id);
}
