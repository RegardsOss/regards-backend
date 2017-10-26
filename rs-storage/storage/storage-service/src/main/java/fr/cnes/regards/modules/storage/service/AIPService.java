/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.FileCorruptedException;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.datastorage.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IWorkingSubset;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage;
import fr.cnes.regards.modules.storage.plugin.security.CatalogSecurityDelegation;
import fr.cnes.regards.modules.storage.plugin.security.ISecurityDelegation;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.DeleteDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;

/**
 * Service to handle {@link AIP} and associated {@link DataFile}s entities from all data straoge systems.<br/>
 * An {@link AIP} can be associated to many {@link DataFile}s but only one of type {@link DataType#AIP}.<br/>
 * Available data storage systems are defined by the available {@link IDataStorage} plugins<br/>
 * Stored files can be stored with :
 * <ul>
 * <li>Online data storage plugins {@link IOnlineDataStorage} : Files are directly accessible for download</li>
 * <li>Nearline data storage plugins {@link INearlineDataStorage} : Files needs to be cached before download</li>
 * </ul>
 *
 * At startup, this service subscribe to all {@link DataStorageEvent}s to handle physical actions
 * (store, retrieve and deletion) on {@link DataFile}s.<br/>
 * See {@link DataStorageEventHandler} class to understand more about actions done on physical files changes.<br/>
 * <br/>
 * This service also run scheduled actions :
 * <ul>
 * <li>{@link #storeMetadata} : This cron action executed every minutes handle
 * update of {@link AIP} state by looking for all associated {@link DataFile} states.
 * An {@link AIP} is STORED when all his {@link DataFile}s are STORED</li>
 * </ul>
 * <br/>
 * The cache system to make nearline files accessible is handled by the {@link ICachedFileService}.<br/>
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Service
@RegardsTransactional
public class AIPService implements IAIPService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPService.class);

    private static final String AIP_ACCESS_FORBIDDEN = "You do not have suffisent access right to get this aip.";

    /**
     * DAO to access {@link AIP} entities through the {@link AIPDataBase} entities stored in db.
     */
    @Autowired
    private IAIPDao aipDao;

    /**
     * DAO to access {@link DataFile} entities.
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * AMQP Publisher.
     */
    @Autowired
    private IPublisher publisher;

    /**
     * AMQP Subscriber.
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Service to retrieve and use Plugins more specificly the {@link IDataStorage} plugins.
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * The AIP service uses JOBS to run asynchronous store actions.
     */
    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * Resolver to know all existing tenants of the current REGARDS instance.
     */
    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private Gson gson;

    /**
     * to get transactionnality inside scheduled
     */
    @Autowired
    private IAIPService self;

    @Value("${regards.storage.workspace}")
    private String workspace;

    /**
     * Service to manage avaibility of nearline files.
     */
    @Autowired
    private ICachedFileService cachedFileService;

    /**
     * Handler to manage {@link DataStorageEvent} events.
     */
    @Autowired
    private DataStorageEventHandler dataStorageEventHandler;

    /**
     * JSON files extension.
     */
    public static final String JSON_FILE_EXT = ".json";

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Subscribe to events on {@link DataFile} changes.
        subscriber.subscribeTo(DataStorageEvent.class, dataStorageEventHandler, WorkerMode.SINGLE, Target.MICROSERVICE);

        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(INearlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(ISecurityDelegation.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(STAFDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(CatalogSecurityDelegation.class.getPackage().getName());
    }

    @Override
    public Set<UUID> store(Set<AIP> aips) throws ModuleException {
        LOG.trace("Entering method store(Set<AIP>) with {} aips", aips.size());
        Set<AIP> aipsInDb = Sets.newHashSet();
        Set<DataFile> dataFilesToStore = Sets.newHashSet();
        // 1. Create each AIP into database with VALID state.
        // 2. Create each DataFile of each AIP into database with PENDING state.
        for (AIP aip : aips) {
            // Can not store an existing AIP.
            if (aipDao.findOneByIpId(aip.getId().toString()).isPresent()) {
                throw new EntityAlreadyExistsException(
                        String.format("AIP with ip id %s already exists", aip.getId().toString()));
            }
            aip.setState(AIPState.VALID);
            aip.addEvent(EventType.SUBMISSION.name(), "Submission to REGARDS");
            aipsInDb.add(aipDao.save(aip));
            Collection<DataFile> dataFiles = dataFileDao.save(DataFile.extractDataFiles(aip));
            dataFiles.forEach(df -> df.setState(DataFileState.PENDING));
            dataFilesToStore.addAll(dataFiles);
            // Notify system for new VALID AIP created.
            publisher.publish(new AIPEvent(aip));
        }
        LOG.trace("{} aips built {} data objects to store", aips.size(), dataFilesToStore.size());
        IAllocationStrategy allocationStrategy = getAllocationStrategy();
        // FIXME: should probably set the tenant into maintenance in case of module exception

        // 3. Now lets ask to the strategy to dispatch dataFiles between possible DataStorages
        Multimap<PluginConfiguration, DataFile> storageWorkingSetMap = allocationStrategy.dispatch(dataFilesToStore);
        LOG.trace("{} data objects has been dispatched between {} data storage by allocation strategy",
                  dataFilesToStore.size(), storageWorkingSetMap.keySet().size());
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        checkDispatch(dataFilesToStore, storageWorkingSetMap);
        Set<UUID> jobIds = scheduleStorage(storageWorkingSetMap, true);
        // change the state to PENDING
        for (AIP aip : aipsInDb) {
            aip.setState(AIPState.PENDING);
            aipDao.save(aip);
            // Notify system for AIP updated to PENDING state.
            publisher.publish(new AIPEvent(aip));
        }
        return jobIds;
    }

    @Override
    public AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest) throws ModuleException {
        Set<String> requestedChecksums = availabilityRequest.getChecksums();
        Set<DataFile> dataFiles = dataFileDao.findAllByChecksumIn(requestedChecksums);
        Set<String> errors = Sets.newHashSet();

        // 1. Check for invalid files.
        if (dataFiles.size() != requestedChecksums.size()) {
            Set<String> dataFilesChecksums = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
            Set<String> checksumNotFound = Sets.difference(requestedChecksums, dataFilesChecksums);
            errors.addAll(checksumNotFound);
            checksumNotFound.stream()
                    .forEach(cs -> LOG.error("File to restore with checksum {} is not stored by REGARDS.", cs));
        }

        Set<DataFile> dataFilesWithAccess = checkLoadFilesAccessRights(dataFiles);

        errors.addAll(Sets.difference(dataFiles, dataFilesWithAccess).stream().map(df -> df.getChecksum())
                .collect(Collectors.toSet()));

        Set<DataFile> onlineFiles = Sets.newHashSet();
        Set<DataFile> nearlineFiles = Sets.newHashSet();

        // 2. Check for online files. Online files doesn't need to be stored in the cache
        // they can be access directly where they are stored.
        for (DataFile df : dataFilesWithAccess) {
            if (df.getDataStorageUsed() != null) {
                if (df.getDataStorageUsed().getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
                    onlineFiles.add(df);
                } else {
                    nearlineFiles.add(df);
                }
            } else {
                LOG.error("File to restore {} has no storage plugin information. Restoration failed.", df.getId());
            }
        }
        // now lets ask the cache service to handle nearline restoration and give us the already available ones
        CoupleAvailableError nearlineAvailableAndError = cachedFileService
                .restore(nearlineFiles, availabilityRequest.getExpirationDate());
        for (DataFile inError : nearlineAvailableAndError.getErrors()) {
            errors.add(inError.getChecksum());
        }
        // lets constrcut the result
        return new AvailabilityResponse(errors, onlineFiles, nearlineAvailableAndError.getAvailables());
    }

    private Set<DataFile> checkLoadFilesAccessRights(Set<DataFile> dataFiles) throws ModuleException {
        Set<DataFile> dataFilesWithAccess = Sets.newHashSet(dataFiles);
        for (DataFile df : dataFiles) {
            AIP aip = df.getAip();
            if (!getSecurityDelegationPlugin().hasAccess(aip.getId().toString())) {
                dataFilesWithAccess.remove(df);
            }
        }
        return dataFilesWithAccess;
    }

    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, Pageable pPageable)
            throws ModuleException { // NOSONAR
        if (!getSecurityDelegationPlugin().hasAccessToListFeature()) {
            throw new EntityOperationForbiddenException("Only Admins can access this feature.");
        }
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return aipDao
                            .findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                        pTo.plusSeconds(1), pPageable);
                }
                return aipDao.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return aipDao.findAllByStateAndLastEventDateBefore(pState, pTo.plusSeconds(1), pPageable);
            }
            return aipDao.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return aipDao.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1),
                                                                                 pTo.plusSeconds(1), pPageable);
            }
            return aipDao.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return aipDao.findAllByLastEventDateBefore(pTo.plusSeconds(1), pPageable);
        }
        return aipDao.findAll(pPageable);
    }

    @Override
    public Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByIpId(pIpId.toString());
        if (aip.isPresent()) {
            if (!getSecurityDelegationPlugin().hasAccess(pIpId.toString())) {
                throw new EntityOperationForbiddenException(pIpId.toString(), AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            Set<DataFile> dataFiles = dataFileDao.findAllByAip(aip.get());
            return dataFiles.stream().map(df -> {
                OAISDataObject dataObject = new OAISDataObject();
                dataObject.setRegardsDataType(df.getDataType());
                dataObject.setUrl(df.getUrl());
                dataObject.setFilename(df.getName());
                dataObject.setFileSize(df.getFileSize());
                dataObject.setChecksum(df.getChecksum());
                dataObject.setAlgorithm(df.getAlgorithm());
                return dataObject;
            }).collect(Collectors.toSet());
        } else {
            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        }
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> versions = aipDao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getId().toString()).collect(Collectors.toList());
    }

    /**
     * Check that all given {@link DataFile}s are dispatch into the given {@link Multimap}.<br/>
     * If it's true, nothing is done.<br/>
     * If not, the associated {@link AIP}s of given {@link DataFile}s are set to {@link AIPState#STORAGE_ERROR} status.
     * @param dataFilesToStore {@link DataFile}s
     * @param storageWorkingSetMap {@link Multimap}<{@link PluginConfiguration}, {@link DataFile}>
     */
    private void checkDispatch(Set<DataFile> dataFilesToStore,
            Multimap<PluginConfiguration, DataFile> storageWorkingSetMap) {
        Set<DataFile> dataFilesInSubSet = storageWorkingSetMap.entries().stream().map(entry -> entry.getValue())
                .collect(Collectors.toSet());
        if (dataFilesToStore.size() != dataFilesInSubSet.size()) {
            Set<DataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToStore);
            notSubSetDataFiles.removeAll(dataFilesInSubSet);
            for (DataFile prepareFailed : notSubSetDataFiles) {
                prepareFailed.setState(DataFileState.ERROR);
                AIP aip = prepareFailed.getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
                // TODO: notify
            }
        }
    }

    /**
     * This method scheduls {@link StoreDataFilesJob} or {@link StoreMetadataFilesJob} to store given
     * {@link DataFile}s.<br/>
     * A Job is scheduled for each {@link IWorkingSubset} of each {@link PluginConfiguration}.<br/>
     * @param storageWorkingSetMap List of {@link DataFile} to store per {@link PluginConfiguration}.
     * @param storingData FALSE to store {@link DataType#AIP}, or TRUE for all other type of {@link DataFile}.
     * @return List of {@link UUID} of jobs scheduled.
     * @throws ModuleException
     */
    public Set<UUID> scheduleStorage(Multimap<PluginConfiguration, DataFile> storageWorkingSetMap, boolean storingData)
            throws ModuleException {
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : storageWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(storageWorkingSetMap.get(dataStorageConf),
                                                                   dataStorageConf);
            LOG.trace("Preparing a job for each working subsets");
            // lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets store a job for
                // each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                if (storingData) {
                    jobsToSchedule
                            .add(new JobInfo(0, parameters, authResolver.getUser(), StoreDataFilesJob.class.getName()));
                } else {
                    jobsToSchedule.add(new JobInfo(0, parameters, authResolver.getUser(),
                            StoreMetadataFilesJob.class.getName()));
                }

            }
        }
        Set<UUID> jobIds = Sets.newHashSet();
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.createAsQueued(job).getId());
        }
        return jobIds;
    }

    /**
     * Call the {@link IDataStorage} plugins associated to the given {@link PluginConfiguration}s to store
     * {@link IWorkingSubset}
     * of {@link DataFile}s.
     * @param dataFilesToSubSet List of {@link DataFile} to prepare.
     * @param dataStorageConf {@link PluginConfiguration}
     * @return {@link IWorkingSubset}s
     * @throws ModuleException
     */
    protected Set<IWorkingSubset> getWorkingSubsets(Collection<DataFile> dataFilesToSubSet,
            PluginConfiguration dataStorageConf) throws ModuleException {
        LOG.trace("Getting working subsets for data storage {}", dataStorageConf.getLabel());
        IDataStorage<IWorkingSubset> storage = pluginService.getPlugin(dataStorageConf);
        Set<IWorkingSubset> workingSubSets = storage.prepare(dataFilesToSubSet, DataStorageAccessModeEnum.STORE_MODE);
        LOG.trace("{} data objects were dispatched into {} working subsets", dataFilesToSubSet.size(),
                  workingSubSets.size());
        // as we are trusty people, we check that the prepare gave us back all DataFiles into the WorkingSubSets
        Set<DataFile> subSetDataFiles = workingSubSets.stream().flatMap(wss -> wss.getDataFiles().stream())
                .collect(Collectors.toSet());
        if (subSetDataFiles.size() != dataFilesToSubSet.size()) {
            Set<DataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToSubSet);
            notSubSetDataFiles.removeAll(subSetDataFiles);
            for (DataFile prepareFailed : notSubSetDataFiles) {
                prepareFailed.setState(DataFileState.ERROR);
                AIP aip = prepareFailed.getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
                // TODO: notify
            }
        }
        return workingSubSets;
    }

    /**
     * Retrieve the only one activated allocation strategy {@link IAllocationStrategy} plugin.
     * @return {@link IAllocationStrategy}
     * @throws ModuleException if many {@link IAllocationStrategy} are active.
     */
    private IAllocationStrategy getAllocationStrategy() throws ModuleException {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> allocationStrategies = pluginService
                .getPluginConfigurationsByType(IAllocationStrategy.class);
        List<PluginConfiguration> activeAllocationStrategies = allocationStrategies.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeAllocationStrategies.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of "
                            + IAllocationStrategy.class.getName());
            LOG.error(e.getMessage(), e);
            throw e;
        }
        return pluginService.getPlugin(activeAllocationStrategies.get(0));
    }

    private ISecurityDelegation getSecurityDelegationPlugin() throws ModuleException {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> securityDelegations = pluginService
                .getPluginConfigurationsByType(ISecurityDelegation.class);
        List<PluginConfiguration> activeSecurityDelegations = securityDelegations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeSecurityDelegations.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of "
                            + ISecurityDelegation.class.getName());
            LOG.error(e.getMessage(), e);
            throw e;
        }
        return pluginService.getPlugin(activeSecurityDelegations.get(0));
    }

    @Override
    public void scheduleStorageMetadata(Set<DataFile> metadataToStore) {
        try {
            IAllocationStrategy allocationStrategy = getAllocationStrategy();

            // we need to listen to those jobs event to clean up the workspace
            Multimap<PluginConfiguration, DataFile> storageWorkingSetMap = allocationStrategy.dispatch(metadataToStore);
            checkDispatch(metadataToStore, storageWorkingSetMap);
            Set<UUID> jobsToMonitor = scheduleStorage(storageWorkingSetMap, false);
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to
            // STORING_METADATA
            for (DataFile dataFile : metadataToStore) {
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORING_METADATA);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
            // TODO: notify, probably should set the system into maintenance mode...
        }
    }

    /**
     * This cron action executed every minutes handle update of {@link AIP} state by
     * looking for all associated {@link DataFile} states. An {@link AIP} is STORED
     * when all his {@link DataFile}s are STORED.
     */
    @Scheduled(fixedDelayString = "${regards.storage.check.aip.metadata.delay:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void storeMetadata() {
        LOG.debug(" ------------------------> Update AIP storage informations - START<---------------------------- ");
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Path tenantWorkspace = Paths.get(workspace, tenant);
            if (!Files.exists(tenantWorkspace)) {
                try {
                    Files.createDirectories(tenantWorkspace);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    // TODO: notify, probably should set the system into maintenance mode...
                }
            }
            Set<DataFile> metadataToStore = Sets.newHashSet();
            // first lets get AIP that are not fully stored(at least metadata are not stored)
            metadataToStore.addAll(self.prepareNotFullyStored(tenantWorkspace));
            if (!metadataToStore.isEmpty()) {
                LOG.debug("Scheduling {} updated metadata files for storage.", metadataToStore.size());
                // now that we know all the metadata that should be stored, lets schedule their storage!
                self.scheduleStorageMetadata(metadataToStore);
            } else {
                LOG.debug("No updated metadata files to store.");
            }
        }
        LOG.debug(" ------------------------> Update AIP storage informations - END <---------------------------- ");
    }

    @Override
    public Set<DataFile> prepareNotFullyStored(Path tenantWorkspace) {
        Set<DataFile> metadataToStore = Sets.newHashSet();
        Set<AIP> notFullyStored = aipDao.findAllByStateInService(AIPState.PENDING, AIPState.STORAGE_ERROR);
        // first lets handle the case where every dataFiles of an AIP are successfully stored.
        for (AIP aip : notFullyStored) {
            Set<DataFile> storedDataFile = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            if (storedDataFile.containsAll(DataFile.extractDataFiles(aip))) {
                // that means all DataFile of this AIP has been stored, lets prepare the metadata storage,
                // first we need to write the metadata into a file
                DataFile meta;
                try {
                    meta = writeMetaToWorkspace(aip, tenantWorkspace);
                    // now if we have a meta to store, lets add it
                    meta.setState(DataFileState.PENDING);
                    dataFileDao.save(meta);
                    metadataToStore.add(meta);
                } catch (IOException | FileCorruptedException e) {
                    // if we don't have a meta to store that means a problem happened and we set the aip to
                    // STORAGE_ERROR
                    LOG.error(e.getMessage(), e);
                    aip.setState(AIPState.STORAGE_ERROR);
                    aipDao.save(aip);
                    publisher.publish(new AIPEvent(aip));
                }
            }
        }
        return metadataToStore;
    }

    @Override
    public Set<AIP> retrieveAipsByTag(String tag) {
        return aipDao.findAllByTags(tag);
    }

    @Override
    public List<Event> retrieveAIPHistory(UniformResourceName ipId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByIpId(ipId.toString());
        if (aip.isPresent()) {
            if (!getSecurityDelegationPlugin().hasAccess(ipId.toString())) {
                throw new EntityOperationForbiddenException(ipId.toString(), AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            return aip.get().getHistory();
        } else {
            throw new EntityNotFoundException(ipId.toString(), AIP.class);
        }
    }

    @Override
    public Set<AIP> retrieveAipsBulk(Set<String> ipIds) {
        return aipDao.findAllByIpIdIn(ipIds);
    }

    @Override
    public AIP retrieveAip(String ipId) throws EntityNotFoundException {
        return aipDao.findOneByIpId(ipId).orElseThrow(() -> new EntityNotFoundException(ipId, AIP.class));
    }

    @Override
    public AIP updateAip(String ipId, AIP updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        Optional<AIP> oldAipOpt = aipDao.findOneByIpId(ipId);
        // first lets check for issues
        if (!oldAipOpt.isPresent()) {
            throw new EntityNotFoundException(ipId, AIP.class);
        }
        AIP oldAip = oldAipOpt.get();
        if ((oldAip.getState() != AIPState.UPDATED) && (oldAip.getState() != AIPState.STORED)) {
            throw new EntityOperationForbiddenException(ipId, AIP.class, "update while aip metadata are being stored!");
        }
        if (updated.getId() == null) {
            throw new EntityNotFoundException("give updated AIP has no id!");
        }
        if (!oldAip.getId().toString().equals(updated.getId().toString())) {
            throw new EntityInconsistentIdentifierException(ipId, updated.getId().toString(), AIP.class);
        }
        LOG.debug(String.format("[METADATA UPDATE] updating metadata of aip %s", ipId));
        // now that requirement are meant, lets update the old one
        AIPBuilder updatingBuilder = new AIPBuilder(oldAip);
        // Only PDI and descriptive information can be updated
        PreservationDescriptionInformation updatedPdi = updated.getProperties().getPdi();
        // Provenance Information
        // first lets merge the events
        updatingBuilder.getPDIBuilder()
                .addProvenanceInformationEvents(updated.getHistory().toArray(new Event[updated.getHistory().size()]));
        // second lets merge other provenance informations
        Map<String, Object> additionalProvenanceInfoMap;
        if ((additionalProvenanceInfoMap = updatedPdi.getProvenanceInformation().getAdditional()) != null) {
            for (Map.Entry<String, Object> additionalProvenanceEntry : additionalProvenanceInfoMap.entrySet()) {
                updatingBuilder.getPDIBuilder()
                        .addAdditionalProvenanceInformation(additionalProvenanceEntry.getKey(),
                                                            additionalProvenanceEntry.getValue());
            }
        }
        // third lets handle those "special" provenance information
        updatingBuilder.getPDIBuilder().setFacility(updatedPdi.getProvenanceInformation().getFacility());
        updatingBuilder.getPDIBuilder().setDetector(updatedPdi.getProvenanceInformation().getDetector());
        updatingBuilder.getPDIBuilder().setFilter(updatedPdi.getProvenanceInformation().getFilter());
        updatingBuilder.getPDIBuilder().setInstrument(updatedPdi.getProvenanceInformation().getInstrument());
        updatingBuilder.getPDIBuilder().setProposal(updatedPdi.getProvenanceInformation().getProposal());
        // Context Information
        // first tags
        updatingBuilder.getPDIBuilder().addTags(updated.getTags().toArray(new String[updated.getTags().size()]));
        // now the rest of them
        Map<String, Object> contextInformationMap;
        if ((contextInformationMap = updatedPdi.getContextInformation()) != null) {
            for (Map.Entry<String, Object> contextEntry : contextInformationMap.entrySet()) {
                // tags have their specific handling
                if (!contextEntry.getKey().equals(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY)) {
                    updatingBuilder.getPDIBuilder().addContextInformation(contextEntry.getKey(),
                                                                          contextEntry.getValue());
                }
            }
        }
        // reference information
        Map<String, String> referenceInformationMap;
        if ((referenceInformationMap = updatedPdi.getReferenceInformation()) != null) {
            for (Map.Entry<String, String> refEntry : referenceInformationMap.entrySet()) {
                // tags have their specific handling
                updatingBuilder.getPDIBuilder().addContextInformation(refEntry.getKey(), refEntry.getValue());
            }
        }
        // fixity information
        Map<String, Object> fixityInformationMap;
        if ((fixityInformationMap = updatedPdi.getFixityInformation()) != null) {
            for (Map.Entry<String, Object> fixityEntry : fixityInformationMap.entrySet()) {
                // tags have their specific handling
                updatingBuilder.getPDIBuilder().addContextInformation(fixityEntry.getKey(), fixityEntry.getValue());
            }
        }
        // Access Right information
        updatingBuilder.getPDIBuilder()
                .setAccessRightInformation(updatedPdi.getAccessRightInformation().getLicence(),
                                           updatedPdi.getAccessRightInformation().getDataRights(),
                                           updatedPdi.getAccessRightInformation().getPublicReleaseDate());
        // descriptive information
        Map<String, Object> descriptiveInformationMap;
        if ((descriptiveInformationMap = updated.getProperties().getDescriptiveInformation()) != null) {
            for (Map.Entry<String, Object> descriptiveEntry : descriptiveInformationMap.entrySet()) {
                updatingBuilder.addDescriptiveInformation(descriptiveEntry.getKey(), descriptiveEntry.getValue());
            }
        }
        // not that all updates are set into the builder, lets build and save the updatedAip. Update event is added
        // thanks once the metadata are stored
        AIP updatedAip = updatingBuilder.build();
        updatedAip.setState(AIPState.UPDATED);
        LOG.debug(String.format("[METADATA UPDATE] Update of aip %s metadata done", ipId));
        LOG.trace(String.format("[METADATA UPDATE] Updated aip : %s", gson.toJson(updatedAip)));
        return aipDao.save(updatedAip);
    }

    @Override
    public Set<UUID> deleteAip(String ipId) throws ModuleException {
        Optional<AIP> toBeDeleted = aipDao.findOneByIpId(ipId);
        if (toBeDeleted.isPresent()) {
            Set<DataFile> dataFilesToDelete = Sets.newHashSet();
            // check if data file are use by any other aip
            Set<DataFile> dataFiles = dataFileDao.findAllByAip(toBeDeleted.get());
            for (DataFile dataFile : dataFiles) {
                // we order deletion of a file if and only if no other aip references the same file
                Set<DataFile> dataFilesWithSameFile = dataFileDao
                        .findAllByChecksumIn(Sets.newHashSet(dataFile.getChecksum()));
                // well lets remove ourselves of course!
                dataFilesWithSameFile.remove(dataFile);
                if (dataFilesWithSameFile.isEmpty()) {
                    // add to datafiles that should be removed
                    dataFilesToDelete.add(dataFile);
                } else {
                    // if other datafiles are referencing a file, we just remove the data file from the database.
                    dataFileDao.remove(dataFile);
                }
            }
            // schedule removal of data and metadata
            toBeDeleted.get().setState(AIPState.DELETED);
            aipDao.save(toBeDeleted.get());
            return scheduleDeletion(dataFilesToDelete);
        }
        return Sets.newHashSet();
    }

    private Set<UUID> scheduleDeletion(Set<DataFile> dataFilesToDelete) throws ModuleException {
        IAllocationStrategy allocationStrategy = getAllocationStrategy();
        // FIXME: should probably set the tenant into maintenance in case of module exception

        Multimap<PluginConfiguration, DataFile> deletionWorkingSetMap = allocationStrategy.dispatch(dataFilesToDelete);
        LOG.trace("{} data objects has been dispatched between {} data storage by allocation strategy",
                  dataFilesToDelete.size(), deletionWorkingSetMap.keySet().size());
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        checkDispatch(dataFilesToDelete, deletionWorkingSetMap);
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : deletionWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(deletionWorkingSetMap.get(dataStorageConf),
                                                                   dataStorageConf);
            LOG.trace("Preparing a job for each working subsets");
            // lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets store a job for
                // each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                jobsToSchedule
                        .add(new JobInfo(0, parameters, authResolver.getUser(), DeleteDataFilesJob.class.getName()));

            }
        }
        Set<UUID> jobIds = Sets.newHashSet();
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.createAsQueued(job).getId());
        }
        return jobIds;
    }

    /**
     * Write on disk the asscoiated metadata file of the given {@link AIP}.
     * @param aip {@link AIP}
     * @param tenantWorkspace {@link Path} of the directory where to write the AIP metadata file.
     * @return {@link DataFile} of the {@link AIP} metadata file.
     * @throws IOException Impossible to write {@link AIP} metadata file to disk.
     */
    private DataFile writeMetaToWorkspace(AIP aip, Path tenantWorkspace) throws IOException, FileCorruptedException {

        DataFile metadataAipFile = null;
        String checksumAlgorithm = "MD5";
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        String toWrite = gson.toJson(aip);
        String checksum = ChecksumUtils.getHexChecksum(md5.digest(toWrite.getBytes(StandardCharsets.UTF_8)));
        Path metadataLocation = Paths.get(tenantWorkspace.toFile().getAbsolutePath(), checksum + JSON_FILE_EXT);
        try (BufferedWriter writer = Files.newBufferedWriter(metadataLocation, StandardCharsets.UTF_8)) {
            writer.write(toWrite);
            writer.flush();
        }
        try (InputStream is = Files.newInputStream(metadataLocation)) {
            String fileChecksum = ChecksumUtils.computeHexChecksum(is, checksumAlgorithm);
            if (fileChecksum.equals(checksum)) {
                URL urlToMetadata = new URL("file", "localhost", metadataLocation.toString());
                metadataAipFile = new DataFile(urlToMetadata, checksum, checksumAlgorithm, DataType.AIP,
                        urlToMetadata.openConnection().getContentLengthLong(), new MimeType("application", "json"), aip,
                        aip.getId().toString() + JSON_FILE_EXT);
            } else {
                LOG.error(String.format(
                                        "Storage of AIP metadata(%s) to the workspace(%s) failed. Its checksum once stored do not match with expected",
                                        aip.getId().toString(), tenantWorkspace));
                throw new FileCorruptedException(
                        String.format("File got corrupted while storing it into the workspace. Checksum before(%s) and after (%s) are different",
                                      checksum, fileChecksum));
            }
        } catch (Exception e) {
            // Delete written file
            if (!metadataLocation.toFile().delete()) {
                LOG.error("Error deleting invalid metadata AIP file {}", metadataLocation);
            }
            throw new IOException(e);
        }
        return metadataAipFile;
    }

    /*
     * Non javadoc, but explanatory: due to settings only interfaces are proxyfied by spring, so we need to use a self
     * reference on the interface to profit from transaction management from spring. This is a self reference because
     * AIPService is annotated @Service with default component scope which is "spring' SINGLETON
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void updateAlreadyStoredMetadata() {
        // Then lets get AIP that should be stored again after an update
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Path tenantWorkspace = Paths.get(workspace, tenant);
            if (!Files.exists(tenantWorkspace)) {
                try {
                    Files.createDirectories(tenantWorkspace);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    // TODO: notify, probably should set the system into maintenance mode...
                }
            }
            LOG.debug(String.format("[METADATA UPDATE DAEMON] Starting to prepare update jobs for tenant %s", tenant));
            Set<UpdatableMetadataFile> metadataToUpdate = self.prepareUpdatedAIP(tenantWorkspace);
            if (!metadataToUpdate.isEmpty()) {
                self.scheduleStorageMetadataUpdate(metadataToUpdate);
            }
            LOG.debug(String.format("[METADATA UPDATE DAEMON] Update jobs for tenant %s have been scheduled", tenant));
        }
    }

    /**
     * Prepare all AIP in UPDATED state in order to store and store the new AIP metadata file (descriptor file)
     * asscoiated.<br/>
     * After an AIP is updated in database, this method write the new {@link DataFile} of the AIP metadata on disk
     * and return the list of created {@link DataFile} mapped to the old {@link DataFile} of the updated AIPs.
     * @param tenantWorkspace {@link Path} to the local directory where to write AIP files.
     * @return {@link Set}<{@link UpdatableMetadataFile}> The list of {@link DataFile} to store.
     */
    @Override
    public Set<UpdatableMetadataFile> prepareUpdatedAIP(Path tenantWorkspace) {
        Set<UpdatableMetadataFile> result = Sets.newHashSet();
        Set<AIP> updatedAips = aipDao.findAllByStateService(AIPState.UPDATED);
        for (AIP updatedAip : updatedAips) {
            // Store the associated dataFile.
            Optional<DataFile> optionalExistingAIPMetadataFile = dataFileDao.findByAipAndType(updatedAip, DataType.AIP);
            if (optionalExistingAIPMetadataFile.isPresent()) {
                // Create new AIP file (descriptor file) for the given updated AIP.
                DataFile existingAIPMetadataFile = optionalExistingAIPMetadataFile.get();
                DataFile newAIPMetadataFile;
                try {
                    // To ensure that at any time there is only one DataFile of AIP type, we do not store
                    // a new DataFile for the newAIPMetadataFile.
                    // The newAIPMetadataFile get the id of the old one and so only replace it when it is stored.
                    newAIPMetadataFile = writeMetaToWorkspace(updatedAip, tenantWorkspace);
                    newAIPMetadataFile.setId(existingAIPMetadataFile.getId());
                    result.add(new UpdatableMetadataFile(existingAIPMetadataFile, newAIPMetadataFile));
                } catch (IOException | FileCorruptedException e) {
                    LOG.error(e.getMessage(), e);
                    // if we don't have a meta to store that means a problem happened and we set the aip to
                    // STORAGE_ERROR
                    updatedAip.setState(AIPState.STORAGE_ERROR);
                    aipDao.save(updatedAip);
                    publisher.publish(new AIPEvent(updatedAip));
                }
            } else {
                LOG.warn("Unable to update AIP metadata for AIP {} as there no existing one");
                // TODO : Notify ?
            }
        }
        return result;
    }

    @Override
    public void scheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate) {
        try {
            // we need to listen to those jobs event for two things: cleaning the workspace and update AIP state
            doScheduleStorageMetadataUpdate(metadataToUpdate);
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to
            // STORING_METADATA
            Set<AIP> aips = metadataToUpdate.stream().map(oldNew -> oldNew.getNewOne().getAip())
                    .collect(Collectors.toSet());
            for (AIP aip : aips) {
                LOG.debug("[UPDATING AIP METADATA] AIP {} is in STORING_METADATA state", aip.getId().toString());
                aip.setState(AIPState.STORING_METADATA);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
            // TODO: notify, probably should set the system into maintenance mode...
        }
    }

    /**
     * Schedule new {@link UpdateDataFilesJob}s to store given updated {@link DataFile}.
     *
     * To do so, this method dispatch all {@link DataFile} of new AIP metadata files to store
     * by {@link PluginConfiguration} of storage plugin used to store associated {@link DataFile}
     * of old AIP metadata.<br/>
     * Then, a new job si scheduled for each {@link IWorkingSubset} returned by
     * the associated {@link PluginConfiguration}s.<br/>
     *
     * @param metadataToUpdate List of {@link DataFile} of new AIP metadata files mapped to old ones.
     * @return {@link Set}<{@link UUID}> List of all Jobs id scheduled.
     * @throws ModuleException
     */
    private Set<UUID> doScheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate)
            throws ModuleException {
        // This is an update so we don't use the allocation strategy and we directly use the PluginConf used to store
        // the file.
        // Lets construct the Multimap<PluginConf, DataFile> allowing us to then store IWorkingSubSets
        Multimap<PluginConfiguration, DataFile> toPrepareMap = HashMultimap.create();
        for (UpdatableMetadataFile oldNew : metadataToUpdate) {
            toPrepareMap.put(oldNew.getOldOne().getDataStorageUsed(), oldNew.getNewOne());
        }
        // now lets work with workingSubsets
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : toPrepareMap.keySet()) {
            Set<IWorkingSubset> workingSubsets = getWorkingSubsets(toPrepareMap.get(dataStorageConf), dataStorageConf);
            for (IWorkingSubset workingSubset : workingSubsets) {
                // for each workingSubset lets get the corresponding old metadata to remove
                Set<DataFile> oldOneCorrespondingToWorkingSubset = metadataToUpdate.stream()
                        .filter(oldNew -> workingSubset.getDataFiles().contains(oldNew.getNewOne()))
                        .map(oldNew -> oldNew.getOldOne()).collect(Collectors.toSet());
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                parameters.add(new JobParameter(UpdateDataFilesJob.OLD_DATA_FILES_PARAMETER_NAME,
                        oldOneCorrespondingToWorkingSubset
                                .toArray(new DataFile[oldOneCorrespondingToWorkingSubset.size()])));
                jobsToSchedule
                        .add(new JobInfo(0, parameters, authResolver.getUser(), UpdateDataFilesJob.class.getName()));
            }
        }
        // scheduleJob for files just give to the job the AIP ipId or id
        Set<UUID> jobIds = Sets.newHashSet();
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.createAsQueued(job).getId());
        }
        return jobIds;
    }

    @Override
    public Optional<DataFile> getAIPDataFile(String pAipId, String pChecksum) throws ModuleException {

        // First find the AIP
        Optional<AIP> oaip = aipDao.findOneByIpId(pAipId);
        if (oaip.isPresent()) {
            AIP aip = oaip.get();
            if (!getSecurityDelegationPlugin().hasAccess(pAipId)) {
                throw new EntityOperationForbiddenException(pAipId, AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            // Now get requested DataFile
            Set<DataFile> aipDataFiles = dataFileDao.findAllByAip(aip);
            Optional<DataFile> odf = aipDataFiles.stream().filter(df -> pChecksum.equals(df.getChecksum())).findFirst();
            if (odf.isPresent()) {
                DataFile dataFile = odf.get();
                if (dataFile.getDataStorageUsed() != null) {
                    if (dataFile.getDataStorageUsed().getInterfaceNames()
                            .contains(IOnlineDataStorage.class.getName())) {
                        return Optional.of(dataFile);
                    } else {
                        // Check if file is available from cache
                        Optional<CachedFile> ocf = cachedFileService.getAvailableCachedFile(pChecksum);
                        if (ocf.isPresent()) {
                            dataFile.setUrl(ocf.get().getLocation());
                            return Optional.of(dataFile);
                        } else {
                            return Optional.empty();
                        }
                    }
                } else {
                    throw new EntityNotFoundException("Storage plugin used to store datafile is unknown.");
                }
            } else {
                throw new EntityNotFoundException(pChecksum, DataFile.class);
            }

        } else {
            throw new EntityNotFoundException(pAipId, AIP.class);
        }
    }
}
