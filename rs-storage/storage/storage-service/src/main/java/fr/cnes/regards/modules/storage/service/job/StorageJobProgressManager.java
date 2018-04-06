/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;

/**
 * Implementaion of {@link IProgressManager} used by {@link IDataStorage} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class StorageJobProgressManager implements IProgressManager {

    /**
     * Publisher to notify system of files events (stored, retrieved or deleted).
     */
    private final IPublisher publisher;

    /**
     * List of all failure causes throw by the {@link IDataStorage} plugins.
     */
    private final Set<String> failureCauses = Sets.newConcurrentHashSet();

    /**
     * Job associated to the current progress manager.
     */
    private final IJob<?> job;

    /**
     * Data storage configuration id which this progress manager is linked to
     */
    private final Long storageConfId;

    /**
     * Does the process is in error ?
     */
    private boolean errorStatus = false;

    /**
     * List of {@link StorageDataFile} in error during {@link IDataStorage} plugin action.
     */
    private final Collection<StorageDataFile> failedDataFile = Sets.newConcurrentHashSet();

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final String tenant;

    /**
     * Collection of data files that has been handled
     */
    private final Collection<StorageDataFile> handledDataFile = Sets.newConcurrentHashSet();

    public StorageJobProgressManager(IPublisher publisher, IJob<?> job, Long storageConfId,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.publisher = publisher;
        this.job = job;
        this.storageConfId = storageConfId;
        this.tenant = runtimeTenantResolver.getTenant();
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void storageSucceed(StorageDataFile dataFile, URL storedUrl, Long storedFileSize) {
        dataFile.setFileSize(storedFileSize);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.SUCCESSFULL, storageConfId, storedUrl);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        // hell yeah this is not the usual publish method, but i know what i'm doing so trust me!
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void storageFailed(StorageDataFile dataFile, String cause) {
        failureCauses.add(cause);
        errorStatus = true;
        failedDataFile.add(dataFile);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE, StorageEventType.FAILED,
                storageConfId);
        dataStorageEvent.setFailureCause(cause);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    @Override
    public void deletionFailed(StorageDataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.FAILED, storageConfId);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        failureCauses.add(failureCause);
        errorStatus = true;
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void deletionSucceed(StorageDataFile dataFile) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.SUCCESSFULL, storageConfId);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void restoreSucceed(StorageDataFile dataFile, Path restoredFilePath) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.SUCCESSFULL, storageConfId);
        dataStorageEvent.setRestorationPath(restoredFilePath);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void restoreFailed(StorageDataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.FAILED, storageConfId);
        failureCauses.add(failureCause);
        errorStatus = true;
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    private void publishWithTenant(DataStorageEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
        runtimeTenantResolver.clearTenant();
    }

    /**
     * @return the failure causes
     */
    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    /**
     * @return the failed data files
     */
    public Collection<StorageDataFile> getFailedDataFile() {
        return failedDataFile;
    }

    /**
     * @return the handled data files
     */
    public Collection<StorageDataFile> getHandledDataFile() {
        return handledDataFile;
    }
}
