/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.dao.AcquisitionProcessingChainSpecifications;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.StopChainThread;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;

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

    @Autowired
    private IAcquisitionFileService acqFileService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAcquisitionProcessingService self;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ITemplateService templateService;

    @SuppressWarnings("unused")
    @Autowired
    private EntityManager entityManager;

    /**
     * Compute file checksum
     * @param filePath file path
     * @param checksumAlgorithm checksum algorithm
     * @return checksum
     * @throws IOException if error occurs!
     * @throws NoSuchAlgorithmException if error occurs!
     */
    private static String computeFileChecksum(Path filePath, String checksumAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        InputStream inputStream = Files.newInputStream(filePath);
        return ChecksumUtils.computeHexChecksum(inputStream, checksumAlgorithm);
    }

    @Override
    public Page<AcquisitionProcessingChain> getAllChains(Pageable pageable) throws ModuleException {
        return acqChainRepository.findAll(pageable);
    }

    @Override
    public AcquisitionProcessingChain getChain(Long id) throws ModuleException {
        AcquisitionProcessingChain chain = acqChainRepository.findCompleteById(id);
        if (chain == null) {
            throw new EntityNotFoundException(id, AcquisitionProcessingChain.class);
        }
        // Now load all plugin configuration (avoiding JPA graphs or subgraphs!)
        // For file info
        for (AcquisitionFileInfo fileInfo : chain.getFileInfos()) {
            pluginService.loadPluginConfiguration(fileInfo.getScanPlugin().getId());
        }
        // And others
        pluginService.loadPluginConfiguration(chain.getValidationPluginConf().getId());
        pluginService.loadPluginConfiguration(chain.getProductPluginConf().getId());
        pluginService.loadPluginConfiguration(chain.getGenerateSipPluginConf().getId());
        if (chain.getPostProcessSipPluginConf().isPresent()) {
            pluginService.loadPluginConfiguration(chain.getPostProcessSipPluginConf().get().getId());
        }
        return chain;
    }

    @Override
    public List<AcquisitionProcessingChain> getFullChains() throws ModuleException {
        List<AcquisitionProcessingChain> apcs = acqChainRepository.findAll();
        List<AcquisitionProcessingChain> fullChains = new ArrayList<>();
        for (AcquisitionProcessingChain apc : apcs) {
            fullChains.add(getChain(apc.getId()));
        }
        return fullChains;
    }

    @Override
    public Page<AcquisitionProcessingChain> getFullChains(Pageable pageable) throws ModuleException {
        Page<AcquisitionProcessingChain> apcs = acqChainRepository.findAll(pageable);
        List<AcquisitionProcessingChain> fullChains = new ArrayList<>();
        for (AcquisitionProcessingChain apc : apcs) {
            fullChains.add(getChain(apc.getId()));
        }
        return new PageImpl<>(fullChains, pageable, apcs.getTotalElements());
    }

    @Override
    public AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Check no identifier
        if (processingChain.getId() != null) {
            throw new EntityInvalidException(
                    String.format("New chain %s must not already have an identifier.", processingChain.getLabel()));
        }

        // Check mode
        checkProcessingChainMode(processingChain);

        // Prevent bad values
        processingChain.setLocked(Boolean.FALSE);
        processingChain.setLastActivationDate(null);
        processingChain.setLastProductAcquisitionJobInfo(null);

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check no identifier
            if (fileInfo.getId() != null) {
                throw new EntityInvalidException(
                        String.format("A file information must not already have an identifier."));
            }

            // Prevent bad value
            fileInfo.setLastModificationDate(null);
            // Manage scan plugin conf
            createPluginConfiguration(fileInfo.getScanPlugin());
            // Save file info
            fileInfoRepository.save(fileInfo);
        }

        // Manage validation plugin conf
        createPluginConfiguration(processingChain.getValidationPluginConf());

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
                    String.format("Plugin configuration %s must not already have an identifier.",
                                  pluginConfiguration.getLabel()));
        }
        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    @Override
    public AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Check already exists
        if (!acqChainRepository.existsChain(processingChain.getId())) {
            throw new EntityNotFoundException(processingChain.getLabel(), IngestProcessingChain.class);
        }

        // Check mode
        checkProcessingChainMode(processingChain);

        List<Optional<PluginConfiguration>> confsToRemove = new ArrayList<>();
        Optional<PluginConfiguration> existing;

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check identifier
            if (fileInfo.getId() == null) {
                // New file info to create
                // Prevent bad value
                fileInfo.setLastModificationDate(null);
                // Manage scan plugin conf
                createPluginConfiguration(fileInfo.getScanPlugin());
            } else {
                // File info to update
                // Manage scan plugin conf
                existing = fileInfoRepository.findOneScanPlugin(fileInfo.getId());
                confsToRemove.add(updatePluginConfiguration(Optional.of(fileInfo.getScanPlugin()), existing));
            }

            // Save file info
            fileInfoRepository.save(fileInfo);
        }

        // Manage validation plugin conf
        existing = acqChainRepository.findOneValidationPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getValidationPluginConf()), existing));

        // Manage product plugin conf
        existing = acqChainRepository.findOneProductPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getProductPluginConf()), existing));

        // Manage generate SIP plugin conf
        existing = acqChainRepository.findOneGenerateSipPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getGenerateSipPluginConf()), existing));

        // Manage post process SIP plugin conf
        existing = acqChainRepository.findOnePostProcessSipPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(processingChain.getPostProcessSipPluginConf(), existing));

        // Save new chain
        acqChainRepository.save(processingChain);

        // Clean unused plugin configuration after chain update avoiding foreign keys constraints restrictions.
        for (Optional<PluginConfiguration> confToRemove : confsToRemove) {
            if (confToRemove.isPresent()) {
                pluginService.deletePluginConfiguration(confToRemove.get().getId());
            }
        }

        return processingChain;
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

    /**
     * Check if mode is configured properly
     * @param processingChain chain to check
     * @throws ModuleException if bad configuration
     */
    private void checkProcessingChainMode(AcquisitionProcessingChain processingChain) throws ModuleException {

        if (AcquisitionProcessingChainMode.AUTO.equals(processingChain.getMode())
                && (processingChain.getPeriodicity() == null)) {
            throw new EntityInvalidException("Missing periodicity for automatic acquisition processing chain");
        }
    }

    @Override
    public void deleteChain(Long id) throws ModuleException {
        AcquisitionProcessingChain processingChain = getChain(id);

        if (processingChain.isActive()) {
            throw new EntityOperationForbiddenException(
                    String.format("Acquisition processing chain \"%s\" must be disabled to be deleted",
                                  processingChain.getLabel()));
        }

        Page<Product> products;
        Pageable pageable = PageRequest.of(0, AcquisitionProperties.WORKING_UNIT);
        do {
            products = productService.findChainProducts(processingChain, pageable);
            if (products.hasNext()) {
                pageable = products.nextPageable();
            }
            // Delete products cascading to related acquisition files
            if (products.hasContent()) {
                for (Product product : products) {
                    // Unlock jobs
                    if (product.getLastPostProductionJobInfo() != null) {
                        jobInfoService.unlock(product.getLastPostProductionJobInfo());
                    }
                    if (product.getLastSIPGenerationJobInfo() != null) {
                        jobInfoService.unlock(product.getLastSIPGenerationJobInfo());
                    }

                    productService.delete(product);
                }
            }
        } while (products.hasNext());

        // Delete acquisition file infos and its plugin configurations
        for (AcquisitionFileInfo afi : processingChain.getFileInfos()) {
            fileInfoRepository.delete(afi);
        }

        // Delete acquisition processing chain and its plugin configurations
        if (processingChain.getLastProductAcquisitionJobInfo() != null) {
            jobInfoService.unlock(processingChain.getLastProductAcquisitionJobInfo());
        }
        acqChainRepository.delete(processingChain);
    }

    @Override
    public void lockChain(Long id) {
        acqChainRepository.setLocked(Boolean.TRUE, id);
    }

    @Override
    public void unlockChain(Long id) {
        acqChainRepository.setLocked(Boolean.FALSE, id);
    }

    @Override
    public void stopChainJobs(Long processingChainId) throws ModuleException {

        AcquisitionProcessingChain processingChain = getChain(processingChainId);

        if (!processingChain.isLocked()) {
            String message = String.format("Jobs cannot be stopped on unlocked processing chain \"%s\"",
                                           processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        // Stop all active jobs for current processing chain
        JobInfo jobInfo = processingChain.getLastProductAcquisitionJobInfo();
        if ((jobInfo != null) && !jobInfo.getStatus().getStatus().isFinished()) {
            jobInfoService.stopJob(jobInfo.getId());
        }
        productService.stopProductJobs(processingChain);
    }

    @Override
    public boolean isChainJobStoppedAndCleaned(Long processingChainId) throws ModuleException {
        AcquisitionProcessingChain processingChain = getChain(processingChainId);
        JobInfo jobInfo = processingChain.getLastProductAcquisitionJobInfo();
        boolean acqJobStopped = (jobInfo == null) || jobInfo.getStatus().getStatus().isFinished();
        return acqJobStopped && productService.isProductJobStoppedAndCleaned(processingChain);
    }

    @MultitenantTransactional(propagation = Propagation.SUPPORTS)
    @Override
    public AcquisitionProcessingChain stopAndCleanChain(Long processingChainId) throws ModuleException {

        AcquisitionProcessingChain processingChain = self.getChain(processingChainId);

        // Prevent chain from being run during stopping to avoid a real mess
        if (processingChain.isLocked()) {
            // FIXME lock is used for chain start and stop!
            LOGGER.warn("There can be an issue with processing chain locking because already locked!");
        } else {
            self.lockChain(processingChain.getId());
        }

        if (AcquisitionProcessingChainMode.AUTO.equals(processingChain.getMode())) {
            processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
            acqChainRepository.save(processingChain);
        }

        // Stop jobs, wait for jobs to stop then clean related products
        Thread stopChainThread = new StopChainThread(runtimeTenantResolver.getTenant(), processingChain.getId());
        beanFactory.autowireBean(stopChainThread);
        stopChainThread.start();

        return processingChain;
    }

    @Override
    public void startAutomaticChains() {

        // Load all automatic chains
        List<AcquisitionProcessingChain> processingChains = acqChainRepository.findAllBootableAutomaticChains();

        for (AcquisitionProcessingChain processingChain : processingChains) {

            // Check periodicity
            if ((processingChain.getLastActivationDate() != null) && processingChain.getLastActivationDate()
                    .plusSeconds(processingChain.getPeriodicity()).isAfter(OffsetDateTime.now())) {
                LOGGER.debug("Acquisition processing chain \"{}\" will not be started due to periodicity",
                             processingChain.getLabel());
            } else if (processingChain.isLocked()) {
                LOGGER.debug("Acquisition processing chain \"{}\" will not be started because still locked (i.e. working)",
                             processingChain.getLabel());
            } else {
                // Schedule job
                scheduleProductAcquisitionJob(processingChain);
            }
        }
    }

    @Override
    public AcquisitionProcessingChain startManualChain(Long processingChainId) throws ModuleException {

        // Load chain
        AcquisitionProcessingChain processingChain = getChain(processingChainId);

        if (!processingChain.isActive()) {
            String message = String.format("Inactive processing chain \"%s\" cannot be started",
                                           processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        if (AcquisitionProcessingChainMode.AUTO.equals(processingChain.getMode())) {
            String message = String.format("Automatic processing chain \"%s\" cannot be started manuallly",
                                           processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        if (processingChain.isLocked()) {
            String message = String.format("Processing chain \"%s\" already locked", processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        // Schedule job
        scheduleProductAcquisitionJob(processingChain);

        return processingChain;
    }

    /**
     * Schedule a product acquisition job for specified processing chain. Only one job can be scheduled.
     * @param processingChain processing chain
     */
    private void scheduleProductAcquisitionJob(AcquisitionProcessingChain processingChain) {

        // Mark processing chain as running
        lockChain(processingChain.getId());
        processingChain.setLastActivationDate(OffsetDateTime.now());
        acqChainRepository.save(processingChain);

        LOGGER.debug("Scheduling product acquisition job for processing chain \"{}\"", processingChain.getLabel());
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        jobInfo.setClassName(ProductAcquisitionJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());

        jobInfoService.createAsQueued(jobInfo);

        // Release lock
        if (processingChain.getLastProductAcquisitionJobInfo() != null) {
            jobInfoService.unlock(processingChain.getLastProductAcquisitionJobInfo());
        }
        processingChain.setLastProductAcquisitionJobInfo(jobInfo);
        acqChainRepository.save(processingChain);

    }

    @Override
    public void scanAndRegisterFiles(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Launch file scanning for each file information
        Iterator<AcquisitionFileInfo> fileInfoIter = processingChain.getFileInfos().iterator();
        while (fileInfoIter.hasNext() && !Thread.currentThread().isInterrupted()) {
            AcquisitionFileInfo fileInfo = fileInfoIter.next();
            // Get plugin instance
            IScanPlugin scanPlugin;
            try {
                scanPlugin = pluginService.getPlugin(fileInfo.getScanPlugin().getId());
            } catch (NotAvailablePluginConfigurationException e1) {
                LOGGER.error("Unable to run files scan as plugin is disabled");
                throw new ModuleException(e1.getMessage(), e1);
            }

            // Clone scanning date for duplicate prevention
            Optional<OffsetDateTime> scanningDate = Optional.empty();
            if (fileInfo.getLastModificationDate() != null) {
                scanningDate = Optional
                        .of(OffsetDateTime.ofInstant(fileInfo.getLastModificationDate().toInstant(), ZoneOffset.UTC));
            }

            // Do scan
            List<Path> scannedFiles = scanPlugin.scan(scanningDate);

            // Sort list according to last modification date
            Collections.sort(scannedFiles, (file1, file2) -> {
                try {
                    return Files.getLastModifiedTime(file1).compareTo(Files.getLastModifiedTime(file2));
                } catch (IOException e) {
                    LOGGER.warn("Cannot read last modification date", e);
                    return 0;
                }
            });

            long startTime = System.currentTimeMillis();
            int fromIndex = 0;
            int toIndex = AcquisitionProperties.WORKING_UNIT;
            int totalCount = 0;
            while ((toIndex <= scannedFiles.size()) && !Thread.currentThread().isInterrupted()) {
                long transactionStartTime = System.currentTimeMillis();
                // Do it in one transaction
                LOGGER.debug("Trying to register {}/{} of the new file(s) scanned", toIndex, scannedFiles.size());
                int count = self.registerFiles(scannedFiles.subList(fromIndex, toIndex), fileInfo, scanningDate);
                totalCount += count;
                LOGGER.debug("{}/{} new file(s) registered in {} milliseconds", count, toIndex,
                             System.currentTimeMillis() - transactionStartTime);
                fromIndex = toIndex;
                toIndex = toIndex + AcquisitionProperties.WORKING_UNIT;
            }
            // Do it in one transaction
            long transactionStartTime = System.currentTimeMillis();
            List<Path> remainingFiles = scannedFiles.subList(fromIndex, scannedFiles.size());
            LOGGER.debug("Trying to register the last {} of the new file(s) scanned", remainingFiles.size());
            int count = self.registerFiles(remainingFiles, fileInfo, scanningDate);
            totalCount += count;
            LOGGER.debug("{}/{} new file(s) registered in {} milliseconds", count, remainingFiles.size(),
                         System.currentTimeMillis() - transactionStartTime);

            LOGGER.info("{} new file(s) registered in {} milliseconds", totalCount,
                        System.currentTimeMillis() - startTime);
        }

    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public int registerFiles(List<Path> filePaths, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate)
            throws ModuleException {
        int countRegistered = 0;
        Iterator<Path> filePathIter = filePaths.iterator();
        while (filePathIter.hasNext() && !Thread.currentThread().isInterrupted()) {
            if (registerFile(filePathIter.next(), info, scanningDate)) {
                countRegistered++;
            }
        }
        return countRegistered;
    }

    @Override
    public boolean registerFile(Path filePath, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate)
            throws ModuleException {

        // Initialize new file
        AcquisitionFile scannedFile = new AcquisitionFile();
        scannedFile.setAcqDate(OffsetDateTime.now());
        scannedFile.setFileInfo(info);
        scannedFile.setFilePath(filePath);
        scannedFile.setChecksumAlgorithm(AcquisitionProcessingChain.CHECKSUM_ALGORITHM);

        // Compute checksum and manage last modification date
        try {
            // Compute and set checksum
            scannedFile.setChecksum(computeFileChecksum(filePath, AcquisitionProcessingChain.CHECKSUM_ALGORITHM));
            scannedFile.setState(AcquisitionFileState.IN_PROGRESS);

            // Update last modification date
            OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(filePath).toInstant(),
                                                          ZoneOffset.UTC);
            // Prevent duplicates for files in the same second as the scanning one
            if (scanningDate.isPresent() && scanningDate.get().isEqual(lmd)) {
                // Check if file not already registered
                if (acqFileRepository.countByFileInfoAndChecksum(info, scannedFile.getChecksum()) != 0) {
                    LOGGER.debug("Duplicate file with path \"{}\" and checksum \"{}\" detected, skipping registration",
                                 scannedFile.getFilePath(), scannedFile.getChecksum());
                    return false;
                }
            }

            info.setLastModificationDate(lmd);
            fileInfoRepository.save(info);
        } catch (ClosedByInterruptException e) {
            // This exception happens when you try to access files while thread has been interrupted.
            // In this case lets ignore the file and act as we did not scanned it.
            LOGGER.warn(String.format("File %s scanning has been interrupted during acquisition chain execution.",
                                      filePath),
                        e);
        } catch (NoSuchAlgorithmException | IOException e) {
            // Continue silently but register error in database
            String errorMessage = String.format("Error registering file %s : %s", scannedFile.getFilePath().toString(),
                                                e.getMessage());
            LOGGER.error(errorMessage, e);
            scannedFile.setError(errorMessage);
            scannedFile.setState(AcquisitionFileState.ERROR);
        }

        // Save file
        acqFileRepository.save(scannedFile);
        return true;
    }

    @Override
    public void manageRegisteredFiles(AcquisitionProcessingChain processingChain) throws ModuleException {
        while (!Thread.currentThread().isInterrupted() && self.manageRegisteredFilesByPage(processingChain)) {
            // Works as long as there is at least one page left
        }
        // Just trace interruption
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean manageRegisteredFilesByPage(AcquisitionProcessingChain processingChain) throws ModuleException {

        // - Retrieve first page of new registered files
        Page<AcquisitionFile> page = acqFileRepository
                .findByStateAndFileInfoInOrderByAcqDateAsc(AcquisitionFileState.IN_PROGRESS,
                                                           processingChain.getFileInfos(),
                                                           PageRequest.of(0, AcquisitionProperties.WORKING_UNIT));
        LOGGER.debug("Managing next new {} registered files (of {})", page.getNumberOfElements(),
                     page.getTotalElements());
        long startTime = System.currentTimeMillis();

        // Get validation plugin
        IValidationPlugin validationPlugin;
        try {
            validationPlugin = pluginService.getPlugin(processingChain.getValidationPluginConf().getId());
        } catch (NotAvailablePluginConfigurationException e1) {
            throw new ModuleException("Unable to run disabled acquisition chain.", e1);
        }
        List<AcquisitionFile> validFiles = new ArrayList<>();
        List<AcquisitionFile> invalidFiles = new ArrayList<>();
        for (AcquisitionFile inProgressFile : page.getContent()) {
            // Validate files
            if (validationPlugin.validate(inProgressFile.getFilePath())) {
                inProgressFile.setState(AcquisitionFileState.VALID);
                validFiles.add(inProgressFile);
            } else {
                // FIXME move invalid files? Might be delegated to validation plugin!
                inProgressFile.setState(AcquisitionFileState.INVALID);
                acqFileRepository.save(inProgressFile);
                invalidFiles.add(inProgressFile);
            }
        }

        // Send Notification for invalid files
        if (!invalidFiles.isEmpty()) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("invalidFiles", invalidFiles);
            String message;
            try {
                message = templateService.render(AcquisitionTemplateConfiguration.ACQUISITION_INVALID_FILES_TEMPLATE,
                                                 dataMap);
                notificationClient.notify(message, "Acquisition invalid files report", NotificationLevel.WARNING,
                                          MimeTypeUtils.TEXT_HTML, DefaultRole.PROJECT_ADMIN);
            } catch (TemplateException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        LOGGER.debug("Validation of {} file(s) finished with {} valid and {} invalid.", page.getNumberOfElements(),
                     validFiles.size(), page.getNumberOfElements() - validFiles.size());

        // Build and schedule products, for a subset of the current file page
        Set<Product> products = productService.linkAcquisitionFilesToProducts(processingChain, validFiles);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} file(s) handles, {} product(s) created or updated in {} milliseconds",
                         page.getNumberOfElements(), products.size(), System.currentTimeMillis() - startTime);
            // Statistics
            int scheduledProducts = 0;
            int notScheduledProducts = 0;
            for (Product product : products) {
                if (product.getSipState() == ProductSIPState.SCHEDULED) {
                    scheduledProducts++;
                } else {
                    notScheduledProducts++;
                }
            }
            LOGGER.debug("{} product(s) scheduled and {} not.", scheduledProducts, notScheduledProducts);
        }

        return page.hasNext();
    }

    @MultitenantTransactional(propagation = Propagation.SUPPORTS)
    @Override
    public void restartInterruptedJobs(AcquisitionProcessingChain processingChain) throws ModuleException {
        while (!Thread.currentThread().isInterrupted()
                && productService.restartInterruptedJobsByPage(processingChain)) {
            // Works as long as there is at least one page left
        }
    }

    @MultitenantTransactional(propagation = Propagation.SUPPORTS)
    @Override
    public void retrySIPGeneration(AcquisitionProcessingChain processingChain) {
        while (!Thread.currentThread().isInterrupted() && productService.retrySIPGenerationByPage(processingChain)) {
            // Works as long as there is at least one page left
        }
    }

    @Override
    public Page<AcquisitionProcessingChainMonitor> buildAcquisitionProcessingChainSummaries(String label,
            Boolean running, AcquisitionProcessingChainMode mode, Pageable pageable) throws ModuleException {
        Page<AcquisitionProcessingChain> acqChains = acqChainRepository
                .findAll(AcquisitionProcessingChainSpecifications.search(label, running, mode), pageable);
        List<AcquisitionProcessingChainMonitor> summaries = acqChains.getContent().stream()
                .map(this::buildAcquisitionProcessingChainSummary).collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, acqChains.getTotalElements());
    }

    @Override
    public void handleProductAcquisitionError(JobInfo jobInfo) {
        Long chainId = jobInfo.getParametersAsMap().get(ProductAcquisitionJob.CHAIN_PARAMETER_ID).getValue();
        Optional<AcquisitionProcessingChain> acqChain = acqChainRepository.findById(chainId);
        if (acqChain.isPresent()) {
            for (AcquisitionFileInfo fileInfo : acqChain.get().getFileInfos()) {
                while (handleProductAcquisitionErrorByPage(fileInfo)) {
                    // Nothing to do
                }
            }
        } else {
            LOGGER.warn("Cannot handle product acquisition error because acquisition chain {} does not exist", chainId);
        }
    }

    private boolean handleProductAcquisitionErrorByPage(AcquisitionFileInfo fileInfo) {
        Page<AcquisitionFile> page = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                    PageRequest.of(0, AcquisitionProperties.WORKING_UNIT));
        for (AcquisitionFile acqFile : page) {
            acqFile.setState(AcquisitionFileState.ERROR);
        }
        acqFileRepository.saveAll(page);
        return page.hasNext();
    }

    private AcquisitionProcessingChainMonitor buildAcquisitionProcessingChainSummary(
            AcquisitionProcessingChain chainToProcess) {

        AcquisitionProcessingChain chain;
        //first lets handle all those lazy initialization issues on the chain
        try {
            chain = getChain(chainToProcess.getId());
        } catch (ModuleException e) {
            // it should not happens, as the chain has just been recovered from DB, but anyway lets log it and rethrow it
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }

        AcquisitionProcessingChainMonitor summary = new AcquisitionProcessingChainMonitor(chain);

        // Handle product summary
        summary.setNbProductErrors(productService.countByProcessingChainAndSipStateIn(chain, Arrays
                .asList(ProductSIPState.GENERATION_ERROR, ProductSIPState.NOT_SCHEDULED_INVALID, SIPState.REJECTED)));
        summary.setNbProducts(productService.countByChain(chain));
        summary.setNbProductsInProgress(productService
                .countByProcessingChainAndSipStateIn(chain,
                                                     Arrays.asList(ProductSIPState.NOT_SCHEDULED,
                                                                   ProductSIPState.SCHEDULED,
                                                                   ProductSIPState.SCHEDULED_INTERRUPTED)));

        // Handle file summary
        summary.setNbFileErrors(acqFileService
                .countByChainAndStateIn(chain,
                                        Arrays.asList(AcquisitionFileState.ERROR, AcquisitionFileState.INVALID)));
        summary.setNbFiles(acqFileService.countByChain(chain));
        summary.setNbFilesInProgress(acqFileService
                .countByChainAndStateIn(chain,
                                        Arrays.asList(AcquisitionFileState.IN_PROGRESS, AcquisitionFileState.VALID)));

        // Handle job summary
        if ((chain.getLastProductAcquisitionJobInfo() != null)
                && !chain.getLastProductAcquisitionJobInfo().getStatus().getStatus().isFinished()) {
            summary.setNbProductAcquisitionJob(1);
        }
        summary.setNbSIPGenerationJobs(productService
                .countSIPGenerationJobInfoByProcessingChainAndSipStateIn(chain, ProductSIPState.SCHEDULED));
        summary.isActive();

        return summary;
    }
}
