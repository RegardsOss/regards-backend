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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
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
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

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

    @Autowired
    private IProductService productService;

    @Override
    public AcquisitionProcessingChain getChain(Long id) throws ModuleException {
        AcquisitionProcessingChain chain = acqChainRepository.findOne(id);
        if (chain == null) {
            throw new EntityNotFoundException(id, AcquisitionProcessingChain.class);
        }
        return chain;
    }

    @Override
    public AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Check no identifier
        if (processingChain.getId() != null) {
            throw new EntityInvalidException(
                    String.format("New chain %s must not already have and identifier.", processingChain.getLabel()));
        }

        // Prevent bad values
        processingChain.setRunning(Boolean.FALSE);
        processingChain.setLastActivationDate(null);

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check no identifier
            if (fileInfo.getId() != null) {
                throw new EntityInvalidException(
                        String.format("A file information must not already have and identifier."));
            }

            // Prevent bad value
            fileInfo.setLastModificationDate(null);
            // Manage scan plugin conf
            createPluginConfiguration(fileInfo.getScanPlugin());
            // Save file info
            fileInfoRepository.save(fileInfo);
        }

        // Manage validation plugin conf
        if (processingChain.getValidationPluginConf().isPresent()) {
            createPluginConfiguration(processingChain.getValidationPluginConf().get());
        }

        // Manage product plugin conf
        createPluginConfiguration(processingChain.getProductPluginConf());

        // Manage generate SIP plugin conf
        createPluginConfiguration(processingChain.getGenerateSipPluginConf());

        // Manage post process SIP plugin conf
        if (processingChain.getPostProcessSipPluginConf().isPresent()) {
            createPluginConfiguration(processingChain.getPostProcessSipPluginConf().get());
        }

        // Save new chain
        return acqChainRepository.save(processingChain);
    }

    private PluginConfiguration createPluginConfiguration(PluginConfiguration pluginConfiguration)
            throws ModuleException {
        // Check no identifier. For each new chain, we force plugin configuration creation. A configuration cannot be
        // reused.
        if (pluginConfiguration.getId() != null) {
            throw new EntityInvalidException(
                    String.format("Plugin configuration %s must not already have and identifier.",
                                  pluginConfiguration.getLabel()));
        }
        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    @Override
    public AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Check already exists
        if (!acqChainRepository.exists(processingChain.getId())) {
            throw new EntityNotFoundException(processingChain.getLabel(), IngestProcessingChain.class);
        }

        List<Optional<PluginConfiguration>> confsToRemove = new ArrayList<>();

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check identifier
            if (fileInfo.getId() == null) {
                throw new EntityInvalidException(String.format("A file information must already have and identifier."));
            }
            // Manage scan plugin conf
            // TODO update scan plugin and clean old conf if necessary
            // Save file info
            fileInfoRepository.save(fileInfo);
        }

        // Manage validation plugin conf
        // TODO update plugin and clean old conf if necessary
        if (processingChain.getValidationPluginConf().isPresent()) {
            createPluginConfiguration(processingChain.getValidationPluginConf().get());
        }

        // Manage product plugin conf
        // TODO update scan plugin and clean old conf if necessary
        createPluginConfiguration(processingChain.getProductPluginConf());

        // Manage generate SIP plugin conf
        // TODO update scan plugin and clean old conf if necessary
        createPluginConfiguration(processingChain.getGenerateSipPluginConf());

        // Manage post process SIP plugin conf
        // TODO update scan plugin and clean old conf if necessary
        if (processingChain.getPostProcessSipPluginConf().isPresent()) {
            createPluginConfiguration(processingChain.getPostProcessSipPluginConf().get());
        }

        // Save new chain
        acqChainRepository.save(processingChain);

        // Clean unused plugin configuration after chain update avoiding foreign keys constraints restrictions.
        for (Optional<PluginConfiguration> confToRemove : confsToRemove) {
            if (confToRemove.isPresent()) {
                pluginService.deletePluginConfiguration(confToRemove.get().getId());
            }
        }

        return processingChain;

        //
        // // Manage plugin configuration
        // // ---------------------------
        // // Pre-processing plugine
        // Optional<PluginConfiguration> existing = ingestChainRepository
        // .findOnePreProcessingPluginByName(chainToUpdate.getName());
        // confsToRemove.add(updatePluginConfiguration(chainToUpdate.getPreProcessingPlugin(), existing));
        // // Validation plugin
        // existing = ingestChainRepository.findOneValidationPluginByName(chainToUpdate.getName());
        // confsToRemove.add(updatePluginConfiguration(Optional.of(chainToUpdate.getValidationPlugin()), existing));
        // // Generation plugin
        // existing = ingestChainRepository.findOneGenerationPluginByName(chainToUpdate.getName());
        // confsToRemove.add(updatePluginConfiguration(Optional.of(chainToUpdate.getGenerationPlugin()), existing));
        // // Tag plugin
        // existing = ingestChainRepository.findOneTagPluginByName(chainToUpdate.getName());
        // confsToRemove.add(updatePluginConfiguration(chainToUpdate.getTagPlugin(), existing));
        // // Post-processing plugin
        // existing = ingestChainRepository.findOnePostProcessingPluginByName(chainToUpdate.getName());
        // confsToRemove.add(updatePluginConfiguration(chainToUpdate.getPostProcessingPlugin(), existing));
        //
        // // Update chain
        // ingestChainRepository.save(chainToUpdate);
        //
        // // Clean unused plugin configuration after chain update avoiding foreign keys constraints restrictions.
        // for (Optional<PluginConfiguration> confToRemove : confsToRemove) {
        // if (confToRemove.isPresent()) {
        // pluginService.deletePluginConfiguration(confToRemove.get().getId());
        // }
        // }
        //
        // return chainToUpdate;
    }

    /**
     * Create or update a plugin configuration cleaning old one if necessary
     * @param pluginConfiguration new plugin configuration or update
     * @param existing existing plugin configuration
     * @return configuration to remove because it is no longer used
     * @throws ModuleException if error occurs!
     */
    private Optional<PluginConfiguration> updatePluginConfiguration(Optional<PluginConfiguration> pluginConfiguration,
            Optional<PluginConfiguration> existing) throws ModuleException {

        Optional<PluginConfiguration> confToRemove = Optional.empty();

        if (pluginConfiguration.isPresent()) {
            PluginConfiguration conf = pluginConfiguration.get();
            if (conf.getId() == null) {
                // Delete previous configuration if exists
                confToRemove = existing;
                // Save new configuration
                pluginService.savePluginConfiguration(conf);
            } else {
                // Update configuration
                pluginService.updatePluginConfiguration(conf);
            }
        } else {
            // Delete previous configuration if exists
            confToRemove = existing;
        }

        return confToRemove;
    }

    @Override
    public void scanAndRegisterFiles(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Launch file scanning for each file information
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            // Get plugin instance
            IScanPlugin scanPlugin = pluginService.getPlugin(fileInfo.getScanPlugin().getId());
            // Launch scanning
            List<Path> scannedFiles = scanPlugin.scan(Optional.ofNullable(fileInfo.getLastModificationDate()));
            // Register scanned files
            registerFiles(scannedFiles, fileInfo);
        }
    }

    private void registerFiles(List<Path> scannedFiles, AcquisitionFileInfo info) throws ModuleException {

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
    public void validateFiles(AcquisitionProcessingChain processingChain) throws ModuleException {

        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            // Load in progress files
            List<AcquisitionFile> inProgressFiles = acqFileRepository
                    .findByStateAndFileInfo(AcquisitionFileState.IN_PROGRESS, fileInfo);

            if (processingChain.getValidationPluginConf().isPresent()) {
                // Get validation plugin
                IValidationPlugin validationPlugin = pluginService
                        .getPlugin(processingChain.getValidationPluginConf().get().getId());
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
    }

    @Override
    public void buildProducts(AcquisitionProcessingChain processingChain) throws ModuleException {

        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            // Load valid files
            List<AcquisitionFile> validFiles = acqFileRepository.findByStateAndFileInfo(AcquisitionFileState.VALID,
                                                                                        fileInfo);

            // Get product plugin
            IProductPlugin productPlugin = pluginService.getPlugin(processingChain.getProductPluginConf().getId());

            // Compute product name for each valid files
            for (AcquisitionFile validFile : validFiles) {
                String productName = productPlugin.getProductName(validFile.getFilePath());
                // FIXME manage session?
                productService.linkAcquisitionFileToProduct(null, validFile, productName, processingChain);
            }
        }
    }
}
