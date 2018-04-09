/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
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
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.StorageJobProgressManager;

/**
 * Handler for DataStorageEvent events. This events are sent by the {@link StorageJobProgressManager} associated
 * to the {@link IDataStorage} plugins. After each {@link StorageDataFile} stored, deleted or restored a
 * {@link DataStorageEvent}
 * should be sent thought the {@link StorageJobProgressManager}.
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Component
@RegardsTransactional
public class DataStorageEventHandler implements IHandler<DataStorageEvent>, IDataStorageEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataStorageEventHandler.class);

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
    private static final String METADATA_UPDATED_SUCCESSFULLY = "AIP metadata has been successfully updated";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * DAO to access {@link StorageDataFile} entities.
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * DAO to access {@link AIP} entities through the {@link AIPEntity} entities stored in db.
     */
    @Autowired
    private IAIPDao aipDao;

    /**
     * AMQP Publisher.
     */
    @Autowired
    private IPublisher publisher;

    @Autowired
    private ICachedFileService cachedFileService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ISubscriber subscriber;

    /**
     * The spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(DataStorageEvent.class, this);
    }

    /**
     * Dispatch actions to handle by {@link StorageAction}
     */
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
     * Handle {@link DataStorageEvent} events for {@link StorageAction#RESTORATION} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    private void handleRestorationAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> data = dataFileDao.findOneById(event.getDataFileId());
        if (data.isPresent()) {
            Path restorationPath = event.getRestorationPath();
            switch (type) {
                case SUCCESSFULL:
                    cachedFileService.handleRestorationSuccess(data.get(), restorationPath);
                    publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, data.get().getChecksum()));
                    break;
                case FAILED:
                    cachedFileService.handleRestorationFailure(data.get());
                    publisher.publish(new DataFileEvent(DataFileEventState.ERROR, data.get().getChecksum()));
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("[DATA STORAGE EVENT] restoration of non existing StorageDataFile id {}", event.getDataFileId());
        }
    }

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#DELETION} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    private void handleDeletionAction(StorageEventType type, DataStorageEvent event) {
        // Check that the given StorageDataFile id is associated to an existing StorageDataFile from db.
        Optional<StorageDataFile> data = dataFileDao.findOneById(event.getDataFileId());
        if (data.isPresent()) {
            switch (type) {
                case SUCCESSFULL:
                    handleDeletionSuccess(data.get(), event.getChecksum());
                    break;
                case FAILED:
                default:
                    // IDataStorage plugin used to delete the file is not able to delete the file right now.
                    // Maybe the file can be deleted later. So do nothing and just notify administrator.
                    String message = String.format("Error deleting file (id: %s, checksum: %s).", event.getDataFileId(),
                                                   event.getChecksum());
                    LOG.error(message);
                    notifyAdmins("File deletion error", message, NotificationType.INFO);
                    break;
            }
        } else {
            LOG.error("[DATAFILE DELETION EVENT] Invalid StorageDataFile deletion event. StorageDataFile does not exists in db for id {}",
                      event.getDataFileId());
        }
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationType type) {
        FeignSecurityManager.asSystem();
        try {
            NotificationDTO notif = new NotificationDTO(message, Sets.newHashSet(),
                    Sets.newHashSet(DefaultRole.ADMIN.name()), applicationName, title, type);
            notificationClient.createNotification(notif);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#DELETION} event is received.
     * @param dataFileDeleted {@link StorageDataFile} deleted.
     * @param checksumOfDeletedFile {@link String} checksum of the deleted {@link StorageDataFile}
     */
    private void handleDeletionSuccess(StorageDataFile dataFileDeleted, String checksumOfDeletedFile) {
        // Get the associated AIP of the deleted StorageDataFile from db
        Optional<AIP> optionalAssociatedAIP = aipDao.findOneByIpId(dataFileDeleted.getAip().getId().toString());
        if (optionalAssociatedAIP.isPresent() && dataFileDeleted.getChecksum().equals(checksumOfDeletedFile)) {
            AIP associatedAIP = optionalAssociatedAIP.get();
            // If deleted file is not an AIP metadata file
            // Set the AIP to UPDATED state.
            if (!DataType.AIP.equals(dataFileDeleted.getDataType())) {
                // Get from the AIP all the content informations to remove. All content informations to remove are the
                // content informations with the same checksum that
                // the deleted StorageDataFile.
                // @formatter:off
                Set<ContentInformation> cisToRemove =
                        associatedAIP.getProperties().getContentInformations()
                            .stream()
                            .filter(ci -> checksumOfDeletedFile.equals(ci.getDataObject().getChecksum()))
                            .collect(Collectors.toSet());
                // @formatter:on
                associatedAIP.getProperties().getContentInformations().removeAll(cisToRemove);
                associatedAIP.addEvent(EventType.DELETION.name(),
                                       String.format(DATAFILE_DELETED_SUCCESSFULLY, dataFileDeleted.getName()));
                aipDao.save(associatedAIP);
                LOG.debug("[DELETE FILE SUCCESS] AIP {} is in UPDATED state",
                          dataFileDeleted.getAip().getId().toString());
                dataFileDao.remove(dataFileDeleted);
                // Now that deletion of this data file is done, check if the aip metadata has been stored at least once
                Optional<StorageDataFile> metadataOpt = dataFileDao.findByAipAndType(associatedAIP, DataType.AIP);
                if (!metadataOpt.isPresent()) {
                    // If it has not been stored, lets check if any data file are still linked to it
                    Set<StorageDataFile> aipFiles = dataFileDao.findAllByAip(associatedAIP);
                    // If none are linked anymore, lets remove it now and publish the event for the rest of the world
                    if (aipFiles.isEmpty()) {
                        publisher.publish(new AIPEvent(associatedAIP));
                        aipDao.remove(associatedAIP);
                    }
                }
            } else {
                if (associatedAIP.getState() == AIPState.DELETED) {
                    // Deletion has been explicitly required, so lets remove all the dataFiles associated to this AIP...
                    dataFileDao.findAllByAip(associatedAIP).forEach(df -> dataFileDao.remove(df));
                    // ...and the aip itself
                    aipDao.remove(associatedAIP);
                    publisher.publish(new AIPEvent(associatedAIP));
                } else {
                    // Do not delete the dataFileDeleted from db. At this time in db the file is the new one that has
                    // been
                    // stored previously to replace the deleted one. This is a special case for AIP metadata file
                    // because,
                    // at any time we want to ensure that there is only one StorageDataFile of AIP type for a given AIP.
                    LOG.debug("[DELETE FILE SUCCESS] AIP metadata file replaced.",
                              dataFileDeleted.getAip().getId().toString());
                    associatedAIP.addEvent(EventType.UPDATE.name(), METADATA_UPDATED_SUCCESSFULLY);
                    // unless no other datafiles are linked to the metadata, in that case it means it
                }
            }
        }
    }

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#STORE} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    private void handleStoreAction(StorageEventType type, DataStorageEvent event) {
        Optional<StorageDataFile> optionalData = dataFileDao.findOneById(event.getDataFileId());
        if (optionalData.isPresent()) {
            StorageDataFile data = optionalData.get();
            Optional<AIP> optionalAssociatedAip = aipDao.findOneByIpId(data.getAip().getId().toString());
            if (optionalAssociatedAip.isPresent()) {
                AIP associatedAIP = optionalAssociatedAip.get();
                switch (type) {
                    case SUCCESSFULL:
                        handleStoreSuccess(data, event.getChecksum(), event.getNewUrl(), event.getFileSize(),
                                           event.getStorageConfId(), event.getWidth(), event.getHeight(),
                                           associatedAIP);
                        break;
                    case FAILED:
                        handleStoreFailed(data, associatedAIP, event.getFailureCause());
                        break;
                    default:
                        LOG.error("Unhandle DataStorage STORE event type {}", type);
                        break;
                }
            } else {
                LOG.warn("[DATA STORAGE EVENT] StorageDataFile stored {} is not associated to an existing AIP",
                         event.getDataFileId());
            }
        } else {
            LOG.warn("[DATA STORAGE EVENT] StorageDataFile stored {} does not exists", event.getDataFileId());
        }
    }

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storedDataFile {@link StorageDataFile} successfully stored
     * @param associatedAIP {@link AIP} associated to the given {@link StorageDataFile} successfully stored
     */
    private void handleStoreSuccess(StorageDataFile storedDataFile, String storedFileChecksum, URL storedFileNewURL,
            Long storedFileSize, Long dataStoragePluginConfId, Integer dataWidth, Integer dataHeight,
            AIP associatedAIP) {
        // update data status
        PrioritizedDataStorage prioritizedDataStorageUsed = null;
        try {
            prioritizedDataStorageUsed = prioritizedDataStorageService.retrieve(dataStoragePluginConfId);
        } catch (ModuleException e) {
            LOG.error("You shouldn't have this issue here! This means the plugin used to storeAndCreate the dataFile "
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
        if (storedDataFile.getNotYetStoredBy() == 0) {
            storedDataFile.setState(DataFileState.STORED);
            storedDataFile.emptyFailureCauses();
            // specific save once it is stored
            dataFileDao.save(storedDataFile);
            LOG.debug("[STORE FILE SUCCESS] DATA FILE {} is in STORED state", storedDataFile.getUrls());
            if (storedDataFile.getDataType() == DataType.AIP) {
                // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
                // if we just stored the AIP, there is nothing to do but changing AIP state, and clean the
                // workspace!
                // Lets clean the workspace
                try {
                    workspaceService.removeFromWorkspace(storedDataFile.getChecksum() + AIPService.JSON_FILE_EXT);
                } catch (IOException e) {
                    LOG.error("Error during workspace cleaning", e);
                }
                associatedAIP.setState(AIPState.STORED);
                associatedAIP.addEvent(EventType.STORAGE.name(), METADATA_STORED_SUCCESSFULLY);
                aipDao.save(associatedAIP);
                LOG.debug("[STORE FILE SUCCESS] AIP {} is in STORED state", storedDataFile.getAip().getId().toString());
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
                    associatedAIP.getProperties().getPdi().getProvenanceInformation()
                            .addEvent(EventType.STORAGE.name(),
                                      "File " + storedDataFile.getName() + " stored into REGARDS");
                    ci.get().getDataObject().setFileSize(storedDataFile.getFileSize());
                    ci.get().getDataObject().getUrls().addAll(storedDataFile.getUrls());
                    ci.get().getDataObject().setFilename(storedDataFile.getName());
                    associatedAIP.addEvent(EventType.STORAGE.name(),
                                           String.format(DATAFILE_STORED_SUCCESSFULLY, storedDataFile.getName()));
                    aipDao.save(associatedAIP);
                }
            }
        } else {
            dataFileDao.save(storedDataFile);
        }
    }

    /**
     * Method called when a FAILURE {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storeFailFile {@link StorageDataFile} not deleted.
     * @param associatedAIP {@link AIP} Associated to the {@link StorageDataFile} in error.
     * @param failureCause
     */
    private void handleStoreFailed(StorageDataFile storeFailFile, AIP associatedAIP, String failureCause) {
        // update data status
        storeFailFile.setState(DataFileState.ERROR);
        storeFailFile.addFailureCause(failureCause);
        dataFileDao.save(storeFailFile);
        // Update associated AIP in db
        associatedAIP.setState(AIPState.STORAGE_ERROR);
        aipDao.save(associatedAIP);
        notifyAdmins("Storage of file " + storeFailFile.getChecksum() + " failed", failureCause, NotificationType.INFO);
        publisher.publish(new AIPEvent(associatedAIP));
    }
}
