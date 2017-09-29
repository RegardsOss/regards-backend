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

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * 
 * @author Christophe Mertz
 * 
 */
public interface IAcquisitionFileService {

    AcquisitionFile save(AcquisitionFile acqFile);

    /**
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> retrieveAll();
    
    /**
     * Find the {@link AcquisitionFile} for a {@link MetaFile}
     * @param metaFile
     * @return a {@link List} of {@link AcquisitionFile}
     */
    public List<AcquisitionFile> findByMetaFile(MetaFile metaFile);

    /**
     * Retrieve one specified {@link AcquisitionFile}
     * @param id of a {@link AcquisitionFile}
     * @return the {@link AcquisitionFile} with the specified id
     */
    AcquisitionFile retrieve(Long id);

    /**
     * Delete a {@link AcquisitionFile}
     * @param id of a {@link AcquisitionFile}
     */
    void delete(Long id);
}
