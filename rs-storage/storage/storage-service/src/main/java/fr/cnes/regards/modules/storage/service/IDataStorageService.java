package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;

/**
 * Contract that should be respected by the services handling {@link IDataStorage}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataStorageService {

    /**
     * Retrieve monitoring information on each and every one of IDataStorage plugin which are active.
     */
    Collection<PluginStorageInfo> getMonitoringInfos() throws ModuleException, IOException;

    /**
     * Periodically scheduled task that monitor {@link IDataStorage}s and notify users when a data storage reaches its disk usage threshold. This method go threw all active tenants.
     */
    void monitorDataStorages();

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#RESTORATION} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    void handleRestorationAction(StorageEventType type, DataStorageEvent event);

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#DELETION} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    void handleDeletionAction(StorageEventType type, DataStorageEvent event);

    /**
     * Use the notification module in admin to create a notification for admins
     */
    void notifyAdmins(String title, String message, NotificationType type);

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#DELETION} event is received.
     * @param dataFileDeleted {@link StorageDataFile} deleted.
     * @param checksumOfDeletedFile {@link String} checksum of the deleted {@link StorageDataFile}
     */
    void handleDeletionSuccess(StorageDataFile dataFileDeleted, URL deletedUrl, String checksumOfDeletedFile);

    /**
     * Handle {@link DataStorageEvent} events for {@link StorageAction#STORE} type.
     * @param type {@link StorageEventType}
     * @param event {@link DataStorageEvent}
     */
    void handleStoreAction(StorageEventType type, DataStorageEvent event);

    /**
     * Method called when a SUCCESSFULL {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storedDataFile {@link StorageDataFile} successfully stored
     */
    void handleStoreSuccess(StorageDataFile storedDataFile, String storedFileChecksum, URL storedFileNewURL,
            Long storedFileSize, Long dataStoragePluginConfId, Integer dataWidth, Integer dataHeight);

    /**
     * Method called when a FAILURE {@link DataStorageEvent} {@link StorageAction#STORE} event is received.
     * @param storeFailFile {@link StorageDataFile} not deleted.
     * @param failureCause
     * @param storageConfId
     */
    void handleStoreFailed(StorageDataFile storeFailFile, String failureCause, Long storageConfId);

    /**
     * @return all diagnostic information from all active {@link IDataStorage}s configuration
     */
    List<Map<String, Object>> getDiagnostics();
}
