/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Strings;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.dao.*;
import fr.cnes.regards.modules.acquisition.domain.*;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import fr.cnes.regards.modules.acquisition.plugins.*;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.DeleteProductsJob;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.StopChainThread;
import fr.cnes.regards.modules.acquisition.service.plugins.CleanAndAcknowledgePlugin;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Acquisition processing service
 *
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AcquisitionProcessingService implements IAcquisitionProcessingService {

    private static final int BATCH_SIZE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingService.class);

    private final IAcquisitionProcessingChainRepository acqChainRepository;

    private final IAcquisitionFileRepository acqFileRepository;

    private final IAcquisitionFileInfoRepository fileInfoRepository;

    private final IScanDirectoriesInfoRepository scanDirInfoRepository;

    private final IPluginService pluginService;

    private final IProductService productService;

    private final IJobInfoService jobInfoService;

    private final IAuthenticationResolver authResolver;

    private final AutowireCapableBeanFactory beanFactory;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IAcquisitionProcessingService self;

    private final SessionNotifier sessionNotifier;

    private final AcquisitionNotificationService acquisitionNotificationService;

    public AcquisitionProcessingService(IAcquisitionProcessingChainRepository acqChainRepository,
                                        IAcquisitionFileRepository acqFileRepository,
                                        IAcquisitionFileInfoRepository fileInfoRepository,
                                        IScanDirectoriesInfoRepository scanDirInfoRepository,
                                        IPluginService pluginService,
                                        IProductService productService,
                                        IJobInfoService jobInfoService,
                                        IAuthenticationResolver authResolver,
                                        AutowireCapableBeanFactory beanFactory,
                                        IRuntimeTenantResolver runtimeTenantResolver,
                                        IAcquisitionProcessingService acquisitionProcessingService,
                                        AcquisitionNotificationService acquisitionNotificationService,
                                        SessionNotifier sessionNotifier) {
        this.acqChainRepository = acqChainRepository;
        this.acqFileRepository = acqFileRepository;
        this.fileInfoRepository = fileInfoRepository;
        this.scanDirInfoRepository = scanDirInfoRepository;
        this.pluginService = pluginService;
        this.productService = productService;
        this.jobInfoService = jobInfoService;
        this.authResolver = authResolver;
        this.beanFactory = beanFactory;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = acquisitionProcessingService;
        this.acquisitionNotificationService = acquisitionNotificationService;
        this.sessionNotifier = sessionNotifier;
    }

    @Override
    public Page<AcquisitionProcessingChain> getAllChains(Pageable pageable) {
        return acqChainRepository.findAll(pageable);
    }

    public List<AcquisitionProcessingChain> getChainsByLabel(String label) {
        return acqChainRepository.findByLabel(label);
    }

    @Override
    public AcquisitionProcessingChain getChain(Long id) throws ModuleException {
        AcquisitionProcessingChain chain = acqChainRepository.findCompleteById(id);
        if (chain == null) {
            throw new EntityNotFoundException(id, AcquisitionProcessingChain.class);
        }
        // Set metadata for all plugins
        setPluginMetadata(chain);
        return chain;
    }

    @Override
    public List<AcquisitionProcessingChain> getFullChains() {
        List<AcquisitionProcessingChain> chains = acqChainRepository.findAll();
        chains.forEach(this::setPluginMetadata);
        return chains;
    }

    @Override
    public Page<AcquisitionProcessingChain> getFullChains(Pageable pageable) {
        Page<AcquisitionProcessingChain> chainPage = acqChainRepository.findAll(pageable);
        chainPage.getContent().forEach(this::setPluginMetadata);
        return chainPage;
    }

    @Override
    public boolean isDeletionPending(AcquisitionProcessingChain chain) {
        return jobInfoService.countByClassAndParameterValueAndStatus(DeleteProductsJob.class.getName(),
                                                                     DeleteProductsJob.CHAIN_ID_PARAM,
                                                                     String.valueOf(chain.getId()),
                                                                     JobStatus.PENDING,
                                                                     JobStatus.QUEUED,
                                                                     JobStatus.RUNNING,
                                                                     JobStatus.TO_BE_RUN) > 0;
    }

    private PluginConfiguration createPluginConfiguration(PluginConfiguration pluginConfiguration)
        throws ModuleException {
        // Check no identifier. For each new chain, we force plugin configuration creation. A configuration cannot be
        // reused.
        if (pluginConfiguration.getId() != null) {
            throw new EntityInvalidException(String.format(
                "Plugin configuration %s must not already have an identifier.",
                pluginConfiguration.getLabel()));
        }
        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    @Override
    public AcquisitionProcessingChain createChain(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Check no identifier
        if (processingChain.getId() != null) {
            throw new EntityInvalidException(String.format("New chain %s must not already have an identifier.",
                                                           processingChain.getLabel()));
        }

        // Check mode
        checkProcessingChainMode(processingChain);

        // Prevent bad values
        processingChain.setLocked(Boolean.FALSE);
        processingChain.setLastActivationDate(null);

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check no identifier
            if (fileInfo.getId() != null) {
                throw new EntityInvalidException("A file information must not already have an identifier.");
            }

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

    @Override
    public AcquisitionProcessingChain updateChain(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Check already exists
        if (!acqChainRepository.existsChain(processingChain.getId())) {
            throw new EntityNotFoundException(processingChain.getLabel(), IngestProcessingChain.class);
        }

        if (isDeletionPending(processingChain)) {
            throw new EntityOperationForbiddenException("Update chain forbidden as a deletion is pending");
        }

        // Check mode
        checkProcessingChainMode(processingChain);

        List<Optional<PluginConfiguration>> confsToRemove = new ArrayList<>();
        Optional<PluginConfiguration> existingPlugin;

        // Manage acquisition file info
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {

            // Check identifier
            if (fileInfo.getId() == null) {
                // New file info to create
                // Manage scan plugin conf
                createPluginConfiguration(fileInfo.getScanPlugin());
            } else {
                // Manage scan plugin conf
                Long fileInfoId = fileInfo.getId();
                existingPlugin = fileInfoRepository.findOneScanPlugin(fileInfoId);
                confsToRemove.add(updatePluginConfiguration(Optional.of(fileInfo.getScanPlugin()), existingPlugin));
            }
            fileInfoRepository.save(fileInfo);
        }

        // Manage validation plugin conf
        existingPlugin = acqChainRepository.findOneValidationPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getValidationPluginConf()),
                                                    existingPlugin));

        // Manage product plugin conf
        existingPlugin = acqChainRepository.findOneProductPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getProductPluginConf()),
                                                    existingPlugin));

        // Manage generate SIP plugin conf
        existingPlugin = acqChainRepository.findOneGenerateSipPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(Optional.of(processingChain.getGenerateSipPluginConf()),
                                                    existingPlugin));

        // Manage post process SIP plugin conf
        existingPlugin = acqChainRepository.findOnePostProcessSipPlugin(processingChain.getId());
        confsToRemove.add(updatePluginConfiguration(processingChain.getPostProcessSipPluginConf(), existingPlugin));

        // Save new chain
        acqChainRepository.save(processingChain);

        // Clean unused plugin configuration after chain update avoiding foreign keys constraints restrictions.
        for (Optional<PluginConfiguration> confToRemove : confsToRemove) {
            if (confToRemove.isPresent()) {
                pluginService.deletePluginConfiguration(confToRemove.get().getBusinessId());
            }
        }

        return processingChain;
    }

    @Override
    public AcquisitionProcessingChain patchStateAndMode(Long chainId, UpdateAcquisitionProcessingChain payload)
        throws ModuleException {

        // Check already exists
        if (!acqChainRepository.existsChain(chainId)) {
            throw new EntityNotFoundException(chainId, IngestProcessingChain.class);
        }

        AcquisitionProcessingChain chain = getChain(chainId);

        if (isDeletionPending(chain)) {
            throw new EntityOperationForbiddenException("update chain state forbidden as a deletion is pending");
        }
        switch (payload.getUpdateType()) {
            case ALL:
                chain.setActive(payload.getActive());
                chain.setMode(payload.getMode());
                break;
            case ONLY_MODE:
                chain.setMode(payload.getMode());
                break;
            case ONLY_ACTIVITY:
                chain.setActive(payload.getActive());
        }
        checkProcessingChainMode(chain);
        chain = updateChain(chain);
        return chain;
    }

    @Override
    public List<AcquisitionProcessingChain> patchChainsStateAndMode(UpdateAcquisitionProcessingChains payload)
        throws ModuleException {
        List<AcquisitionProcessingChain> results = new ArrayList<>();
        for (Long chainId : payload.getChainIds()) {
            results.add(patchStateAndMode(chainId,
                                          UpdateAcquisitionProcessingChain.build(payload.getActive(),
                                                                                 payload.getMode(),
                                                                                 payload.getUpdateType())));
        }
        return results;
    }

    /**
     * Create or update a plugin configuration cleaning old one if necessary
     *
     * @param pluginConfiguration new plugin configuration or update
     * @param existing            existing plugin configuration
     * @return configuration to remove because it is no longer used
     * @throws ModuleException if error occurs!
     */
    private Optional<PluginConfiguration> updatePluginConfiguration(Optional<PluginConfiguration> pluginConfiguration,
                                                                    Optional<PluginConfiguration> existing)
        throws ModuleException {

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
     *
     * @param processingChain chain to check
     * @throws ModuleException if bad configuration
     */
    private void checkProcessingChainMode(AcquisitionProcessingChain processingChain) throws ModuleException {

        if (AcquisitionProcessingChainMode.AUTO.equals(processingChain.getMode()) && (processingChain.getPeriodicity()
                                                                                      == null)) {
            throw new EntityInvalidException("Missing periodicity for automatic acquisition processing chain");
        }

        if ((processingChain.getPeriodicity() != null)
            && !CronExpression.isValidExpression(processingChain.getPeriodicity())) {
            throw new EntityInvalidException("Cron expression is not valid for processing chain automatic trigger");
        }
    }

    @Override
    public void deleteChain(Long id) throws ModuleException {
        AcquisitionProcessingChain processingChain = getChain(id);

        if (processingChain.isActive()) {
            throw new EntityOperationForbiddenException(String.format(
                "Acquisition processing chain \"%s\" must be disabled to be deleted",
                processingChain.getLabel()));
        }

        productService.deleteByProcessingChain(processingChain);

        // Delete acquisition file infos and its plugin configurations
        for (AcquisitionFileInfo afi : processingChain.getFileInfos()) {
            // Before deleting file info, we have to clean up the database from invalid files that are not linked to any products
            acqFileRepository.deleteByFileInfoAndStateIn(afi,
                                                         AcquisitionFileState.IN_PROGRESS,
                                                         AcquisitionFileState.VALID,
                                                         AcquisitionFileState.INVALID);
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
    public void startAutomaticChains(OffsetDateTime lastCheckDate, OffsetDateTime currentDate) {

        // Load all automatic chains
        List<AcquisitionProcessingChain> processingChains = acqChainRepository.findAllBootableAutomaticChains();

        for (AcquisitionProcessingChain processingChain : processingChains) {
            // Check periodicity
            if (CronComparator.shouldRun(processingChain.getPeriodicity(), lastCheckDate, currentDate)) {
                if (isDeletionPending(processingChain)) {
                    LOGGER.warn("Acquisition processing chain \"{}\" won't start due to deletion pending",
                                processingChain.getLabel());
                } else if (processingChain.isLocked()) {
                    LOGGER.warn(
                        "Acquisition processing chain \"{}\" won't start because it's still locked (i.e. working)",
                        processingChain.getLabel());
                } else {
                    // Schedule job
                    scheduleProductAcquisitionJob(processingChain, Optional.empty(), false);
                }
            }
        }
    }

    @Override
    public AcquisitionProcessingChain startManualChain(Long processingChainId,
                                                       Optional<String> session,
                                                       boolean onlyErrors) throws ModuleException {

        AcquisitionProcessingChain processingChain = getChain(processingChainId);

        if (!processingChain.isActive()) {
            String message = String.format("Inactive processing chain \"%s\" cannot be started",
                                           processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        if (isDeletionPending(processingChain)) {
            throw new EntityOperationForbiddenException("Start chain forbidden as a deletion is pending");
        }

        if (Boolean.TRUE.equals(processingChain.isLocked())) {
            String message = String.format("Processing chain \"%s\" already locked", processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        if (hasExecutionBlockers(processingChain, true)) {
            String message = String.format("Acquisition chain \"%s\" has executions blockers",
                                           processingChain.getLabel());
            LOGGER.error(message);
            throw new EntityInvalidException(message);
        }

        scheduleProductAcquisitionJob(processingChain, session, onlyErrors);

        return processingChain;
    }

    @Override
    public void relaunchErrors(String chainName, String session) throws ModuleException {
        List<AcquisitionProcessingChain> chains = getChainsByLabel(chainName);
        switch (chains.size()) {
            case 0:
                throw new ModuleException("Chain %s not found.");
            case 1:
                startManualChain(chains.get(0).getId(), Optional.of(session), true);
                break;
            default:
                throw new ModuleException("Chain label %s is associated to multiple chains.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<String> getExecutionBlockers(AcquisitionProcessingChain processingChain) {
        List<String> blockers = new ArrayList<>();

        // Blockers for Scan plugins
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            blockers.addAll(computePluginBlocker(processingChain,
                                                 fileInfo.getScanPlugin().getBusinessId(),
                                                 String.format("Scan plugin for file descriptor %s",
                                                               fileInfo.getComment())));
        }

        // Blockers for Validation plugin
        blockers.addAll(computePluginBlocker(processingChain,
                                             processingChain.getValidationPluginConf().getBusinessId(),
                                             "Validation plugin"));

        // Blockers for Product plugin
        blockers.addAll(computePluginBlocker(processingChain,
                                             processingChain.getProductPluginConf().getBusinessId(),
                                             "Product plugin"));

        // Blockers for Generate Sip plugin
        blockers.addAll(computePluginBlocker(processingChain,
                                             processingChain.getGenerateSipPluginConf().getBusinessId(),
                                             "Generate Sip plugin"));

        // Blocker for Post-processing Sip Plugin if used
        if (processingChain.getPostProcessSipPluginConf().isPresent()) {
            blockers.addAll(computePluginBlocker(processingChain,
                                                 processingChain.getPostProcessSipPluginConf().get().getBusinessId(),
                                                 "Post-processing Sip plugin"));
        }

        return blockers;
    }

    /**
     * Compute potential blockers for the given plugin
     *
     * @param processingChain the processing chain
     * @param pluginId        the business id of the plugin to check
     * @param pluginName      the name of the plugin displayed in error messages and logs
     * @return the eventual blockers for the given plugin
     */
    private List<String> computePluginBlocker(AcquisitionProcessingChain processingChain,
                                              String pluginId,
                                              String pluginName) {
        List<String> blockers = new ArrayList<>();
        try {
            Object plugin = pluginService.getPlugin(pluginId);
            if (plugin instanceof IChainBlockingPlugin) {
                blockers.addAll(((IChainBlockingPlugin) plugin).getExecutionBlockers(processingChain));
            }
        } catch (PluginUtilsRuntimeException e) {
            LOGGER.error(String.format("Could not instantiate %s.", pluginName), e);
            blockers.add(String.format("Could not instantiate %s. Check logs to get more information.", pluginName));
        } catch (NotAvailablePluginConfigurationException e) {
            LOGGER.error(String.format("%s is not active.", pluginName), e);
            blockers.add(String.format("%s is not active.", pluginName));
        } catch (ModuleException e) {
            LOGGER.error("Unable to evaluate chain blocking plugins", e);
            blockers.add("Unable to evaluate chain blocking plugins");
        }
        return blockers;
    }

    @Override
    public boolean hasExecutionBlockers(AcquisitionProcessingChain chain, boolean doNotify) {

        List<String> executionBlockers = self.getExecutionBlockers(chain);
        boolean hasExecutionBlockers = !CollectionUtils.isEmpty(executionBlockers);

        if (hasExecutionBlockers && doNotify) {
            acquisitionNotificationService.notifyExecutionBlockers(chain.getLabel(), executionBlockers);
        }
        return hasExecutionBlockers;
    }

    @Override
    public boolean canBeStarted(AcquisitionProcessingChainMonitor chainMonitor) {
        AcquisitionProcessingChain chain = chainMonitor.getChain();
        return !chain.isLocked() && chain.isActive() && CollectionUtils.isEmpty(chainMonitor.getExecutionBlockers());
    }

    @Override
    public boolean canBeStarted(AcquisitionProcessingChain chain) {
        return !chain.isLocked() && chain.isActive() && !hasExecutionBlockers(chain, false);
    }

    /**
     * Schedule a product acquisition job for specified processing chain. Only one job can be scheduled.
     *
     * @param processingChain processing chain
     * @param session         user defined session name
     */
    private void scheduleProductAcquisitionJob(AcquisitionProcessingChain processingChain,
                                               Optional<String> session,
                                               boolean onlyErrors) {

        // Mark processing chain as running
        lockChain(processingChain.getId());
        processingChain.setLastActivationDate(OffsetDateTime.now());
        acqChainRepository.save(processingChain);

        // Use either the session name provided by the user either the current date
        String sessionName = session.orElseGet(() -> OffsetDateTime.now().toString());

        LOGGER.debug("Scheduling product acquisition job for processing chain \"{}\"", processingChain.getLabel());
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY);
        jobInfo.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID, processingChain.getId()),
                              new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION, sessionName),
                              new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ONLY_ERRORS, onlyErrors));
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
    @MultitenantTransactional(propagation = Propagation.NOT_SUPPORTED)
    public void scanAndRegisterFiles(AcquisitionProcessingChain processingChain, String session)
        throws ModuleException {
        // Launch file scanning for each file information
        Iterator<AcquisitionFileInfo> fileInfoIter = processingChain.getFileInfos().iterator();
        while (fileInfoIter.hasNext() && !Thread.currentThread().isInterrupted()) {
            AcquisitionFileInfo fileInfo = fileInfoIter.next();
            // Get plugin instance
            IScanPlugin scanPlugin;
            try {
                scanPlugin = pluginService.getPlugin(fileInfo.getScanPlugin().getBusinessId());
            } catch (NotAvailablePluginConfigurationException e1) {
                LOGGER.error("Unable to run files scan as plugin is disabled");
                throw new ModuleException(e1.getMessage(), e1);
            }
            String fileExtensionToNotRegister = getAckExtensionIfExists(processingChain);
            // Get files to scan
            Set<ScanDirectoryInfo> scanDirs = fileInfo.getScanDirInfo();
            for (ScanDirectoryInfo scanDirInfo : scanDirs) {
                // Clone scanning date for duplicate prevention
                Optional<OffsetDateTime> scanningDate = Optional.empty();
                if (scanDirInfo.getLastModificationDate() != null) {
                    scanningDate = Optional.of(OffsetDateTime.ofInstant((scanDirInfo.getLastModificationDate()).toInstant(),
                                                                        ZoneOffset.UTC));
                }
                // Scan folders
                if (scanPlugin instanceof IFluxScanPlugin) {
                    streamAndRegisterFiles(fileInfo,
                                           scanDirInfo,
                                           (IFluxScanPlugin) scanPlugin,
                                           scanningDate,
                                           session,
                                           processingChain.getLabel(),
                                           fileExtensionToNotRegister);
                } else {
                    scanAndRegisterFiles(fileInfo,
                                         scanDirInfo,
                                         scanPlugin,
                                         scanningDate,
                                         session,
                                         processingChain.getLabel(),
                                         fileExtensionToNotRegister);
                }
            }
        }
    }

    /**
     * Return ack extension, if postProcess plugin is set to {@link CleanAndAcknowledgePlugin},
     * and if createAck param is set to true
     * </br>
     * This is necessary during the scan operation, to remove ack files created at the previous acquisitions
     *
     * @see <a href="https://odin.si.c-s.fr/plugins/tracker/?aid=295088">odin FA</a>
     */
    private String getAckExtensionIfExists(AcquisitionProcessingChain acqProcessingChain) {
        Optional<PluginConfiguration> postProcessSipPluginConfOpt = acqProcessingChain.getPostProcessSipPluginConf();
        if (postProcessSipPluginConfOpt.isPresent()) {
            try {
                ISipPostProcessingPlugin postProcessPlugin = pluginService.getPlugin(postProcessSipPluginConfOpt.get()
                                                                                                                .getBusinessId());
                Optional<String> extensionOpt = postProcessPlugin.getFileExtensionToExcludeInScanStep();
                if (extensionOpt.isPresent()) {
                    String extension = extensionOpt.get();
                    // removes "." if indicated
                    if (extension.startsWith(".")) {
                        extension = extension.substring(1);
                    }
                    return extension;
                }
            } catch (ModuleException e) {
                LOGGER.warn("Unable to get postprocess plugin : " + e.getMessage(), e);
            }
        }
        return null;
    }

    private void scanAndRegisterFiles(AcquisitionFileInfo fileInfo,
                                      ScanDirectoryInfo scanDirInfo,
                                      IScanPlugin scanPlugin,
                                      Optional<OffsetDateTime> scanningDate,
                                      String session,
                                      String sessionOwner,
                                      String fileExtensionToFilter) throws ModuleException {
        // Do scan
        List<Path> scannedFiles = scanPlugin.scan(scanDirInfo.getScannedDirectory(), scanningDate);

        // Sort list according to last modification date
        scannedFiles.sort((file1, file2) -> {
            try {
                return Files.getLastModifiedTime(file1).compareTo(Files.getLastModifiedTime(file2));
            } catch (IOException e) {
                LOGGER.warn("Cannot read last modification date", e);
                return 0;
            }
        });
        if (!scannedFiles.isEmpty()) {
            registerFiles(scannedFiles.iterator(),
                          fileInfo,
                          scanDirInfo,
                          scanningDate,
                          session,
                          sessionOwner,
                          fileExtensionToFilter);
        }
        if (scanningDate.isPresent()) {
            LOGGER.info("[{} - {}] Scan for files <{}> found {} files with last update date > {} ",
                        sessionOwner,
                        session,
                        fileInfo.getComment(),
                        scannedFiles.size(),
                        scanningDate.get());
        } else {
            LOGGER.info("[{} - {}] Scan for files <{}> found {} files with no date filter.",
                        sessionOwner,
                        session,
                        fileInfo.getComment(),
                        scannedFiles.size());
        }
    }

    private void streamAndRegisterFiles(AcquisitionFileInfo fileInfo,
                                        ScanDirectoryInfo scanDirInfo,
                                        IFluxScanPlugin scanPlugin,
                                        Optional<OffsetDateTime> scanningDate,
                                        String session,
                                        String sessionOwner,
                                        String fileExtensionToNotRegister) throws ModuleException {
        List<Stream<Path>> streams = scanPlugin.stream(scanDirInfo.getScannedDirectory(), scanningDate);
        Iterator<Stream<Path>> streamsIt = streams.iterator();
        while (streamsIt.hasNext() && !Thread.currentThread().isInterrupted()) {
            try (Stream<Path> stream = streamsIt.next()) {
                registerFiles(stream.iterator(),
                              fileInfo,
                              scanDirInfo,
                              scanningDate,
                              session,
                              sessionOwner,
                              fileExtensionToNotRegister);
            }
        }
    }

    @Override
    public long registerFiles(Iterator<Path> filePathsIt,
                              AcquisitionFileInfo fileInfo,
                              ScanDirectoryInfo scanDir,
                              Optional<OffsetDateTime> scanningDate,
                              String session,
                              String sessionOwner,
                              String fileExtensionToNotRegister) throws ModuleException {
        RegisterFilesResponse response;
        long totalCount = 0;
        OffsetDateTime lmd = null;
        long nbFilesAcquired;
        do {
            long startTime = System.currentTimeMillis();
            response = self.registerFilesBatch(filePathsIt,
                                               fileInfo,
                                               scanningDate,
                                               BATCH_SIZE,
                                               session,
                                               sessionOwner,
                                               fileExtensionToNotRegister);
            nbFilesAcquired = response.getNumberOfRegisteredFiles();
            // Calculate most recent file registered.
            if ((lmd == null) || (lmd.isBefore(response.getLastUpdateDate()) && !Thread.currentThread()
                                                                                       .isInterrupted())) {
                lmd = response.getLastUpdateDate();
            }
            // notify only if files were acquired
            if (nbFilesAcquired > 0) {
                totalCount += nbFilesAcquired;
                sessionNotifier.notifyFileAcquired(session, sessionOwner, nbFilesAcquired);
                LOGGER.info("{} new file(s) registered in {} milliseconds",
                            nbFilesAcquired,
                            System.currentTimeMillis() - startTime);
            }
        } while (response.hasNext() && !Thread.currentThread().isInterrupted());
        // Update scanDirInfo last update date with the most recent file registered.
        if ((lmd != null) && ((scanDir.getLastModificationDate() == null)
                              || lmd.isAfter(scanDir.getLastModificationDate()))) {
            scanDir.setLastModificationDate(lmd);
            scanDirInfoRepository.save(scanDir);
        }
        return totalCount;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public RegisterFilesResponse registerFilesBatch(Iterator<Path> filePaths,
                                                    AcquisitionFileInfo info,
                                                    Optional<OffsetDateTime> scanningDate,
                                                    int limit,
                                                    String session,
                                                    String sessionOwner,
                                                    String fileExtensionToNotRegister) throws ModuleException {
        int countRegistered = 0;
        OffsetDateTime lastUpdateDate = null;
        // We catch general exception to avoid AccessDeniedException thrown by FileTreeIterator provided to this method.
        boolean nextPath = true;
        // First calculate
        while (nextPath && (countRegistered < limit) && !Thread.currentThread().isInterrupted()) {
            try {
                Path filePath = filePaths.next();
                if (needToRegisterFile(filePath, fileExtensionToNotRegister)) {
                    if (registerFile(filePath, info, scanningDate)) {
                        countRegistered++;
                        lastUpdateDate = getLastUpdateDate(filePath, lastUpdateDate);
                    }
                }
            } catch (Exception e) { // NOSONAR
                LOGGER.error(String.format("Error parsing file. %s", e.getMessage()), e);
            } finally {
                nextPath = filePaths.hasNext();
            }
        }
        return RegisterFilesResponse.build(countRegistered, lastUpdateDate, filePaths.hasNext());
    }

    private boolean needToRegisterFile(Path filePath, String fileExtensionToNotRegister) {
        return !FilenameUtils.getExtension(filePath.getFileName().toString()).equals(fileExtensionToNotRegister);
    }

    /**
     * Calculate last file update date
     *
     * @param filePath       file  to check
     * @param lastUpdateDate current last update date
     * @return filePath last update date if after given current lastUpdateDate
     */
    private OffsetDateTime getLastUpdateDate(Path filePath, OffsetDateTime lastUpdateDate) {
        OffsetDateTime result = lastUpdateDate;
        try {
            OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(filePath).toInstant(),
                                                          ZoneOffset.UTC);
            if ((lastUpdateDate == null) || lmd.isAfter(lastUpdateDate)) {
                result = lmd;
            }
        } catch (IOException e) {
            LOGGER.error("Error getting last update date for file {} cause : {}.", filePath, e.getMessage());
        }
        return result;
    }

    @Override
    public boolean registerFile(Path filePath, AcquisitionFileInfo info, Optional<OffsetDateTime> scanningDate) {
        OffsetDateTime lmd;
        try {
            // If new file to register date <= last scanning date, check if file is not already acquired.
            // truncate to microseconds because postgres timestamp has a resolution of 1 microsecond
            lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(filePath).toInstant(), ZoneOffset.UTC)
                                .truncatedTo(ChronoUnit.MICROS);
            if (scanningDate.isPresent()
                && (lmd.isBefore(scanningDate.get()) || lmd.isEqual(scanningDate.get()))
                && acqFileRepository.existsByFilePathInAndFileInfo(filePath, info)) {
                return false;
            } else {
                // Initialize new file
                AcquisitionFile scannedFile = new AcquisitionFile();
                scannedFile.setAcqDate(OffsetDateTime.now());
                scannedFile.setFileInfo(info);
                scannedFile.setFilePath(filePath);
                scannedFile.setState(AcquisitionFileState.IN_PROGRESS);
                acqFileRepository.save(scannedFile);
                return true;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public long manageRegisteredFiles(AcquisitionProcessingChain processingChain, String session)
        throws ModuleException {
        long nbProductsScheduled = 0L;
        boolean stop = false;
        while (!Thread.currentThread().isInterrupted() && !stop) {
            ProductsPage productsPage = self.manageRegisteredFilesByPage(processingChain, session);
            // Works as long as there is at least one page left
            nbProductsScheduled += productsPage.getScheduled();
            stop = !productsPage.hasNext();
        }
        // Just trace interruption
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
        return nbProductsScheduled;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public ProductsPage manageRegisteredFilesByPage(AcquisitionProcessingChain processingChain, String session)
        throws ModuleException {

        // Retrieve first page of new registered files
        Page<AcquisitionFile> page = acqFileRepository.findByStateAndFileInfoInOrderByAcqDateAsc(AcquisitionFileState.IN_PROGRESS,
                                                                                                 processingChain.getFileInfos(),
                                                                                                 PageRequest.of(0,
                                                                                                                productService.getBulkAcquisitionLimit()));
        LOGGER.debug("Managing next new {} registered files (of {})",
                     page.getNumberOfElements(),
                     page.getTotalElements());
        long startTime = System.currentTimeMillis();

        // Get validation plugin
        IValidationPlugin validationPlugin;
        try {
            validationPlugin = pluginService.getPlugin(processingChain.getValidationPluginConf().getBusinessId());
        } catch (NotAvailablePluginConfigurationException e1) {
            throw new ModuleException("Unable to run disabled acquisition chain.", e1);
        }
        List<AcquisitionFile> validFiles = new ArrayList<>();
        List<AcquisitionFile> invalidFiles = new ArrayList<>();
        for (AcquisitionFile inProgressFile : page.getContent()) {
            // Validate files
            try {
                if (validationPlugin.validate(inProgressFile.getFilePath())) {
                    inProgressFile.setState(AcquisitionFileState.VALID);
                    validFiles.add(inProgressFile);
                } else {
                    String errorMessage = "File not valid according to plugin " + validationPlugin.getClass()
                                                                                                  .getSimpleName();
                    LOGGER.error(errorMessage);
                    inProgressFile.setErrorMsgWithState(errorMessage, AcquisitionFileState.INVALID);
                    invalidFiles.add(acqFileRepository.save(inProgressFile));
                    sessionNotifier.notifyFileInvalid(session, processingChain.getLabel(), 1);
                }
            } catch (ModuleException e) {
                LOGGER.error(e.getMessage(), e);
                inProgressFile.setErrorMsgWithState(String.format("File not valid according to plugin %s. Cause : %s",
                                                                  validationPlugin.getClass().getSimpleName(),
                                                                  e.getMessage()), AcquisitionFileState.INVALID);
                invalidFiles.add(acqFileRepository.save(inProgressFile));
                sessionNotifier.notifyFileInvalid(session, processingChain.getLabel(), 1);
            }
        }

        // Send Notification for invalid files
        if (!invalidFiles.isEmpty()) {
            acquisitionNotificationService.notifyInvalidAcquisitionFile(invalidFiles);
        }

        LOGGER.debug("Validation of {} file(s) finished with {} valid and {} invalid.",
                     page.getNumberOfElements(),
                     validFiles.size(),
                     page.getNumberOfElements() - validFiles.size());

        // Build and schedule products, for a subset of the current file page
        Set<Product> products = productService.linkAcquisitionFilesToProducts(processingChain, session, validFiles);
        LOGGER.debug("{} file(s) handles, {} product(s) created or updated in {} milliseconds",
                     page.getNumberOfElements(),
                     products.size(),
                     System.currentTimeMillis() - startTime);

        int scheduledProducts = 0;
        int notScheduledProducts = 0;

        // Statistics
        for (Product product : products) {
            if (product.getSipState() == ProductSIPState.SCHEDULED) {
                scheduledProducts++;
            } else {
                notScheduledProducts++;
            }
        }
        LOGGER.debug("{} product(s) scheduled and {} not.", scheduledProducts, notScheduledProducts);
        return ProductsPage.build(page.hasNext(), scheduledProducts, notScheduledProducts);
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
    public void retrySIPGeneration(AcquisitionProcessingChain processingChain, Optional<String> sessionToRetry) {
        while (!Thread.currentThread().isInterrupted() && productService.retrySIPGenerationByPage(processingChain,
                                                                                                  sessionToRetry)) {
            // Works as long as there is at least one page left
        }
    }

    @Override
    public Page<AcquisitionProcessingChainMonitor> buildAcquisitionProcessingChainSummaries(String label,
                                                                                            Boolean runnable,
                                                                                            AcquisitionProcessingChainMode mode,
                                                                                            Pageable pageable) {
        Page<AcquisitionProcessingChain> acqChains = acqChainRepository.findAll(AcquisitionProcessingChainSpecifications.search(
            label,
            runnable,
            mode), pageable);
        List<AcquisitionProcessingChainMonitor> monitors = acqChains.getContent()
                                                                    .stream()
                                                                    .map(this::buildAcquisitionProcessingChainMonitor)
                                                                    .collect(Collectors.toList());
        return new PageImpl<>(monitors, pageable, acqChains.getTotalElements());
    }

    @Override
    public void handleProductAcquisitionAborted(JobInfo jobInfo) {
        Long chainId = jobInfo.getParametersAsMap().get(ProductAcquisitionJob.CHAIN_PARAMETER_ID).getValue();
        acqChainRepository.findById(chainId).ifPresent(chain -> {
            unlockChain(chainId);
        });
    }

    @Override
    public void handleProductAcquisitionError(JobInfo jobInfo) {
        Long chainId = jobInfo.getParametersAsMap().get(ProductAcquisitionJob.CHAIN_PARAMETER_ID).getValue();
        Optional<AcquisitionProcessingChain> acqChain = acqChainRepository.findById(chainId);
        String errorMessage = Strings.isNullOrEmpty(jobInfo.getStatus().getStackTrace()) ?
            "There was an unexpected error during the corresponding job execution. Not even the job is tracing what was the issue." :
            String.format(
                "There was an error during job execution. Please refer to the job(id:%s) error for more information",
                jobInfo.getId().toString());
        if (acqChain.isPresent()) {
            for (AcquisitionFileInfo fileInfo : acqChain.get().getFileInfos()) {
                while (handleProductAcquisitionErrorByPage(fileInfo, errorMessage)) {
                    // Nothing to do
                }
            }
            unlockChain(acqChain.get().getId());
        } else {
            LOGGER.warn("Cannot handle product acquisition error because acquisition chain {} does not exist", chainId);
        }
    }

    private boolean handleProductAcquisitionErrorByPage(AcquisitionFileInfo fileInfo, String errorMessage) {
        Page<AcquisitionFile> page = acqFileRepository.findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS,
                                                                                          fileInfo,
                                                                                          PageRequest.of(0,
                                                                                                         productService.getBulkAcquisitionLimit()));
        for (AcquisitionFile acqFile : page) {
            // set error message in case there was none (more specific)
            if (Strings.isNullOrEmpty(acqFile.getError())) {
                acqFile.setError(errorMessage);
            }
            acqFile.setState(AcquisitionFileState.ERROR);
        }
        acqFileRepository.saveAll(page);
        return page.hasNext();
    }

    private AcquisitionProcessingChainMonitor buildAcquisitionProcessingChainMonitor(AcquisitionProcessingChain chain) {
        AcquisitionProcessingChainMonitor monitor = new AcquisitionProcessingChainMonitor(chain,
                                                                                          isDeletionPending(chain));
        boolean isProductAcquisitionJobActive = chain.getLastProductAcquisitionJobInfo() != null
                                                && !chain.getLastProductAcquisitionJobInfo()
                                                         .getStatus()
                                                         .getStatus()
                                                         .isFinished();
        monitor.setActive(isProductAcquisitionJobActive,
                          productService.countSIPGenerationJobInfoByProcessingChainAndSipStateIn(chain,
                                                                                                 ProductSIPState.SCHEDULED));
        monitor.getExecutionBlockers().addAll(self.getExecutionBlockers(chain));
        return monitor;
    }

    @Override
    public void scheduleProductDeletion(String processingChainLabel, Optional<String> session, boolean deleteChain)
        throws ModuleException {
        List<AcquisitionProcessingChain> chains = getChainsByLabel(processingChainLabel);
        for (AcquisitionProcessingChain chain : chains) {
            if (deleteChain && (chain.isLocked() || chain.isActive())) {
                throw new EntityOperationForbiddenException(
                    "Acquisition chain is locked or running. Deletion is not available right now.");
            } else {
                productService.scheduleProductsDeletionJob(chain, session, deleteChain);
            }
        }
    }

    @Override
    public void scheduleProductDeletion(Long processingChainId, Optional<String> session, boolean deleteChain)
        throws ModuleException {
        AcquisitionProcessingChain chain = getChain(processingChainId);
        if (deleteChain && (chain.isLocked() || chain.isActive())) {
            throw new EntityOperationForbiddenException(
                "Acquisition chain is locked or running. Deletion is not available right now.");
        }
        productService.scheduleProductsDeletionJob(chain, session, deleteChain);
    }

    @Override
    public List<AcquisitionProcessingChain> findAllBootableAutomaticChains() {
        return acqChainRepository.findAllBootableAutomaticChains();
    }

    @Override
    public List<AcquisitionProcessingChain> findByModeAndActiveTrueAndLockedFalse(AcquisitionProcessingChainMode manual) {
        return acqChainRepository.findByModeAndActiveTrueAndLockedFalse(manual);
    }

    private void setPluginMetadata(AcquisitionProcessingChain chain) {
        for (AcquisitionFileInfo fileInfo : chain.getFileInfos()) {
            pluginService.setMetadata(fileInfo.getScanPlugin());
        }
        pluginService.setMetadata(chain.getValidationPluginConf());
        pluginService.setMetadata(chain.getProductPluginConf());
        pluginService.setMetadata(chain.getGenerateSipPluginConf());
        chain.getPostProcessSipPluginConf()
             .ifPresent(pluginConfiguration -> pluginService.setMetadata(pluginConfiguration));
    }

}
