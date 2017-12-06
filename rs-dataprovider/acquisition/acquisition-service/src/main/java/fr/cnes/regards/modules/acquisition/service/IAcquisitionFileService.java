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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

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
    Page<AcquisitionFile> retrieveAll(Pageable page);

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

    /**
     * Delete a {@link AcquisitionFile}
     * @param acquisitionFile the {@link AcquisitionFile} to delete
     */
    void delete(AcquisitionFile acquisitionFile);

    /**
     * Find the {@link AcquisitionFile} for a {@link MetaFile}
     * @param metaFile
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> findByMetaFile(MetaFile metaFile);

    List<AcquisitionFile> findByStatus(AcquisitionFileStatus status);

    List<AcquisitionFile> findByStatusAndMetaFile(AcquisitionFileStatus status, MetaFile metaFile);

    List<AcquisitionFile> findByProduct(Product product);

    /**
     * Set the status of all the {@link AcquisitionFile} to {@link AcquisitionFileStatus#IN_PROGRESS} and set the last acquisition date of the current {@link AcquisitionProcessingChain}.</br>
     * If a {@link AcquisitionFile} does not exixt it is creates and persists.
     *   
     * Save all the {@link AcquisitionFile} and the {@link AcquisitionProcessingChain}.
     * 
     * @param acquisitionFiles a {@link Set} of {@link AcquisitionFile}
     * @param chain the current {@link AcquisitionProcessingChain}
     */
    void saveAcqFilesAndChain(Set<AcquisitionFile> acquisitionFiles, AcquisitionProcessingChain chain) throws ModuleException;

    /**
     * Save the current {@link AcquisitionFile} and the associated {@link Product}.
     * @param acqFile the current {@link AcquisitionFile}
     */
    void saveAcqFileAndProduct(AcquisitionFile acqFile);

    /**
     * Calculus the {@link AcquisitionFileStatus} of the current {@link AcquisitionFile} and save the {@link AcquisitionFile} and the {@link Product}.<br>
     * <li>If the result is <code>true</code> the status is set to {@link AcquisitionFileStatus#VALID}</br></br>
     * <li>If the result is <code>false</code> the status is set to {@link AcquisitionFileStatus#INVALID}</br></br>
     * @param result <code>true</code> if the check result of current file is correct   
     * @param session the current session
     * @param acqFile the current {@link AcquisitionFile}
     * @param productName the {@link Product} name
     * @param metaProduct the {@link MetaProduct} of the current {@link Product}
     * @param ingestChain the current ingest processing chain
     */
    void checkFileStatus(boolean result, String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain);

}
