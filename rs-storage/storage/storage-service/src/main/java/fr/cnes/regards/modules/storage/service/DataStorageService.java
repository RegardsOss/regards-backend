package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
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
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;

/**
 * Data storage service
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class DataStorageService implements IDataStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStorageInfo.class);

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
     * Metadata updated successfully message
     */
    public static final String METADATA_UPDATED_SUCCESSFULLY = "AIP metadata has been successfully updated";

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

    /**
     * Spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

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
        Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors
                .toMap(MonitoringAggregation::getDataStorageUsedId, MonitoringAggregation::getUsedSize));
        for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
            // lets initialize the monitoring information for this data storage configuration by getting plugin
            // informations
            Long activeDataStorageConfId = activeDataStorageConf.getId();
            PluginMetaData activeDataStorageMeta = pluginService
                    .getPluginMetaDataById(activeDataStorageConf.getPluginId());
            PluginStorageInfo monitoringInfo = new PluginStorageInfo(activeDataStorageConfId,
                    activeDataStorageMeta.getDescription(), activeDataStorageConf.getLabel());
            // now lets get the data storage monitoring information from the plugin
            @SuppressWarnings("rawtypes")
            Long dataStorageTotalSpace = ((IDataStorage) pluginService.getPlugin(activeDataStorageConfId))
                    .getTotalSpace();
            DataStorageInfo dataStorageInfo;
            if (monitoringAggregationMap.containsKey(activeDataStorageConfId)) {
                dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(), dataStorageTotalSpace,
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
            Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors
                    .toMap(MonitoringAggregation::getDataStorageUsedId, MonitoringAggregation::getUsedSize));
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
                                dataStorageTotalSpace, monitoringAggregationMap.get(activeDataStorageConfId));
                        Double ratio = dataStorageInfo.getRatio();
                        if (ratio >= criticalThreshold) {
                            String message = String.format(
                                                           "Data storage(configuration id: %s, configuration label: %s) has reach its "
                                                                   + "disk usage critical threshold. Actual occupation: %.2f%%, critical threshold: %s%%",
                                                           activeDataStorageConf.getId().toString(),
                                                           activeDataStorageConf.getLabel(), ratio, criticalThreshold);
                            LOGGER.error(message);
                            notifyAdmins("Data storage " + activeDataStorageConf.getLabel() + " is almost full",
                                         message, NotificationType.ERROR);
                            MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
                            return;
                        }
                        if (ratio >= threshold) {
                            String message = String.format(
                                                           "Data storage(configuration id: %s, configuration label: %s) has reach its "
                                                                   + "disk usage threshold. Actual occupation: %.2f%%, threshold: %s%%",
                                                           activeDataStorageConf.getId().toString(),
                                                           activeDataStorageConf.getLabel(), ratio, threshold);
                            LOGGER.warn(message);
                            notifyAdmins("Data storage " + activeDataStorageConf.getLabel() + " is almost full",
                                         message, NotificationType.WARNING);
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

    @PostConstruct
    public void init() {
        pluginService.addPluginPackage("fr.cnes.regards.modules.storage");
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    @Override
    public void notifyAdmins(String title, String message, NotificationType type) {
        FeignSecurityManager.asSystem();
        try {
            NotificationDTO notif = new NotificationDTO(message, Sets.newHashSet(),
                    Sets.newHashSet(DefaultRole.ADMIN.name()), applicationName, title, type);
            notificationClient.createNotification(notif);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void handleRestorationAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> data = dataFileDao.findOneById(event.getDataFileId());
        if (data.isPresent()) {
            Path restorationPath = event.getRestorationPath();
            switch (type) {
                case SUCCESSFULL:
                    cachedFileService.handleRestorationSuccess(data.get(), restorationPath);
                    break;
                case FAILED:
                    cachedFileService.handleRestorationFailure(data.get());
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
        Optional<StorageDataFile> data = dataFileDao.findLockedOneById(event.getDataFileId());
        if (data.isPresent()) {
            switch (type) {
                case SUCCESSFULL:
                    handleDeletionSuccess(data.get(), event.getHandledUrl(), event.getChecksum());
                    break;
                case FAILED:
                default:
                    // IDataStorage plugin used to delete the file is not able to delete the file right now.
                    // Maybe the file can be deleted later. So do nothing and just notify administrator.
                    String message = String.format("Error deleting file (id: %s, checksum: %s).", event.getDataFileId(),
                                                   event.getChecksum());
                    LOGGER.error(message);
                    notifyAdmins("File deletion error", message, NotificationType.INFO);
                    break;
            }
        } else {
            LOGGER.error("[DATAFILE DELETION EVENT] Invalid StorageDataFile deletion event. StorageDataFile does not exists in db for id {}",
                         event.getDataFileId());
        }
    }

    @Override
    public void handleDeletionSuccess(StorageDataFile dataFileDeleted, URL deletedUrl, String checksumOfDeletedFile) {
        // Get the associated AIP of the deleted StorageDataFile from db
        Optional<AIP> optionalAssociatedAIP = aipDao.findOneByAipId(dataFileDeleted.getAip().getId().toString());
        // Verify that deleted file checksum match StorageDataFile checksum
        if (optionalAssociatedAIP.isPresent() && dataFileDeleted.getChecksum().equals(checksumOfDeletedFile)) {
            AIP associatedAIP = optionalAssociatedAIP.get();
            // 1. Remove deleted file location from AIP.
            removeDeletedUrlFromDataFile(dataFileDeleted, deletedUrl, associatedAIP);
            if (DataType.AIP.equals(dataFileDeleted.getDataType())
                    && (!associatedAIP.getState().equals(AIPState.DELETED))) {
                // Do not delete the dataFileDeleted from db. At this time in db the file is the new one that has
                // been
                // stored previously to replace the deleted one. This is a special case for AIP metadata file
                // because,
                // at any time we want to ensure that there is only one StorageDataFile of AIP type for a given AIP.
                LOGGER.info("[DELETE FILE SUCCESS] AIP metadata file replaced.",
                            dataFileDeleted.getAip().getId().toString());
                associatedAIP.addEvent(EventType.UPDATE.name(), METADATA_UPDATED_SUCCESSFULLY);
                aipService.save(associatedAIP, false);
            }
        } else {
            LOGGER.warn("Deleted file checksum {}, does not match StorageDataFile {} checksum {}",
                        checksumOfDeletedFile, dataFileDeleted.getName(), dataFileDeleted.getChecksum());
        }
    }

    /**
     * Handle deletion of one location of the given {@link StorageDataFile} file.
     * @param dataFileDeleted {@link StorageDataFile}
     * @param urlToRemove location deleted.
     * @param associatedAIP {@link AIP} associated to the given {@link StorageDataFile}
     */
    private void removeDeletedUrlFromDataFile(StorageDataFile dataFileDeleted, URL urlToRemove, AIP associatedAIP) {
        if (dataFileDeleted.getUrls().isEmpty()) {
            LOGGER.info("Datafile to delete does not contains any location url. Deletion of the dataFile {}",
                        dataFileDeleted.getName());
            dataFileDao.remove(dataFileDeleted);
        }

        if (urlToRemove == null || dataFileDeleted.getUrls().contains(urlToRemove)) {
            if (urlToRemove == null || dataFileDeleted.getUrls().size() == 1) {
                // Get from the AIP all the content informations to remove. All content informations to remove are the
                // content informations with the same checksum that
                // the deleted StorageDataFile.
                // @formatter:off
                Set<ContentInformation> cisToRemove =
                        associatedAIP.getProperties().getContentInformations()
                            .stream()
                            .filter(ci -> dataFileDeleted.getChecksum().equals(ci.getDataObject().getChecksum()))
                            .collect(Collectors.toSet());
                // @formatter:on
                associatedAIP.getProperties().getContentInformations().removeAll(cisToRemove);
                associatedAIP.addEvent(EventType.DELETION.name(),
                                       String.format(DATAFILE_DELETED_SUCCESSFULLY, urlToRemove));
                associatedAIP = aipService.save(associatedAIP, false);
                LOGGER.debug("[DELETE FILE SUCCESS] AIP {} is in UPDATED state",
                             dataFileDeleted.getAip().getId().toString());
                LOGGER.debug("Deleted location {} is the only one location of the StorageDataFile {}. So we can completly remove the StorageDataFile.",
                             urlToRemove, dataFileDeleted.getName());
                dataFileDao.remove(dataFileDeleted);
            } else {
                LOGGER.info("Partial deletion of StorageDataFile {}. One of the location has been removed {}.",
                            dataFileDeleted.getName(), urlToRemove);
                associatedAIP.getProperties().getContentInformations().stream()
                        .filter(ci -> dataFileDeleted.getChecksum().equals(ci.getDataObject().getChecksum()))
                        .forEach(ci -> ci.getDataObject().getUrls().remove(urlToRemove));
                associatedAIP.addEvent(EventType.DELETION.name(),
                                       String.format(DATAFILE_DELETED_SUCCESSFULLY, urlToRemove));
                aipService.save(associatedAIP, false);
                dataFileDeleted.getUrls().remove(urlToRemove);
                dataFileDao.save(dataFileDeleted);
            }
        } else {
            LOGGER.warn("Removed URL {} is not associated to the StorageDataFile to delete {}", urlToRemove,
                        dataFileDeleted.getName());
        }

        // If associated AIP is not linked to any dataFile anymore, delete aip.
        if (dataFileDao.findAllByAip(associatedAIP).isEmpty()) {
            publisher.publish(new AIPEvent(associatedAIP));
            aipDao.remove(associatedAIP);
        }
    }

    @Override
    public void handleStoreAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> optionalData = dataFileDao.findLockedOneById(event.getDataFileId());
        if (optionalData.isPresent()) {
            StorageDataFile data = optionalData.get();
            Optional<AIP> optionalAssociatedAip = aipDao.findOneByAipId(data.getAip().getId().toString());
            if (optionalAssociatedAip.isPresent()) {
                AIP associatedAIP = optionalAssociatedAip.get();
                switch (type) {
                    case SUCCESSFULL:
                        handleStoreSuccess(data, event.getChecksum(), event.getHandledUrl(), event.getFileSize(),
                                           event.getStorageConfId(), event.getWidth(), event.getHeight(),
                                           associatedAIP);
                        break;
                    case FAILED:
                        handleStoreFailed(data, associatedAIP, event.getFailureCause());
                        break;
                    default:
                        LOGGER.error("Unhandle DataStorage STORE event type {}", type);
                        break;
                }
            } else {
                LOGGER.warn("[DATA STORAGE EVENT] StorageDataFile stored {} is not associated to an existing AIP",
                            event.getDataFileId());
            }
        } else {
            LOGGER.warn("[DATA STORAGE EVENT] StorageDataFile stored {} does not exists", event.getDataFileId());
        }
    }

    @Override
    public void handleStoreSuccess(StorageDataFile storedDataFile, String storedFileChecksum, URL storedFileNewURL,
            Long storedFileSize, Long dataStoragePluginConfId, Integer dataWidth, Integer dataHeight,
            AIP associatedAIP) {
        // update data status
        PrioritizedDataStorage prioritizedDataStorageUsed = null;
        try {
            prioritizedDataStorageUsed = prioritizedDataStorageService.retrieve(dataStoragePluginConfId);
        } catch (ModuleException e) {
            LOGGER.error("You shouldn't have this issue here! This means the plugin used to storeAndCreate the dataFile "
                    + "has just been removed from the application", e);
            return;
        }
        storedDataFile.setChecksum(storedFileChecksum);
        storedDataFile.setFileSize(storedFileSize);
        storedDataFile.addDataStorageUsed(prioritizedDataStorageUsed);
        storedDataFile.decreaseNotYetStoredBy();
        storedDataFile.getUrls().add(storedFileNewURL);
        storedDataFile.setHeight(dataHeight);
        storedDataFile.setWidth(dataWidth);
        LOGGER.debug("Datafile {} stored for pluginConfId:{} missing {} confs.", storedDataFile.getId(),
                     dataStoragePluginConfId, storedDataFile.getNotYetStoredBy());
        if (storedDataFile.getNotYetStoredBy().equals(0L)) {
            storedDataFile.setState(DataFileState.STORED);
            storedDataFile.emptyFailureCauses();
            // specific save once it is stored
            dataFileDao.save(storedDataFile);
            LOGGER.debug("[STORE FILE SUCCESS] DATA FILE {} is in STORED state", storedDataFile.getUrls());
            if (storedDataFile.getDataType() == DataType.AIP) {
                // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
                // if we just stored the AIP, there is nothing to do but changing AIP state, and clean the
                // workspace!
                // Lets clean the workspace
                try {
                    workspaceService.removeFromWorkspace(storedDataFile.getChecksum() + AIPService.JSON_FILE_EXT);
                } catch (IOException e) {
                    LOGGER.error("Error during workspace cleaning", e);
                }
                associatedAIP.setState(AIPState.STORED);
                associatedAIP.addEvent(EventType.STORAGE.name(), METADATA_STORED_SUCCESSFULLY);
                aipService.save(associatedAIP, false);
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
                    aipService.save(associatedAIP, false);
                }
            }
        } else {
            dataFileDao.save(storedDataFile);
            LOGGER.debug("Datafile not fully stored missing {} confs.", storedDataFile.getNotYetStoredBy());
        }
    }

    @Override
    public void handleStoreFailed(StorageDataFile storeFailFile, AIP associatedAIP, String failureCause) {
        // update data status
        storeFailFile.setState(DataFileState.ERROR);
        storeFailFile.addFailureCause(failureCause);
        dataFileDao.save(storeFailFile);
        // Update associated AIP in db
        associatedAIP.setState(AIPState.STORAGE_ERROR);
        aipService.save(associatedAIP, false);
        notifyAdmins("Storage of file " + storeFailFile.getChecksum() + " failed", failureCause, NotificationType.INFO);
        publisher.publish(new AIPEvent(associatedAIP));
    }
}
