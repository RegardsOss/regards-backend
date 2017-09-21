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
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
@RegardsTransactional
public class AIPService implements IAIPService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AIPService.class);

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * to get transactionnality inside scheduled
     */
    @Autowired
    private IAIPService self;

    @Value("${regards.storage.workspace}")
    private String workspace;

    @Autowired
    private ICachedFileService cachedFileService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DataStorageEvent.class, new DataStorageEventHandler(), WorkerMode.SINGLE,
                               Target.MICROSERVICE);
    }

    /**
     * Handler for DataStorageEvent events. This events are sent by the {@link ProgressManager} associated
     * to the {@link IDataStorage} plugins. After each {@link DataFile} stored, deleted or restored a {@link DataStorageEvent}
     * should be sent thought the {@link ProgressManager}.
     * @author Sébastien Binda
     */
    private class DataStorageEventHandler implements IHandler<DataStorageEvent> {

        @Override
        public void handle(TenantWrapper<DataStorageEvent> wrapper) {
            String tenant = wrapper.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            DataStorageEvent event = wrapper.getContent();
            StorageAction action = event.getStorageAction();
            StorageEventType type = event.getType();
            switch (action) {
                case STORE:
                    handleStoreAction(type, event);
                    break;
                case DELETION:
                    handleDeletionAction(type, event);
                    break;
                case RESTORATION:
                    handleRestorationAction(type, event);
                    break;
                default:
                    throw new EnumConstantNotPresentException(StorageAction.class, action.toString());
            }
            runtimeTenantResolver.clearTenant();
        }

        /**
         * Handle {@link DataFile} restoration events.
         * @param type {@link StorageEventType}
         * @param event {@link DataStorageEvent}
         */
        private void handleRestorationAction(StorageEventType type, DataStorageEvent event) {
            DataFile data = dataFileDao.findOneById(event.getDataFileId());
            Path restorationPath = event.getRestorationPath();
            switch (type) {
                case SUCCESSFULL:
                    cachedFileService.handleRestorationSuccess(data, restorationPath);
                    publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, data.getChecksum()));
                    break;
                case FAILED:
                    cachedFileService.handleRestorationFailure(data);
                    publisher.publish(new DataFileEvent(DataFileEventState.ERROR, data.getChecksum()));
                    break;
                default:
                    break;
            }
        }

        /**
         * Handle {@link DataFile} deletion events.
         * @param type {@link StorageEventType}
         * @param event {@link DataStorageEvent}
         */
        private void handleDeletionAction(StorageEventType type, DataStorageEvent event) {
            DataFile data = dataFileDao.findOneById(event.getDataFileId());
            if (data != null) {
                switch (type) {
                    case SUCCESSFULL:
                        if (data.getChecksum().equals(event.getChecksum())) {
                            AIP aip = aipDao.findOneByIpId(data.getAip().getIpId());
                            DataFile file = dataFileDao.findByAipAndType(aip, DataType.AIP);
                            // If deleted file is not AIP metadata file
                            // or if the AIP metadata file deleted is the current associated one
                            // Set the AIP to updated state.
                            if (!DataType.AIP.equals(data.getDataType()) || !(file.getId().equals(data.getId()))) {
                                Set<InformationObject> iosToRemove = aip
                                        .getInformationObjects().stream().filter(io -> io.getPdi()
                                                .getFixityInformation().getChecksum().equals(data.getChecksum()))
                                        .collect(Collectors.toSet());
                                aip.getInformationObjects().removeAll(iosToRemove);
                                aip.setState(AIPState.UPDATED);
                                aipDao.save(aip);
                                dataFileDao.remove(data);
                                LOG.debug("[DELETE FILE SUCCESS] AIP {} is in UPDATED state", data.getAip().getIpId());
                            } else {
                                LOG.debug("[DELETE FILE SUCCESS] AIP metadata file replaced.", data.getAip().getIpId());
                            }
                        }
                        //otherwise we consider it comes from an update and the aip should not be set to updated
                        break;
                    case FAILED:
                    default:
                        //FIXME: what to do?
                        // update data status
                        // dataFileDao.remove(data);
                        // FIXME: what do we do on AIP here? change the meta or not? do we change meta on removal query?
                        break;
                }
            } else {
                LOG.error("[DATAFILE DELETION EVENT] Invalid DataFile deletion event. DataFile does not exists in db for id {}",
                          event.getDataFileId());
            }
        }

        /**
         * Handle {@link DataFile} store events.
         * @param type {@link StorageEventType}
         * @param event {@link DataStorageEvent}
         */
        private void handleStoreAction(StorageEventType type, DataStorageEvent event) {
            DataFile data = dataFileDao.findOneById(event.getDataFileId());
            data.setUrl(event.getNewUrl());
            switch (type) {
                case SUCCESSFULL:
                    // update data status
                    PluginConfiguration dataStorageUsed = null;
                    try {
                        dataStorageUsed = pluginService.getPluginConfiguration(event.getStorageConfId());
                    } catch (ModuleException e) {
                        LOG.error("You should not have this issue here! That means that the plugin used to store the dataFile just has been removed from the application",
                                  e);
                        throw new RuntimeException(e);
                    }
                    data.setChecksum(event.getChecksum());
                    data.setFileSize(event.getFileSize());
                    data.setDataStorageUsed(dataStorageUsed);
                    data.setState(DataFileState.STORED);
                    dataFileDao.save(data);
                    LOG.debug("[STORE FILE SUCCESS] DATA FILE {} is in STORED state", data.getUrl());
                    if (data.getDataType() == DataType.AIP) {
                        // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
                        // if we just stored the AIP, there is nothing to do but changing AIP state, and clean the
                        // workspace!
                        Paths.get(workspace, runtimeTenantResolver.getTenant(), data.getChecksum() + ".json").toFile()
                                .delete();
                        AIP aip = data.getAip();
                        aip.setState(AIPState.STORED);
                        aipDao.save(aip);
                        LOG.debug("[STORE FILE SUCCESS] AIP {} is in STORED state", data.getAip().getIpId());
                        publisher.publish(new AIPEvent(aip));
                    } else {
                        // if it is not the AIP metadata then the AIP metadata are not even scheduled for storage,
                        // just let set the new information about this DataFile
                        AIP aip = aipDao.findOneByIpId(data.getAip().getIpId());
                        InformationObject io = aip
                                .getInformationObjects().stream().filter(informationObject -> informationObject.getPdi()
                                        .getFixityInformation().getChecksum().equals(data.getChecksum()))
                                .findFirst().get();
                        io.getPdi().getProvenanceInformation().addEvent(EventType.STORAGE.name(),
                                                                        "File stored into REGARDS");
                        io.getPdi().getFixityInformation().setFileSize(data.getFileSize());
                        io.getContentInformation().getDataObject().setUrl(data.getUrl());
                        io.getContentInformation().getDataObject().setFilename(data.getName());
                        aipDao.save(aip);
                    }
                    break;
                case FAILED:
                    // update data status
                    data.setState(DataFileState.ERROR);
                    dataFileDao.save(data);
                    AIP aip = data.getAip();
                    aip = aipDao.findOneByIpId(aip.getIpId());
                    aip.setState(AIPState.STORAGE_ERROR);
                    aipDao.save(aip);
                    publisher.publish(new AIPEvent(aip));
                    break;
            }
        }
    }

    /**
     * AIP has been validated by Controller REST. Validation of an AIP is only to check if network has not corrupted
     * informations.(There is another validation point when each file is stored as file are only downloaded by
     * asynchronous task)
     *
     */
    @Override
    public Set<UUID> create(Set<AIP> aips) throws ModuleException {
        LOG.trace("Entering method create(Set<AIP>) with {} aips", aips.size());
        Set<AIP> aipsInDb = Sets.newHashSet();
        Set<DataFile> dataFilesToStore = Sets.newHashSet();
        // 1. Create each AIP into database with VALID state.
        // 2. Create each DataFile of each AIP into database with PENDING state.
        for (AIP aip : aips) {
            // Can not create an existing AIP.
            if (aipDao.findOneByIpId(aip.getIpId()) != null) {
                throw new EntityAlreadyExistsException(
                        String.format("AIP with ip id %s already exists", aip.getIpId()));
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
        IAllocationStrategy allocationStrategy = getAllocationStrategy(); // FIXME: should probably set the tenant into
        // maintenance in case of module exception

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

    protected void checkDispatch(Set<DataFile> dataFilesToStore,
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

    public Set<UUID> scheduleStorage(Multimap<PluginConfiguration, DataFile> storageWorkingSetMap, boolean storingData)
            throws ModuleException {
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : storageWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(storageWorkingSetMap, dataStorageConf);
            LOG.trace("Preparing a job for each working subsets");
            // lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a job for
                // each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));

                if (storingData) {
                    jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), StoreDataFilesJob.class.getName()));
                } else {
                    jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), StoreMetadataFilesJob.class.getName()));
                }

            }
        }

        Set<UUID> jobIds = Sets.newHashSet();
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.createAsQueued(job).getId());
        }
        return jobIds;
    }

    protected Set<IWorkingSubset> getWorkingSubsets(Multimap<PluginConfiguration, DataFile> storageWorkingSetMap,
            PluginConfiguration dataStorageConf) throws ModuleException {
        LOG.trace("Getting working subsets for data storage {}", dataStorageConf.getLabel());
        IDataStorage storage = pluginService.getPlugin(dataStorageConf);
        Collection<DataFile> dataFilesToSubSet = storageWorkingSetMap.get(dataStorageConf);
        Set<IWorkingSubset> workingSubSets = storage.prepare(dataFilesToSubSet, DataStorageAccessModeEnum.STORE_MODE);
        LOG.trace("{} data objects were dispatched into {} working subsets", dataFilesToSubSet.size(),
                  workingSubSets.size());
        // as we are trusty people, we check that the prepare gave us back all DataFiles into the WorkingSubSets
        checkPrepareResult(dataFilesToSubSet, workingSubSets);
        return workingSubSets;
    }

    private void checkPrepareResult(Collection<DataFile> dataFilesToSubSet, Set<IWorkingSubset> workingSubSets) {
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
    }

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

    @Override
    public void scheduleStorageMetadata(Set<DataFile> metadataToStore) {
        try {
            IAllocationStrategy allocationStrategy = getAllocationStrategy();

            // we need to listen to those jobs event to clean up the workspace
            Multimap<PluginConfiguration, DataFile> storageWorkingSetMap = allocationStrategy.dispatch(metadataToStore);
            checkDispatch(metadataToStore, storageWorkingSetMap);
            Set<UUID> jobsToMonitor = scheduleStorage(storageWorkingSetMap, false);
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to STORING_METADATA
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

    class History {

        private DataFile oldOne;

        private DataFile newOne;

        public History(DataFile oldOne, DataFile newOne) {
            this.oldOne = oldOne;
            this.newOne = newOne;
        }

        public DataFile getOldOne() {
            return oldOne;
        }

        public void setOldOne(DataFile oldOne) {
            this.oldOne = oldOne;
        }

        public DataFile getNewOne() {
            return newOne;
        }

        public void setNewOne(DataFile newOne) {
            this.newOne = newOne;
        }
    }

    private String getOwner() {
        return SecurityUtils.getActualUser();
    }

    /**
     * two {@link OffsetDateTime} are here considered equals to the second
     */
    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, Pageable pPageable) { // NOSONAR
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
        // return dao.findAll(pPageable);
        return null;
    }

    @Override
    public List<DataObject> retrieveAIPFiles(UniformResourceName pIpId) throws EntityNotFoundException {
        // AIP aip = dao.findOneByIpIdWithDataObjects(pIpId.toString());
        // if (aip == null) {
        // throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        // }
        // return aip.getDataObjects();
        return null;
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> versions = aipDao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getIpId()).collect(Collectors.toList());
    }

    @Override
    public AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest) {
        Set<String> requestedChecksums = availabilityRequest.getChecksums();
        Set<DataFile> dataFiles = dataFileDao.findAllByChecksumIn(requestedChecksums);
        Set<String> errors = Sets.newHashSet();
        //first lets identify the files that we don't recognize
        if (dataFiles.size() != requestedChecksums.size()) {
            Set<String> dataFilesChecksums = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
            errors.addAll(Sets.difference(requestedChecksums, dataFilesChecksums));
        }
        Set<DataFile> onlineFiles = Sets.newHashSet();
        Set<DataFile> nearlineFiles = Sets.newHashSet();
        // for each data file, lets see if it is online or not
        for (DataFile df : dataFiles) {
            if (df.getDataStorageUsed().getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
                onlineFiles.add(df);
            } else {
                nearlineFiles.add(df);
            }
        }
        // now lets ask the cache service to handle nearline restoration and give us the already available ones
        CoupleAvailableError nearlineAvailableAndError = cachedFileService
                .restore(nearlineFiles, availabilityRequest.getExpirationDate());
        for (DataFile inError : nearlineAvailableAndError.getErrors()) {
            errors.add(inError.getChecksum());
        }
        // lets constrcut the result
        AvailabilityResponse availabilityResponse = new AvailabilityResponse(errors, onlineFiles,
                nearlineAvailableAndError.getAvailables());
        return availabilityResponse;
    }

    /**
     * Scheduled to be executed every minute after the end of the last invocation.
     * Waiting one minute in the worst case to eventually store the aip metadata seems acceptable and it might allow us
     * to treat multiple aip metadata
     */
    @Scheduled(fixedDelayString = "${regards.storage.check.aip.metadata.delay:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void storeMetadata() {
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
                // now that we know all the metadata that should be stored, lets schedule their storage!
                self.scheduleStorageMetadata(metadataToStore);
            }
        }
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
                } catch (IOException e) {
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

    /**
     * Write on disk the asscoiated metadata file of the given {@link  AIP}.
     * @param aip {@link AIP}
     * @param tenantWorkspace {@link Path} of the directory where to write the AIP metadata file.
     * @return {@link DataFile} of the {@link AIP} metadata file.
     * @throws IOException Impossible to write {@link AIP} metadata file to disk.
     */
    private DataFile writeMetaToWorkspace(AIP aip, Path tenantWorkspace) throws IOException {

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
        Path metadataLocation = Paths.get(tenantWorkspace.toString(), checksum + ".json");
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
                        aip.getIpId() + ".json");
            } else {
                LOG.error(String.format(
                                        "Storage of AIP metadata(%s) to the workspace(%s) failed. Its checksum once stored do not match with expected",
                                        aip.getIpId(), tenantWorkspace));
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

    /**
     * This method handle the update of physical AIP metadata files associated to {@link AIP} updated in database.
     * This method is periodicly called by {@link UpdateMetadataScheduler}.
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
            Set<History> metadataToUpdate = self.prepareUpdatedAIP(tenantWorkspace);
            if (!metadataToUpdate.isEmpty()) {
                self.scheduleStorageMetadataUpdate(metadataToUpdate);
            }
        }
    }

    /**
     * Prepare all AIP in UPDATED state in order to create and store the new AIP metadata file (descriptor file) asscoiated.<br/>
     * After an AIP is updated in database, this method write the new {@link DataFile} of the AIP metadata on disk
     * and return the list of created {@link DataFile} mapped to the old {@link DataFile} of the updated AIPs.
     * @param tenantWorkspace {@link Path} to the local directory where to write AIP files.
     * @return {@link Set}<{@link History}> The list of {@link DataFile} to store.
     */
    @Override
    public Set<History> prepareUpdatedAIP(Path tenantWorkspace) {
        Set<History> result = Sets.newHashSet();
        Set<AIP> updatedAips = aipDao.findAllByStateService(AIPState.UPDATED);
        for (AIP updatedAip : updatedAips) {
            // Create new AIP file (descriptor file) for the given updated AIP.
            DataFile meta;
            try {
                meta = writeMetaToWorkspace(updatedAip, tenantWorkspace);
                // Store the associated dataFile.
                DataFile oldOne = dataFileDao.findByAipAndType(updatedAip, DataType.AIP);
                meta.setId(oldOne.getId());
                result.add(new History(oldOne, meta));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                // if we don't have a meta to store that means a problem happened and we set the aip to STORAGE_ERROR
                updatedAip.setState(AIPState.STORAGE_ERROR);
                aipDao.save(updatedAip);
                publisher.publish(new AIPEvent(updatedAip));
            }
        }
        return result;
    }

    /**
     * Schedule new {@link UpdateDataFilesJob}s for all {@link DataFile} of AIP metadata files given
     * and set there state to STORING_METADATA.
     * @param metadataToUpdate List of {@link DataFile} of new AIP metadata files mapped to old ones.
     */
    @Override
    public void scheduleStorageMetadataUpdate(Set<History> metadataToUpdate) {
        try {
            // we need to listen to those jobs event for two things: cleaning the workspace and update AIP state
            doScheduleStorageMetadataUpdate(metadataToUpdate);
            //to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to STORING_METADATA
            Set<AIP> aips = metadataToUpdate.stream().map(oldNew -> oldNew.getNewOne().getAip())
                    .collect(Collectors.toSet());
            for (AIP aip : aips) {
                LOG.debug("[UPDATING AIP METADATA] AIP {} is in STORING_METADATA state", aip.getIpId());
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
    private Set<UUID> doScheduleStorageMetadataUpdate(Set<History> metadataToUpdate) throws ModuleException {
        // This is an update so we don't use the allocation strategy and we directly use the PluginConf used to store
        // the file.
        // Lets construct the Multimap<PluginConf, DataFile> allowing us to then create IWorkingSubSets
        Multimap<PluginConfiguration, DataFile> toPrepareMap = HashMultimap.create();
        for (History oldNew : metadataToUpdate) {
            toPrepareMap.put(oldNew.getOldOne().getDataStorageUsed(), oldNew.getNewOne());
        }
        // now lets work with workingSubsets
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : toPrepareMap.keySet()) {
            Set<IWorkingSubset> workingSubsets = getWorkingSubsets(toPrepareMap, dataStorageConf);
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
                jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), UpdateDataFilesJob.class.getName()));
            }
        }
        // scheduleJob for files just give to the job the AIP ipId or id
        Set<UUID> jobIds = Sets.newHashSet();
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.createAsQueued(job).getId());
        }
        return jobIds;
    }
}
