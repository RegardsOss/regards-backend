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
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * @author Christophe Mertz
 *
 */
public interface IAcquisitionFileService {

    /**
     * Save a {@link AcquisitionFile}
     * @param acqFile the {@link AcquisitionFile} to save
     * @return the saved {@link AcquisitionFile}
     */
    AcquisitionFile save(AcquisitionFile acqFile);

    /**
     *
     * @param page
     * @return a {@link Page} of {@link AcquisitionFile}
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
     * Find a {@link List} of {@link AcquisitionFile} for a {@link MetaFile}
     * @param metaFile the {@link MetaFile} to search
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> findByMetaFile(MetaFile metaFile);

    /**
     * Find a {@link List} of {@link AcquisitionFile} for with a {@link AcquisitionFileState}
     * @param status the {@link AcquisitionFileState} to search
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> findByStatus(AcquisitionFileState status);

    /**
     * Find a {@link List} of {@link AcquisitionFile} for with a {@link AcquisitionFileState} and a {@link MetaFile}
     * @param status the {@link AcquisitionFileState} to search
     * @param metaFile the {@link MetaFile} to search
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> findByStatusAndMetaFile(AcquisitionFileState status, MetaFile metaFile);

    /**
     * Find a {@link List} of {@link AcquisitionFile} for a {@link Product}
     * @param productName the {@link Product} name to search
     * @return a {@link List} of {@link AcquisitionFile}
     */
    List<AcquisitionFile> findByProduct(String productName);

    /**
     * Set the status of all the {@link AcquisitionFile} to {@link AcquisitionFileState#IN_PROGRESS} and set the last
     * acquisition date of the current {@link AcquisitionProcessingChain2}.</br>
     * If a {@link AcquisitionFile} does not exixt it is creates and persists.
     *
     * Save all the {@link AcquisitionFile} and the {@link AcquisitionProcessingChain2}.
     *
     * @param acquisitionFiles a {@link Set} of {@link AcquisitionFile}
     * @param chain the current {@link AcquisitionProcessingChain2}
     */
    void saveAcqFilesAndChain(Set<AcquisitionFile> acquisitionFiles, AcquisitionProcessingChain2 chain)
            throws ModuleException;

    /**
     * Calculus the {@link AcquisitionFileState} of the current {@link AcquisitionFile} and save the
     * {@link AcquisitionFile} and the {@link Product}.<br>
     * <li>If the result is <code>true</code> the status is set to {@link AcquisitionFileState#VALID}</br>
     * </br>
     * <li>If the result is <code>false</code> the status is set to {@link AcquisitionFileState#INVALID}</br>
     * </br>
     * @param result <code>true</code> if the check result of current file is correct
     * @param session the current session
     * @param acqFile the current {@link AcquisitionFile}
     * @param productName the {@link Product} name
     * @param metaProduct the {@link MetaProduct} of the current {@link Product}
     * @param ingestChain the current ingest processing chain
     */
    void checkFileStatus(boolean result, String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain) throws ModuleException;

}
