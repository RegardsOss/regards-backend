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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Acquisition processing service interface
 *
 * @author Marc Sordi
 *
 */
public interface IAcquisitionProcessingService {

    /**
     * Retrieve a processing chain according to its identifier.
     * @param id {@link AcquisitionProcessingChain} identifier
     * @return {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs.
     */
    AcquisitionProcessingChain getChain(Long id) throws ModuleException;

    /**
     * Register detected files in database initializing its metadata
     * @param scannedFiles list of scanned files to register
     * @param info the related file information
     * @throws ModuleException if error occurs
     */
    void registerFiles(List<Path> scannedFiles, AcquisitionFileInfo info) throws ModuleException;

    /**
     * Validate {@link AcquisitionFileState#IN_PROGRESS} files for specified {@link AcquisitionFileInfo}
     * @param fileInfo file info filter
     * @param validationPluginConf optional validation plugin configuration
     * @throws ModuleException if error occurs!
     */
    void validateFiles(AcquisitionFileInfo fileInfo, Optional<PluginConfiguration> validationPluginConf)
            throws ModuleException;

    /**
     * Build products according to {@link AcquisitionFileState#VALID} files of specified {@link AcquisitionFileInfo}
     * @param fileInfo file info filter
     * @param productPluginConf required product plugin configuration
     * @throws ModuleException if error occurs!
     */
    void buildProducts(AcquisitionFileInfo fileInfo, PluginConfiguration productPluginConf) throws ModuleException;
}
