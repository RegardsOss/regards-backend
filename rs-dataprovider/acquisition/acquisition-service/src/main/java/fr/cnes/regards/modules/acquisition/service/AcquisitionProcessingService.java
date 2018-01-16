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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;

/**
 * Acquisition processing service
 *
 * @author Marc Sordi
 *
 */
@Service
@MultitenantTransactional
public class AcquisitionProcessingService implements IAcquisitionProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingService.class);

    @Autowired
    private IAcquisitionProcessingChainRepository acqChainRepository;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IPluginService pluginService;

    @Override
    public AcquisitionProcessingChain getChain(Long id) throws ModuleException {

        AcquisitionProcessingChain chain = acqChainRepository.findOne(id);
        if (chain == null) {
            throw new EntityNotFoundException(id, AcquisitionProcessingChain.class);
        }
        return chain;
    }

    @Override
    public void registerFiles(List<Path> scannedFiles, AcquisitionFileInfo info) throws ModuleException {

        // Register the most recent last modification date
        OffsetDateTime reference = info.getLastModificationDate();

        for (Path filePath : scannedFiles) {
            // Check if file not already registered : TODO

            // Initialize new file
            AcquisitionFile scannedFile = new AcquisitionFile();
            scannedFile.setAcqDate(OffsetDateTime.now());
            scannedFile.setFileInfo(info);
            scannedFile.setFilePath(filePath);
            scannedFile.setChecksumAlgorithm(AcquisitionProcessingChain.CHECKSUM_ALGORITHM);

            // Compute checksum and manage last modification date
            try {
                // Compute last modification date
                reference = getMostRecentDate(reference, filePath);
                // Compute and set checksum
                scannedFile
                        .setChecksum(computeMD5FileChecksum(filePath, AcquisitionProcessingChain.CHECKSUM_ALGORITHM));
                scannedFile.setState(AcquisitionFileState.IN_PROGRESS);
            } catch (NoSuchAlgorithmException | IOException e) {
                // Continue silently bug register error in database
                String errorMessage = String.format("Error registering file : %s", e.getMessage());
                LOGGER.error(errorMessage, e);
                scannedFile.setError(errorMessage);
                scannedFile.setState(AcquisitionFileState.ERROR);
            }

            // Save file
            acqFileRepository.save(scannedFile);
        }

        // Update last modification date
        info.setLastModificationDate(reference);
        fileInfoRepository.save(info);
    }

    /**
     * Compute most recent last modification date
     * @param reference reference date
     * @param filePath file to analyze
     * @return most recent date
     * @throws IOException if error occurs!
     */
    private OffsetDateTime getMostRecentDate(OffsetDateTime reference, Path filePath) throws IOException {

        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
        OffsetDateTime lmd = OffsetDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneOffset.UTC);

        if ((reference == null) || lmd.isAfter(reference)) {
            return lmd;
        }
        return reference;
    }

    /**
     * Compute file checksum
     * @param filePath file path
     * @param checksumAlgorithm checksum algorithm
     * @return checksum
     * @throws IOException if error occurs!
     * @throws NoSuchAlgorithmException if error occurs!
     */
    private static String computeMD5FileChecksum(Path filePath, String checksumAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        InputStream inputStream = Files.newInputStream(filePath);
        return ChecksumUtils.computeHexChecksum(inputStream, checksumAlgorithm);
    }

    @Override
    public void validateFiles(AcquisitionFileInfo fileInfo, Optional<PluginConfiguration> validationPluginConf)
            throws ModuleException {

        // Load in progress files
        List<AcquisitionFile> inProgressFiles = acqFileRepository
                .findByStateAndFileInfo(AcquisitionFileState.IN_PROGRESS, fileInfo);

        if (validationPluginConf.isPresent()) {
            // Get validation plugin
            IValidationPlugin validationPlugin = pluginService.getPlugin(validationPluginConf.get().getId());
            // Apply to all files
            for (AcquisitionFile inProgressFile : inProgressFiles) {
                if (validationPlugin.validate(inProgressFile.getFilePath())) {
                    inProgressFile.setState(AcquisitionFileState.VALID);
                } else {
                    inProgressFile.setState(AcquisitionFileState.INVALID);
                }
                acqFileRepository.save(inProgressFile);
            }
        } else {
            // Files are always considered valid
            for (AcquisitionFile inProgressFile : inProgressFiles) {
                inProgressFile.setState(AcquisitionFileState.VALID);
                acqFileRepository.save(inProgressFile);
            }
        }
    }

    @Override
    public void buildProducts(AcquisitionFileInfo fileInfo, PluginConfiguration productPluginConf)
            throws ModuleException {

        // Load valid files
        List<AcquisitionFile> validFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.VALID,
                                                                                    fileInfo);

        // Get product plugin
        IProductPlugin productPlugin = pluginService.getPlugin(productPluginConf.getId());

        // Compute product name for each valid files
        for (AcquisitionFile validFile : validFiles) {
            String productName = productPlugin.getProductName(validFile.getFilePath());
            // TODO
        }

    }
}
