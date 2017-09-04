/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
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
import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.InformationObject;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.AIPValid;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;
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

    private final IAIPDao dao;

    private final IPublisher publisher;

    private final ISubscriber subscriber;

    private final PluginService pluginService;

    private final IJobInfoService jobInfoService;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final Gson gson;

    private final IDataFileDao dataFileDao;

    private String workspace;

    public AIPService(IAIPDao dao, IPublisher publisher, PluginService pluginService, ISubscriber subscriber,
            IJobInfoService jobInfoService, ITenantResolver tenantResolver,
            IRuntimeTenantResolver runtimeTenantResolver, Gson gson, IDataFileDao dataFileDao,
            @Value("${regards.storage.workspace}") String workspace) {
        this.dao = dao;
        this.publisher = publisher;
        this.pluginService = pluginService;
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.gson = gson;
        this.dataFileDao = dataFileDao;
        this.workspace = workspace;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DataStorageEvent.class, new DataStorageEventHandler());
    }

    private class DataStorageEventHandler implements IHandler<DataStorageEvent> {

        @Override
        public void handle(TenantWrapper<DataStorageEvent> wrapper) {
            String tenant = wrapper.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            DataStorageEvent event = wrapper.getContent();
            DataFile data = event.getDataFile();
            StorageAction action = event.getStorageAction();
            StorageEventType type = event.getType();
            switch (action) {
                case STORE:
                    handleStoreAction(type, data);
                    break;
                case DELETION:
                    handleDeletionAction(type, data);
                    break;
                default:
                    break;
            }
            runtimeTenantResolver.clearTenant();
        }

        private void handleDeletionAction(StorageEventType type, DataFile data) {
            switch (type) {
                case SUCCESSFUL:
                    //update data status
                    //                    dataFileDao.remove(data);
                    //FIXME: what do we do on AIP here? change the meta or not? do we change meta on removal query?
                    break;
                case FAILED:
                    //update data status
                    //FIXME: what to do?
                    break;
            }
        }

        private void handleStoreAction(StorageEventType type, DataFile data) {
            switch (type) {
                case SUCCESSFUL:
                    // update data status
                    data.setState(DataFileState.STORED);
                    dataFileDao.save(data);
                    if (data.getType() == DataType.AIP) {
                        // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
                        // if we just stored the AIP, there is nothing to do but changing AIP state
                        data.getAip().setState(AIPState.STORED);
                        dao.save(data.getAip());
                    } else {
                        // if it is not the AIP metadata then the AIP metadata are not even scheduled for storage,
                        // just let set the new information about this DataFile
                        AIP aip = dao.findOneByIpId(data.getAip().getIpId());
                        InformationObject io = aip.getInformationObjects().stream()
                                .filter(informationObject -> informationObject.getPdi().getFixityInformation()
                                        .getChecksum().equals(data.getChecksum())).findFirst().get();
                        io.getPdi().getFixityInformation().setFileSize(data.getFileSize());
                        io.getContentInformation().getDataObject().setUrl(data.getOriginUrl());
                        dao.save(aip);
                    }
                    break;
                case FAILED:
                    //update data status
                    data.setState(DataFileState.ERROR);
                    dataFileDao.save(data);
                    AIP aip = data.getAip();
                    aip = dao.findOneByIpId(aip.getIpId());
                    aip.setState(AIPState.STORAGE_ERROR);
                    dao.save(aip);
                    break;
            }
        }
    }

    /**
     * AIP has been validated by Controller REST. Validation of an AIP is only to check if network has not corrupted
     * informations.(There is another validation point when each file is stocked as file are only downloaded by
     * asynchronous task)
     *
     */
    @Override
    public Set<UUID> create(Set<AIP> aips) throws ModuleException {
        // save into DB as valid
        // TODO: check with rs-ingest if ipIds are already set or not
        Set<AIP> aipsInDb = Sets.newHashSet();
        Set<DataFile> dataFilesToStore = Sets.newHashSet();
        for (AIP aip : aips) {
            aip.setState(AIPState.VALID);
            aipsInDb.add(dao.save(aip));
            dataFilesToStore.addAll(dataFileDao.save(DataFile.extractDataFiles(aip)));
            // Publish AIP_VALID
            publisher.publish(new AIPValid(aip));
        }
        IAllocationStrategy allocationStrategy = getAllocationStrategy(); //FIXME: should probably set the tenant into maintenance in case of module exception

        // now lets ask to the strategy to dispatch dataFiles between possible DataStorages
        Multimap<PluginConfiguration, DataFile> storageWorkingSetMap = allocationStrategy.dispatch(dataFilesToStore);
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        Set<DataFile> dataFilesInSubSet = storageWorkingSetMap.entries().stream().map(entry -> entry.getValue())
                .collect(Collectors.toSet());
        if (dataFilesToStore.size() != dataFilesInSubSet.size()) {
            Set<DataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToStore);
            notSubSetDataFiles.removeAll(dataFilesInSubSet);
            for (DataFile prepareFailed : notSubSetDataFiles) {
                prepareFailed.setState(DataFileState.ERROR);
                prepareFailed.getAip().setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                dao.save(prepareFailed.getAip());
                //TODO: notify
            }
        }
        Set<UUID> jobIds = scheduleStorage(storageWorkingSetMap, true);
        // change the state to PENDING
        for (AIP aip : aipsInDb) {
            aip.setState(AIPState.PENDING);
            dao.save(aip);
        }

        return jobIds;
    }

    private Set<UUID> scheduleUpdate(Set<History> metadataToUpdate) throws ModuleException {
        // This is an update so we don't use the allocation strategy and we directly use the PluginConf used to store the file.
        // Lets construct the Multimap<PluginConf, DataFile> allowing us to then create IWorkingSubSets
        Multimap<PluginConfiguration, DataFile> toPrepareMap = HashMultimap.create();
        for (History oldNew : metadataToUpdate) {
            toPrepareMap.put(oldNew.getOldOne().getDataStorageUsed(), oldNew.getNewOne());
        }
        //now lets work with workingSubsets
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : toPrepareMap.keySet()) {
            Set<IWorkingSubset> workingSubsets = getWorkingSubsets(toPrepareMap, dataStorageConf);
            for (IWorkingSubset workingSubset : workingSubsets) {
                //for each workingSubset lets get the corresponding old metadata to remove
                Set<DataFile> oldOneCorrespondingToWorkingSubset = metadataToUpdate.stream()
                        .filter(oldNew -> workingSubset.getDataFiles().contains(oldNew.getNewOne()))
                        .map(oldNew -> oldNew.getOldOne()).collect(Collectors.toSet());
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                parameters.add(new JobParameter(UpdateDataFilesJob.OLD_DATA_FILES_PARAMETER_NAME,
                                                oldOneCorrespondingToWorkingSubset));
                jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), UpdateDataFilesJob.class.getName(),
                                               new JobStatusInfo()));
            }
        }
        // scheduleJob for files just give to the job the AIP ipId or id
        Set<UUID> jobIds = null;
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.create(job).getId());
        }
        return jobIds;
    }

    public Set<UUID> scheduleStorage(Multimap<PluginConfiguration, DataFile> storageWorkingSetMap, boolean storingData)
            throws ModuleException {
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : storageWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(storageWorkingSetMap, dataStorageConf);
            //lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                //for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a job for each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));

                if (storingData) {
                    jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), StoreDataFilesJob.class.getName(),
                                                   new JobStatusInfo()));
                } else {
                    jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), StoreMetadataFilesJob.class.getName(),
                                                   new JobStatusInfo()));
                }

            }
        }

        // scheduleJob for files just give to the job the AIP ipId or id
        Set<UUID> jobIds = null;
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.create(job).getId());
        }
        return jobIds;
    }

    protected Set<IWorkingSubset> getWorkingSubsets(Multimap<PluginConfiguration, DataFile> storageWorkingSetMap,
            PluginConfiguration dataStorageConf) throws ModuleException {
        IDataStorage storage = pluginService.getPlugin(dataStorageConf);
        Collection<DataFile> dataFilesToSubSet = storageWorkingSetMap.get(dataStorageConf);
        Set<IWorkingSubset> workingSubSets = storage.prepare(dataFilesToSubSet);
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
                prepareFailed.getAip().setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                dao.save(prepareFailed.getAip());
                //TODO: notify
            }
        }
    }

    public IAllocationStrategy getAllocationStrategy() throws ModuleException {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> allocationStrategies = pluginService
                .getPluginConfigurationsByType(IAllocationStrategy.class);
        List<PluginConfiguration> activeAllocationStrategies = allocationStrategies.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeAllocationStrategies.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of " + IAllocationStrategy.class
                            .getName());
            throw e;
        }
        return pluginService.getPlugin(activeAllocationStrategies.get(0));
    }

    private void scheduleStorageMetadata(Set<DataFile> metadataToStore) {
        try {
            IAllocationStrategy allocationStrategy = getAllocationStrategy();

            // we need to listen to those jobs event to clean up the workspace
            Set<UUID> jobsToMonitor = scheduleStorage(allocationStrategy.dispatch(metadataToStore), false);
            //TODO: save those jobs uuid somewhere
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to STORING_METADATA
            for (DataFile dataFile : metadataToStore) {
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORING_METADATA);
                dao.save(aip);
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
            //TODO: notify, probably should set the system into maintenance mode...
        }
    }

    private void scheduleStorageMetadataUpdate(Set<History> metadataToUpdate) {
        try {
            // we need to listen to those jobs event for two things: cleaning the workspace and update AIP state
            Set<UUID> jobsToMonitor = scheduleUpdate(metadataToUpdate);
            //TODO: save those jobs uuid somewhere
            //to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to STORING_METADATA
            Set<AIP> aips = metadataToUpdate.stream().map(oldNew -> oldNew.getNewOne().getAip())
                    .collect(Collectors.toSet());
            for (AIP aip : aips) {
                aip.setState(AIPState.STORING_METADATA);
                dao.save(aip);
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
            //TODO: notify, probably should set the system into maintenance mode...
        }
    }

    private class History {

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

    private Set<History> prepareUpdatedAIP(String tenantWorkspace) {
        Set<History> result = Sets.newHashSet();
        Set<AIP> aips = dao.findAllByStateService(AIPState.UPDATED);
        for (AIP aip : aips) {
            DataFile meta = writeMetaToWorkspace(aip, tenantWorkspace);
            if (meta != null) {
                // now if we have a meta to store, lets add it
                DataFile oldOne = dataFileDao.findByAipAndType(aip, DataType.AIP);
                result.add(new History(oldOne, meta));
            } else {
                // if we don't have a meta to store that means a problem happened and we set the aip to STORAGE_ERROR
                aip.setState(AIPState.STORAGE_ERROR);
                dao.save(aip);
            }
        }
        return result;
    }

    public Set<DataFile> prepareNotFullyStored(String tenantWorkspace) {
        Set<DataFile> metadataToStore = Sets.newHashSet();
        Set<AIP> notFullyStored = dao.findAllByStateService(AIPState.PENDING);
        // first lets handle the case where every dataFiles of an AIP are successfully stored.
        for (AIP aip : notFullyStored) {
            Set<DataFile> storedDataFile = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            if (storedDataFile.containsAll(DataFile.extractDataFiles(aip))) {
                // that means all DataFile of this AIP has been stored, lets prepare the metadata storage,
                // first we need to write the metadata into a file
                DataFile meta = writeMetaToWorkspace(aip, tenantWorkspace);
                if (meta != null) {
                    // now if we have a meta to store, lets add it
                    metadataToStore.add(meta);
                } else {
                    // if we don't have a meta to store that means a problem happened and we set the aip to STORAGE_ERROR
                    aip.setState(AIPState.STORAGE_ERROR);
                    dao.save(aip);
                }
            }
        }
        return metadataToStore;
    }

    private DataFile writeMetaToWorkspace(AIP aip, String tenantWorkspace) {

        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // why is it a checked exception??????????
            LOG.error("You should spank your devs for being rude!", e);
            // we throw a runtime that will break the loop over tenants but that's not an issue as this should happens for every tenant
            throw new RuntimeException(e);
        }
        String toWrite = gson.toJson(aip);
        String checksum = ChecksumUtils.getHexChecksum(md5.digest(toWrite.getBytes(StandardCharsets.UTF_8)));
        Path metadataLocation = Paths.get(tenantWorkspace, checksum + ".json");

        try {
            BufferedWriter writer = Files.newBufferedWriter(metadataLocation, StandardCharsets.UTF_8);

            writer.write(toWrite);

            writer.flush();
            writer.close();
            //lets check the storage
            DigestInputStream dis = new DigestInputStream(Files.newInputStream(metadataLocation), md5);
            while (dis.read() != -1) {
            }
            String fileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
            if (!fileChecksum.equals(checksum)) {
                //if checksum differs, remove the file from the workspace and just wait for the next try?
                LOG.error(String.format(
                        "Storage of AIP metadata(%s) to the workspace(%s) failed. Its checksum once stored do not match with expected",
                        aip.getIpId(), tenantWorkspace));
                metadataLocation.toFile().delete();
                return null;
            } else {
                // then we create a DataFile with originUrl set to the created file
                try {
                    URL urlToMetadata = new URL("file", "localhost", metadataLocation.toString());
                    return new DataFile(urlToMetadata, checksum, md5.getAlgorithm(), DataType.AIP,
                                        urlToMetadata.openConnection().getContentLengthLong(),
                                        new MimeType("application", "json"), aip);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(
                            "url is malformed without help of the rest of the world, go spank your devs", e);
                } catch (IOException e) {
                    throw new RuntimeException("we could not get the size of a file we just wrote, go spank your devs",
                                               e);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //TODO: notify
            metadataLocation.toFile().delete();
            return null;
        }

    }

    private String getOwner() {
        return SecurityUtils.getActualUser();
    }

    /**
     * two {@link OffsetDateTime} are here considered equals to the second
     */
    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable) { // NOSONAR
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                          pTo.plusSeconds(1),
                                                                                          pPageable);
                }
                return dao.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return dao.findAllByStateAndLastEventDateBefore(pState, pTo.plusSeconds(1), pPageable);
            }
            return dao.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return dao.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1), pTo.plusSeconds(1),
                                                                              pPageable);
            }
            return dao.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return dao.findAllByLastEventDateBefore(pTo.plusSeconds(1), pPageable);
        }
        //        return dao.findAll(pPageable);
        return null;
    }

    @Override
    public List<DataObject> retrieveAIPFiles(UniformResourceName pIpId) throws EntityNotFoundException {
        //        AIP aip = dao.findOneByIpIdWithDataObjects(pIpId.toString());
        //        if (aip == null) {
        //            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        //        }
        //        return aip.getDataObjects();
        return null;
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> versions = dao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getIpId()).collect(Collectors.toList());
    }

    /**
     * Scheduled to be executed every minute after the end of the last invocation.
     * Waiting one minute in the worst case to eventually store the aip metadata seems acceptable and it might allow us to treat multiple aip metadata
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional(propagation = Propagation.SUPPORTS)
    public void storeMetadata() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            String tenantWorkspace = workspace + "/" + tenant;
            try {
                Files.createDirectory(Paths.get(tenantWorkspace));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                //FIXME: find a way to notify the admins/instance admin maybe thanks to notification module from rs-admin
                continue;
            }
            Set<DataFile> metadataToStore = Sets.newHashSet();
            // first lets get AIP that are not fully stored(at least metadata are not stored)
            metadataToStore.addAll(prepareNotFullyStored(tenantWorkspace));

            //now that we know all the metadata that should be stored, lets schedule their storage!
            scheduleStorageMetadata(metadataToStore);
        }
    }

    /**
     * Scheduled to be executed every 24H
     */
    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAlreadyStoredMetadata() {
        // Then lets get AIP that should be restored after an update
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            String tenantWorkspace = workspace + "/" + tenant;
            try {
                Files.createDirectory(Paths.get(tenantWorkspace));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                //FIXME: find a way to notify the admins/instance admin maybe thanks to notification module from rs-admin
                continue;
            }
            Set<History> metadataToUpdate = prepareUpdatedAIP(tenantWorkspace);
            scheduleStorageMetadataUpdate(metadataToUpdate);
        }
    }
}
