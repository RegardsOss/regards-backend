package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.RestorationSuccessApplicationEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;
import fr.cnes.regards.modules.storage.service.job.WriteAIPMetadataJob;

/**
 * Data storage service
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class DataStorageService implements IDataStorageService {

    /**
     * Metadata updated successfully message
     */
    public static final String METADATA_UPDATED_SUCCESSFULLY = "AIP metadata has been successfully updated";

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStorageService.class);

    /**
     * Metadata stored successfully message
     */
    private static final String METADATA_STORED_SUCCESSFULLY = "AIP metadata has been successfully stored into REGARDS";

    /**
     * Data file stored successfully message format
     */
    private static final String DATAFILE_STORED_SUCCESSFULLY = "File %s has been successfully stored";

    /**
     * Data file deleted successfully message format
     */
    private static final String DATAFILE_DELETED_SUCCESSFULLY = "File %s has been successfully deleted";

    /**
     * Data file url deleted successfully message format
     */
    private static final String DATAFILE_URL_DELETED_SUCCESSFULLY = "Url %s has been successfully deleted for file %s";

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * {@link IDataFileDao} instance
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * DAO to access {@link AIP} entities through the {@link AIPEntity} entities stored in db.
     */
    @Autowired
    private IAIPDao aipDao;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AMQP Publisher.
     */
    @Autowired
    private IPublisher publisher;

    /**
     * {@link INotificationClient} instance
     */
    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ICachedFileService cachedFileService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ApplicationEventPublisher springPublisher;

    /**
     * data storage occupation threshold in percent
     */
    @Value("${regards.storage.data.storage.threshold.percent:70}")
    private Integer threshold;

    @Value("${regards.storage.data.storage.critical.threshold.percent:90}")
    private Integer criticalThreshold;

    @Override
    public Collection<PluginStorageInfo> getMonitoringInfos() throws ModuleException, IOException {
        Set<PluginStorageInfo> monitoringInfos = Sets.newHashSet();
        List<PluginConfiguration> dataStorageConfigurations = pluginService
                .getPluginConfigurationsByType(IDataStorage.class);
        // lets take only the activated ones
        Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toSet());
        // for each active conf, lets get their DataStorageInfo
        // lets ask the data base to calculate the used space per data storage
        Collection<MonitoringAggregation> monitoringAggregations = dataFileDao.getMonitoringAggregation();
        // now lets transform it into Map<Long, Long>, it is easier to use
        Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors.toMap(
                MonitoringAggregation::getDataStorageUsedId,
                MonitoringAggregation::getUsedSize));
        for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
            // lets initialize the monitoring information for this data storage configuration by getting plugin
            // informations
            Long activeDataStorageConfId = activeDataStorageConf.getId();
            PluginMetaData activeDataStorageMeta = pluginService
                    .getPluginMetaDataById(activeDataStorageConf.getPluginId());
            PluginStorageInfo monitoringInfo = new PluginStorageInfo(activeDataStorageConfId,
                                                                     activeDataStorageMeta.getDescription(),
                                                                     activeDataStorageConf.getLabel());
            // now lets get the data storage monitoring information from the plugin
            @SuppressWarnings("rawtypes") Long dataStorageTotalSpace = ((IDataStorage) pluginService
                    .getPlugin(activeDataStorageConfId)).getTotalSpace();
            DataStorageInfo dataStorageInfo;
            if (monitoringAggregationMap.containsKey(activeDataStorageConfId)) {
                dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(),
                                                      dataStorageTotalSpace,
                                                      monitoringAggregationMap.get(activeDataStorageConfId));
            } else {
                dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(), dataStorageTotalSpace, 0);

            }
            monitoringInfo.setTotalSize(dataStorageInfo.getTotalSize());
            monitoringInfo.setUsedSize(dataStorageInfo.getUsedSize());
            monitoringInfo.setRatio(dataStorageInfo.getRatio());

            monitoringInfos.add(monitoringInfo);
        }
        return monitoringInfos;
    }

    @Override
    public void monitorDataStorages() {

        List<PluginConfiguration> dataStorageConfigurations = pluginService
                .getPluginConfigurationsByType(IDataStorage.class);
        // lets take only the activated ones
        Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toSet());
        // lets ask the data base to calculate the used space per data storage
        Collection<MonitoringAggregation> monitoringAggregations = dataFileDao.getMonitoringAggregation();
        if (!monitoringAggregations.isEmpty()) {
            // now lets transform it into Map<Long, Long>, it is easier to use
            Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors.toMap(
                    MonitoringAggregation::getDataStorageUsedId,
                    MonitoringAggregation::getUsedSize));
            // lets instantiate those data storage and get their total space
            for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
                // lets initialize the monitoring information for this data storage configuration by getting plugin
                // informations
                try {
                    IDataStorage<?> activeDataStorage = pluginService.getPlugin(activeDataStorageConf.getId());

                    Long activeDataStorageConfId = activeDataStorageConf.getId();
                    Long dataStorageTotalSpace = activeDataStorage.getTotalSpace();
                    if (monitoringAggregationMap.containsKey(activeDataStorageConfId)) {
                        DataStorageInfo dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(),
                                                                              dataStorageTotalSpace,
                                                                              monitoringAggregationMap
                                                                                      .get(activeDataStorageConfId));
                        Double ratio = dataStorageInfo.getRatio();
                        if (ratio >= criticalThreshold) {
                            String message = String
                                    .format("Data storage(configuration id: %s, configuration label: %s) has reach its "
                                                    + "disk usage critical threshold. %nActual occupation: %.2f%%, critical threshold: %s%%",
                                            activeDataStorageConf.getId().toString(),
                                            activeDataStorageConf.getLabel(),
                                            ratio,
                                            criticalThreshold);
                            LOGGER.error(message);
                            notifyAdmins("Data storage " + activeDataStorageConf.getLabel() + " is almost full",
                                         message,
                                         NotificationLevel.ERROR,
                                         MimeTypeUtils.TEXT_PLAIN);
                            MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
                            return;
                        }
                        if (ratio >= threshold) {
                            String message = String
                                    .format("Data storage(configuration id: %s, configuration label: %s) has reach its "
                                                    + "disk usage threshold. %nActual occupation: %.2f%%, threshold: %s%%",
                                            activeDataStorageConf.getId().toString(),
                                            activeDataStorageConf.getLabel(),
                                            ratio,
                                            threshold);
                            LOGGER.warn(message);
                            notifyAdmins("Data storage " + activeDataStorageConf.getLabel() + " is almost full",
                                         message,
                                         NotificationLevel.WARNING,
                                         MimeTypeUtils.TEXT_PLAIN);
                        }
                    }

                } catch (ModuleException e) {
                    // should never happens, currently it is an exception that cannot be thrown in this code(issues with
                    // dynamic parameters)
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationLevel type, MimeType mimeType) {
        notificationClient.notify(message, title, type, mimeType, DefaultRole.ADMIN);
    }

    @Override
    public void handleRestorationAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> oData = dataFileDao.findOneById(event.getDataFileId());
        if (oData.isPresent()) {
            Path restorationPath = event.getRestorationPath();
            switch (type) {
                case SUCCESSFULL:
                    StorageDataFile data = oData.get();
                    springPublisher.publishEvent(new RestorationSuccessApplicationEvent(data,
                                                                                        restorationPath,
                                                                                        event.getStorageConfId(),
                                                                                        runtimeTenantResolver
                                                                                                .getTenant()));
                    cachedFileService.handleRestorationSuccess(data, restorationPath);
                    break;
                case FAILED:
                    cachedFileService.handleRestorationFailure(oData.get());
                    break;
                default:
                    break;
            }
        } else {
            LOGGER.warn("[DATA STORAGE EVENT] restoration of non existing StorageDataFile id {}",
                        event.getDataFileId());
        }
    }

    @Override
    public void handleDeletionAction(StorageEventType type, DataStorageEvent event) {
        // Check that the given StorageDataFile id is associated to an existing StorageDataFile from db.
        Optional<StorageDataFile> oData = dataFileDao.findLockedOneById(event.getDataFileId());
        Long dataStorageConfId = event.getStorageConfId();
        if (oData.isPresent()) {
            StorageDataFile data = oData.get();
            switch (type) {
                case SUCCESSFULL:
                    handleDeletionSuccess(data, event.getHandledUrl(), event.getChecksum(), dataStorageConfId);
                    break;
                case FAILED:
                default:
                    PrioritizedDataStorage storageConf = null;
                    try {
                        storageConf = prioritizedDataStorageService.retrieve(dataStorageConfId);
                    } catch (EntityNotFoundException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    // IDataStorage plugin used to delete the file is not able to delete the file right now.
                    // Notify administrator and set data file to STORED, because it is its real state for now.
                    String message = String.format("Error deleting file (id: %s, checksum: %s).%n"
                                                           + "Data Storage configuration: %s(%s)%n" + "Error:%s",
                                                   event.getDataFileId(),
                                                   event.getChecksum(),
                                                   event.getStorageConfId(),
                                                   storageConf == null ?
                                                           null :
                                                           storageConf.getDataStorageConfiguration().getLabel(),
                                                   event.getFailureCause());
                    // In case we were on a partial deletion, lets reset data storage removed by IAIPService#deleteFilesFromDataStorage(Collection, Long)
                    if (data.getState() == DataFileState.PARTIAL_DELETION_PENDING) {
                        data.getPrioritizedDataStorages().add(storageConf);
                    } else {
                        AIP aip = data.getAip();
                        aip.setState(AIPState.PARTIAL_DELETION);
                        aipDao.updateAIPStateAndRetry(aip);
                    }
                    data.setState(DataFileState.STORED);
                    dataFileDao.save(data);
                    LOGGER.error(message);
                    notifyAdmins("File deletion error", message, NotificationLevel.INFO, MimeTypeUtils.TEXT_PLAIN);
                    break;
            }
        } else {
            LOGGER.error(
                    "[DATAFILE DELETION EVENT] Invalid StorageDataFile deletion event. StorageDataFile does not exists in db for id {}",
                    event.getDataFileId());
        }
    }

    @Override
    public void handleDeletionSuccess(StorageDataFile dataFileDeleted, URL deletedUrl, String checksumOfDeletedFile,
            Long dataStorageConfId) {
        // Get the associated AIP of the deleted StorageDataFile from db
        Optional<AIP> optionalAssociatedAIP = aipDao.findOneByAipId(dataFileDeleted.getAip().getId().toString());
        // Verify that deleted file checksum match StorageDataFile checksum
        if (optionalAssociatedAIP.isPresent() && dataFileDeleted.getChecksum().equals(checksumOfDeletedFile)) {
            AIP associatedAIP = optionalAssociatedAIP.get();
            // 1. Remove deleted file location from AIP.
            removeDeletedUrlFromDataFile(dataFileDeleted, deletedUrl, associatedAIP, dataStorageConfId);
            if (DataType.AIP.equals(dataFileDeleted.getDataType()) && !associatedAIP.getState()
                    .equals(AIPState.DELETED)) {
                // Do not delete the dataFileDeleted from db. At this time in db the file is the new one that has
                // been stored previously to replace the deleted one. This is a special case for AIP metadata file
                // because, at any time we want to ensure that there is only one StorageDataFile of AIP type for a given AIP.
                LOGGER.info("[DELETE FILE SUCCESS] AIP metadata file replaced.",
                            dataFileDeleted.getAip().getId().toString());
                associatedAIP.addEvent(EventType.UPDATE.name(), METADATA_UPDATED_SUCCESSFULLY);
                aipService.save(associatedAIP, false);
            }
        } else {
            LOGGER.warn("Deleted file checksum {}, does not match StorageDataFile {} checksum {}",
                        checksumOfDeletedFile,
                        dataFileDeleted.getName(),
                        dataFileDeleted.getChecksum());
        }
    }

    /**
     * Handle deletion of one location of the given {@link StorageDataFile} file.
     * @param dataFileDeleted {@link StorageDataFile}
     * @param urlToRemove location deleted.
     * @param associatedAIP {@link AIP} associated to the given {@link StorageDataFile}
     * @param dataStorageConfId
     */
    private void removeDeletedUrlFromDataFile(StorageDataFile dataFileDeleted, URL urlToRemove, AIP associatedAIP,
            Long dataStorageConfId) {

        // If dataFile to delete contains given url to remove, remove URL from it and update associated AIP to add
        // URL remove event.
        if ((urlToRemove != null) && dataFileDeleted.getUrls().contains(urlToRemove)) {
            LOGGER.info("Partial deletion of StorageDataFile {}. One of the location has been removed {}.",
                        dataFileDeleted.getName(),
                        urlToRemove);
            associatedAIP.getProperties().getContentInformations().stream()
                    .filter(ci -> !ci.getDataObject().isReference())
                    .filter(ci -> dataFileDeleted.getChecksum().equals(ci.getDataObject().getChecksum()))
                    .forEach(ci -> ci.getDataObject().getUrls().remove(urlToRemove));
            String message = String.format(DATAFILE_URL_DELETED_SUCCESSFULLY, urlToRemove, dataFileDeleted.getName());
            associatedAIP.addEvent(EventType.DELETION.name(), message);
            LOGGER.info(message);
            aipService.save(associatedAIP, false);
            dataFileDeleted.getUrls().remove(urlToRemove);
            // lets handle partial deletion specificity
            if (dataFileDeleted.getState() == DataFileState.PARTIAL_DELETION_PENDING) {
                try {
                    dataFileDeleted.decreaseNotYetDeletedBy();
                } catch (EntityOperationForbiddenException e) {
                    // in case it is deleted from 1 more data storage than expected it is not blocking as aip will be
                    // updated to take that into account.
                    LOGGER.error(String.format(
                            "Data file %s has been successfuly deleted one more time than expected from IDataStorage"
                                    + " plugin configuration (id: %s).",
                            dataFileDeleted.getId(),
                            dataStorageConfId), e);
                }
                // if there is no more partial deletion, lets set the state back to stored
                if (dataFileDeleted.getNotYetDeletedBy() == 0) {
                    dataFileDeleted.setState(DataFileState.STORED);
                    // if there is no more datafile being partially deleted for this AIP, lets update it so information
                    // stored on other data storages reflects the partial deletion.
                    if (dataFileDao.countByAipAndByState(associatedAIP, DataFileState.PARTIAL_DELETION_PENDING) == 0) {
                        try {
                            aipService.updateAip(associatedAIP.getId().toString(),
                                                 associatedAIP,
                                                 "Deletion of files on specific data storages is done.");
                        } catch (EntityNotFoundException | EntityInconsistentIdentifierException e) {
                            Optional<String> oSipId = associatedAIP.getSipId();
                            String msg = String
                                    .format("AIP (ipId: %s, sipId: %s, providerId: %s, state: %s) could not be updated after "
                                                    + "partial deletion because it has been deleted somewhere else.",
                                            associatedAIP.getId().toString(),
                                            oSipId.isPresent() ? oSipId.get() : "undefinied",
                                            associatedAIP.getProviderId());
                            LOGGER.error(msg, e);
                            throw new RsRuntimeException(msg, e);
                        }
                    }
                }
            }
            dataFileDao.save(dataFileDeleted);
        } else if (urlToRemove != null) {
            LOGGER.warn("Removed URL {} is not associated to the StorageDataFile to delete {}",
                        urlToRemove,
                        dataFileDeleted.getName());
        }

        // If url to remove is null (so all the location are deleted) or if there is no longer any location for the datafile,
        // we can remove it from db and form AIP.
        if ((urlToRemove == null) || dataFileDeleted.getUrls().isEmpty()) {
            LOGGER.info("Datafile to delete does not contains any location url. Deletion of the dataFile {}",
                        dataFileDeleted.getName());
            dataFileDao.remove(dataFileDeleted);
            String message = String.format(DATAFILE_DELETED_SUCCESSFULLY, dataFileDeleted.getName());
            associatedAIP.addEvent(EventType.DELETION.name(), message);
            LOGGER.info(message);
            // Remove content information from aip
            Set<ContentInformation> ciToRemove = associatedAIP.getProperties().getContentInformations().stream()
                    .filter(ci -> !ci.getDataObject().isReference())
                    .filter(ci -> dataFileDeleted.getChecksum().equals(ci.getDataObject().getChecksum()))
                    .collect(Collectors.toSet());
            ciToRemove.forEach(ci -> associatedAIP.getProperties().getContentInformations().remove(ci));
            aipService.save(associatedAIP, false);
        }

        // If associated AIP is not linked to any dataFile anymore, delete aip.
        if (dataFileDao.countByAip(associatedAIP) == 0) {
            aipDao.remove(associatedAIP);
        }
    }

    @Override
    public void handleStoreAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> optionalData = dataFileDao.findLockedOneById(event.getDataFileId());
        if (optionalData.isPresent()) {
            StorageDataFile data = optionalData.get();
            switch (type) {
                case SUCCESSFULL:
                    handleStoreSuccess(data,
                                       event.getChecksum(),
                                       event.getHandledUrl(),
                                       event.getFileSize(),
                                       event.getStorageConfId(),
                                       event.getWidth(),
                                       event.getHeight());
                    break;
                case FAILED:
                    handleStoreFailed(data, event.getFailureCause(), event.getStorageConfId());
                    break;
                default:
                    LOGGER.error("Unhandle DataStorage STORE event type {}", type);
                    break;
            }
        } else {
            LOGGER.warn("[DATA STORAGE EVENT] StorageDataFile stored {} does not exists", event.getDataFileId());
        }
    }

    @Override
    public void handleStoreSuccess(StorageDataFile storedDataFile, String storedFileChecksum, URL storedFileNewURL,
            Long storedFileSize, Long dataStoragePluginConfId, Integer dataWidth, Integer dataHeight) {
        // update data status
        PrioritizedDataStorage prioritizedDataStorageUsed = null;
        try {
            prioritizedDataStorageUsed = prioritizedDataStorageService.retrieve(dataStoragePluginConfId);
        } catch (ModuleException e) {
            LOGGER.error(
                    "You shouldn't have this issue here! This means the plugin used to storeAndCreate the dataFile "
                            + "has just been removed from the application",
                    e);
            return;
        }

        storedDataFile.setChecksum(storedFileChecksum);
        storedDataFile.setFileSize(storedFileSize);
        storedDataFile.addDataStorageUsed(prioritizedDataStorageUsed);
        try {
            storedDataFile.decreaseNotYetStoredBy();
        } catch (EntityOperationForbiddenException e) {
            LOGGER.error(String.format(
                    "Data file %s has been successfuly stored one more time than expected into %s by IDataStorage plugin configuration %s.",
                    storedDataFile.getId(),
                    storedFileNewURL,
                    dataStoragePluginConfId), e);
        }
        storedDataFile.getUrls().add(storedFileNewURL);
        storedDataFile.setHeight(dataHeight);
        storedDataFile.setWidth(dataWidth);
        LOGGER.debug("Datafile {} stored for pluginConfId:{} missing {} confs.",
                     storedDataFile.getId(),
                     dataStoragePluginConfId,
                     storedDataFile.getNotYetStoredBy());
        try {
            if (storedDataFile.getNotYetStoredBy().equals(0L)) {
                LOGGER.debug("[STORE FILE SUCCESS] Datafile {} ({}) is fully stored.",
                             storedDataFile.getName(),
                             storedDataFile.getChecksum());
                storedDataFile.setState(DataFileState.STORED);
                storedDataFile.emptyFailureCauses();
                handleStorageDataFileFullyStored(storedDataFile);
            } else {
                LOGGER.debug(
                        "[STORE FILE SUCCESS] Datafile {} ({}) is not fully stored. Missing {} locations to be stored.",
                        storedDataFile.getName(),
                        storedDataFile.getChecksum(),
                        storedDataFile.getNotYetStoredBy());
            }
        } catch (EntityNotFoundException e) {
            LOGGER.warn(String.format("AIP %s does not exists anymore. Associated file {} stored will be deleted",
                                      storedDataFile.getName()), e);
            storedDataFile.setState(DataFileState.TO_BE_DELETED);
        } finally {
            storedDataFile = dataFileDao.save(storedDataFile);
        }
    }

    /**
     * Handle the case when a {@link StorageDataFile} is fully stored. (File is successfully stored in all his location).
     * <ul>
     * <li>If file is a metadata file then update associated AIP to STORED status.</li>
     * <li>If file is not a metadata file then update associated AIP to add the file locations in it</li>
     * </ul>
     * @param storedDataFile
     */
    private void handleStorageDataFileFullyStored(StorageDataFile storedDataFile) throws EntityNotFoundException {
        Optional<AIP> optionalAssociatedAip = aipDao.findOneByAipId(storedDataFile.getAip().getId().toString());
        if (optionalAssociatedAip.isPresent()) {
            AIP associatedAIP = optionalAssociatedAip.get();
            if (storedDataFile.getDataType() == DataType.AIP) {
                // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
                // if we just stored the AIP, there is nothing to do but changing AIP state, and clean the
                // workspace!
                // Lets clean the workspace
                try {
                    workspaceService
                            .removeFromWorkspace(storedDataFile.getChecksum() + WriteAIPMetadataJob.JSON_FILE_EXT);
                } catch (IOException e) {
                    LOGGER.error("Error during workspace cleaning", e);
                }
                // If  older AIP DataFiles are stored, schedule their deletion by updating their state to TO_BE_DELETED
                dataFileDao.findByAipAndType(associatedAIP, DataType.AIP).forEach(df -> {
                    if (!df.getId().equals(storedDataFile.getId())) {
                        LOGGER.debug("[STORE FILE SUCCESS] Schedule old AIP metadata file {} to be deleted for AIP {}",
                                     df.getName(),
                                     associatedAIP.getProviderId());
                        df.setState(DataFileState.TO_BE_DELETED);
                        dataFileDao.save(df);
                    }
                });

                associatedAIP.setState(AIPState.STORED);
                associatedAIP.addEvent(EventType.STORAGE.name(), METADATA_STORED_SUCCESSFULLY);
                LOGGER.debug("[STORE FILE SUCCESS] AIP {} is in STORED state",
                             storedDataFile.getAip().getId().toString());
                publisher.publish(new AIPEvent(associatedAIP));
            } else {
                // if it is not the AIP metadata then the AIP metadata are not even scheduled for storage,
                // just let set the new information about this StorageDataFile
                // @formatter:off
                final StorageDataFile storedDataFileFinal = storedDataFile;
            Optional<ContentInformation> ci =
                    associatedAIP.getProperties().getContentInformations()
                        .stream()
                        .filter(contentInformation -> !contentInformation.getDataObject().isReference())
                        .filter(contentInformation -> contentInformation.getDataObject().getChecksum().equals(storedDataFileFinal.getChecksum()))
                        .findFirst();
            // @formatter:on
                if (ci.isPresent()) {
                    ci.get().getDataObject().setFileSize(storedDataFile.getFileSize());
                    ci.get().getDataObject().getUrls().clear();
                    ci.get().getDataObject().getUrls().addAll(storedDataFile.getUrls());
                    ci.get().getDataObject().setFilename(storedDataFile.getName());
                    associatedAIP.addEvent(EventType.STORAGE.name(),
                                           String.format(DATAFILE_STORED_SUCCESSFULLY, storedDataFile.getName()));
                }

                if (dataFileDao.countByAipAndStateNotIn(associatedAIP, Sets.newHashSet(DataFileState.STORED)) == 0) {
                    LOGGER.info("[STORE FILE SUCCESS] All files stored for AIP {}. The  AIP metadata can be handled.",
                                associatedAIP.getProviderId());
                    associatedAIP.setState(AIPState.DATAFILES_STORED);
                }

            }
            aipService.save(associatedAIP, false);
        } else {
            // AIP does not exists anymore. File
            String message = "AIP associated to fully stored Datafile does not exists.";
            throw new EntityNotFoundException(message);
        }
    }

    @Override
    public void handleStoreFailed(StorageDataFile storeFailFile, String failureCause, Long storageConfId) {
        Optional<AIP> optionalAssociatedAip = aipDao.findOneByAipId(storeFailFile.getAip().getId().toString());
        if (optionalAssociatedAip.isPresent()) {
            AIP associatedAIP = optionalAssociatedAip.get();
            // update data status
            storeFailFile.setState(DataFileState.ERROR);
            // reset notYetStoredBy to avoid issue during retry
            storeFailFile.resetNotYetStoredBy();
            storeFailFile.addFailureCause(failureCause);
            dataFileDao.save(storeFailFile);
            // Update associated AIP in db
            associatedAIP.setState(AIPState.STORAGE_ERROR);
            aipService.save(associatedAIP, false);
            PrioritizedDataStorage storageConf = null;
            try {
                storageConf = prioritizedDataStorageService.retrieve(storageConfId);
            } catch (fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException e) {
                LOGGER.error(String.format("StorageDataFile %s could not be stored on non existing DataStorage %s",
                                           storeFailFile.getId(),
                                           storageConfId), e);
            }
            String notifMsg = String
                    .format("**FileName**: %s %n" + "**AIP provider id**: %s %n" + "**DataStorage label**: %s %n"
                                    + "**Error**: %s",
                            storeFailFile.getName(),
                            storeFailFile.getAip().getProviderId(),
                            storageConf == null ? null : storageConf.getDataStorageConfiguration().getLabel(),
                            failureCause);
            notifyAdmins("Storage of file " + storeFailFile.getName() + " failed",
                         notifMsg,
                         NotificationLevel.INFO,
                         MimeTypeUtils.TEXT_PLAIN);
            publisher.publish(new AIPEvent(associatedAIP));
        } else {
            LOGGER.warn("AIP {} does not exists anymore. Associated file {} in store error status will be deleted",
                        storeFailFile.getName());
            dataFileDao.remove(storeFailFile);
        }
    }

    @Override
    public List<Map<String, Object>> getDiagnostics() {
        List<PluginConfiguration> dataStorageConfigurations = pluginService
                .getPluginConfigurationsByType(IDataStorage.class);
        // lets take only the activated ones
        Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toSet());
        List<Map<String, Object>> diagnostic = new ArrayList<>(activeDataStorageConfs.size());
        for (PluginConfiguration dataStorageConf : activeDataStorageConfs) {
            try {
                IDataStorage<?> activeDataStorage = pluginService.getPlugin(dataStorageConf.getId());
                Map<String, Object> diagInfo = activeDataStorage.getDiagnosticInfo();
                diagInfo.put("Plugin Conf", dataStorageConf);
                diagnostic.add(diagInfo);
            } catch (ModuleException e) {
                // We do not really care about issues here, we are getting diagnostic information so we probably
                // have access to logs to see issues
                LOGGER.error(e.getMessage(), e);
            }
        }
        return diagnostic;
    }
}
