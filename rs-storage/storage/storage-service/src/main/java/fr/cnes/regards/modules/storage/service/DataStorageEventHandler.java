/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.StorageJobProgressManager;

/**
 * Handler for DataStorageEvent events. This events are sent by the {@link StorageJobProgressManager} associated
 * to the {@link IDataStorage} plugins. After each {@link DataFile} stored, deleted or restored a {@link DataStorageEvent}
 * should be sent thought the {@link StorageJobProgressManager}.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Component
public class DataStorageEventHandler implements IHandler<DataStorageEvent> {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataStorageEventHandler.class);

    private static final String METADATA_STORED_SUCCESSFULLY = "AIP metadata has been successfully stored into REGARDS";

    private static final String DATAFILE_STORED_SUCCESSFULLY = "File %s has been successfully stored";

    private static final String DATAFILE_DELETED_SUCCESSFULLY = "File %s has been successfully deleted";

    private static final String METADATA_UPDATED_SUCCESSFULLY = "AIP metadata has been successfully updated";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * DAO to access {@link DataFile} entities.
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * DAO to access {@link AIP} entities through the {@link AIPDataBase} entities stored in db.
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

    /**
     * Service to retrieve and use Plugins more specificly the {@link IDataStorage} plugins.
     */
    @Autowired
    private PluginService pluginService;

    @Value("${regards.storage.workspace}")
    private String workspace;

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
        Optional<DataFile> data = dataFileDao.findOneById(event.getDataFileId());
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
            LOG.warn("[DATA STORAGE EVENT] restoration of non existing DataFile id {}", event.getDataFileId());
        }
    }

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#DELETION} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    private void handleDeletionAction(StorageEventType type, DataStorageEvent event) {
        // Check that the given DataFile id is associated to an existing DataFile from db.
        Optional<DataFile> data = dataFileDao.findOneById(event.getDataFileId());
        if (data.isPresent()) {
            switch (type) {
                case SUCCESSFULL:
                    handleDeletionSuccess(data.get(), event.getChecksum());
                    break;
                case FAILED:
                default:
                    // IDataStorage plugin used to delete the file is not able to delete the file right now.
                    // Maybe the file can be deleted later. So do nothing and just notify administrator.
                    LOG.error("Error deleting file {}", event.getDataFileId());
                    // TODO : Notofy deletion error.
                    break;
            }
        } else {
            LOG.error(
                    "[DATAFILE DELETION EVENT] Invalid DataFile deletion event. DataFile does not exists in db for id {}",
                    event.getDataFileId());
        }
    }

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#DELETION} event is received.
     * @param dataFileDeleted {@link DataFile} deleted.
     * @param checksumOfDeletedFile {@link String} checksum of the deleted {@link DataFile}
     */
    private void handleDeletionSuccess(DataFile dataFileDeleted, String checksumOfDeletedFile) {
        // Get the associated AIP of the deleted DataFile from db
        Optional<AIP> optionalAssociatedAIP = aipDao.findOneByIpId(dataFileDeleted.getAip().getId().toString());
        if (optionalAssociatedAIP.isPresent() && dataFileDeleted.getChecksum().equals(checksumOfDeletedFile)) {
            AIP associatedAIP = optionalAssociatedAIP.get();
            Optional<DataFile> metadataAIPFile = dataFileDao.findByAipAndType(associatedAIP, DataType.AIP);
            // If deleted file is not an AIP metadata file
            // Set the AIP to UPDATED state.
            if (!DataType.AIP.equals(dataFileDeleted.getDataType())) {
                // Get from the AIP all the content informations to remove. All content informations to remove are the content informations with the same checksum that
                // the deleted DataFile.
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
            } else {
                if (associatedAIP.getState() != AIPState.DELETED) {
                    // Do not delete the dataFileDeleted from db. At this time in db the file is the new one that has been
                    // stored previously to replace the deleted one. This is a special case for AIP metadata file because,
                    // at any time we want to ensure that there is only one DataFile of AIP type for a given AIP.
                    LOG.debug("[DELETE FILE SUCCESS] AIP metadata file replaced.",
                              dataFileDeleted.getAip().getId().toString());
                    associatedAIP.addEvent(EventType.UPDATE.name(), METADATA_UPDATED_SUCCESSFULLY);
                    // unless no other datafiles are linked to the metadata, in that case it means it
                } else {
                    // Deletion has been explicitly required, so lets remove all the dataFiles associated to this AIP...
                    dataFileDao.findAllByAip(associatedAIP).forEach(df -> dataFileDao.remove(df));
                    // ...and the aip itself
                    aipDao.remove(associatedAIP);
                    publisher.publish(new AIPEvent(associatedAIP));
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
        Optional<DataFile> optionalData = dataFileDao.findOneById(event.getDataFileId());
        if (optionalData.isPresent()) {
            DataFile data = optionalData.get();
            Optional<AIP> optionalAssociatedAip = aipDao.findOneByIpId(data.getAip().getId().toString());
            if (optionalAssociatedAip.isPresent()) {
                AIP associatedAIP = optionalAssociatedAip.get();
                switch (type) {
                    case SUCCESSFULL:
                        handleStoreSuccess(data, event.getChecksum(), event.getNewUrl(), event.getFileSize(),
                                           event.getStorageConfId(), associatedAIP);
                        break;
                    case FAILED:
                        handleStoreFailed(data, associatedAIP, event.getNewUrl());
                        break;
                    default:
                        LOG.error("Unhandle DataStorage STORE event type {}", type);
                        break;
                }
            } else {
                LOG.warn("[DATA STORAGE EVENT] DataFile stored {} is not associated to an existing AIP",
                         event.getDataFileId());
            }
        } else {
            LOG.warn("[DATA STORAGE EVENT] DataFile stored {} does not exists", event.getDataFileId());
        }
    }

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storedDataFile {@link DataFile} successfully stored
     * @param associatedAIP {@link AIP} associated to the given {@link DataFile} successfully stored
     */
    private void handleStoreSuccess(DataFile storedDataFile, String storedFileChecksum, URL storedFileNewURL,
            Long storedFileSize, Long dataStoragePluginConfId, AIP associatedAIP) {
        // update data status
        PluginConfiguration dataStorageUsed = null;
        try {
            dataStorageUsed = pluginService.getPluginConfiguration(dataStoragePluginConfId);
        } catch (ModuleException e) {
            LOG.error(
                    "You should not have this issue here! That means that the plugin used to store the dataFile just has been removed from the application",
                    e);
            // TODO : notify admin for invalid configuration.
            return;
        }
        storedDataFile.setChecksum(storedFileChecksum);
        storedDataFile.setFileSize(storedFileSize);
        storedDataFile.setDataStorageUsed(dataStorageUsed);
        storedDataFile.setState(DataFileState.STORED);
        storedDataFile.setUrl(storedFileNewURL);
        dataFileDao.save(storedDataFile);
        LOG.debug("[STORE FILE SUCCESS] DATA FILE {} is in STORED state", storedDataFile.getUrl());
        if (storedDataFile.getDataType() == DataType.AIP) {
            // can only be obtained after the aip state STORING_METADATA which can only changed to STORED
            // if we just stored the AIP, there is nothing to do but changing AIP state, and clean the
            // workspace!
            String dataFileName = storedDataFile.getChecksum() + AIPService.JSON_FILE_EXT;
            Path aipDataFileFromWorkspace = Paths.get(workspace, runtimeTenantResolver.getTenant(), dataFileName);
            if (aipDataFileFromWorkspace.toFile().exists()) {
                try {
                    Files.delete(aipDataFileFromWorkspace);
                } catch (IOException e) {
                    LOG.error("Error deleting temporary AIP metadata file from workspace {}", aipDataFileFromWorkspace,
                              e);
                }
            }
            associatedAIP.setState(AIPState.STORED);
            associatedAIP.addEvent(EventType.STORAGE.name(), METADATA_STORED_SUCCESSFULLY);
            aipDao.save(associatedAIP);
            LOG.debug("[STORE FILE SUCCESS] AIP {} is in STORED state", storedDataFile.getAip().getId().toString());
            publisher.publish(new AIPEvent(associatedAIP));
        } else {
            // if it is not the AIP metadata then the AIP metadata are not even scheduled for storage,
            // just let set the new information about this DataFile
            // @formatter:off
            Optional<ContentInformation> ci =
                    associatedAIP.getProperties().getContentInformations()
                        .stream()
                        .filter(contentInformation -> contentInformation.getDataObject().getChecksum().equals(storedDataFile.getChecksum()))
                        .findFirst();
            // @formatter:on
            if (ci.isPresent()) {
                associatedAIP.getProperties().getPdi().getProvenanceInformation().addEvent(EventType.STORAGE.name(),
                                                                                           "File " + storedDataFile
                                                                                                   .getName()
                                                                                                   + " stored into REGARDS");
                ci.get().getDataObject().setFileSize(storedDataFile.getFileSize());
                ci.get().getDataObject().setUrl(storedDataFile.getUrl());
                ci.get().getDataObject().setFilename(storedDataFile.getName());
                associatedAIP.addEvent(EventType.STORAGE.name(),
                                       String.format(DATAFILE_STORED_SUCCESSFULLY, storedDataFile.getName()));
                aipDao.save(associatedAIP);
            }
        }
    }

    /**
     * Method called when a FAILURE {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storeFailFile {@link DataFile} not deleted.
     * @param associatedAIP {@link AIP} Associated to the {@link DataFile} in error.
     * @param newUrl {@link URL} new URL of the {@link DataFile}
     */
    private void handleStoreFailed(DataFile storeFailFile, AIP associatedAIP, URL newUrl) {
        // update data status
        storeFailFile.setState(DataFileState.ERROR);
        storeFailFile.setUrl(newUrl);
        dataFileDao.save(storeFailFile);
        // Update associated AIP in db
        associatedAIP.setState(AIPState.STORAGE_ERROR);
        aipDao.save(associatedAIP);
        publisher.publish(new AIPEvent(associatedAIP));
    }
}
