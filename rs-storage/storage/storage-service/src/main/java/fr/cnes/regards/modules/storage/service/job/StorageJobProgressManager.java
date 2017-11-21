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
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IProgressManager;

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
     * Does the process is in error ?
     */
    private boolean errorStatus = false;

    /**
     * List of {@link DataFile} in error during {@link IDataStorage} plugin action.
     */
    private final Collection<DataFile> failedDataFile = Sets.newConcurrentHashSet();

    private final Collection<DataFile> handledDataFile = Sets.newConcurrentHashSet();

    public StorageJobProgressManager(IPublisher publisher, IJob<?> job) {
        this.publisher = publisher;
        this.job = job;
    }

    @Override
    public void storageSucceed(DataFile dataFile, URL storedUrl, Long storedFileSize) {
        dataFile.setUrl(storedUrl);
        dataFile.setFileSize(storedFileSize);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.SUCCESSFULL);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        // hell yeah this is not the usual publish method, but i know what i'm doing so trust me!
        publisher.publish(dataStorageEvent);
    }

    @Override
    public void storageFailed(DataFile dataFile, String cause) {
        failureCauses.add(cause);
        errorStatus = true;
        failedDataFile.add(dataFile);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.FAILED);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent);
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    @Override
    public void deletionFailed(DataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.FAILED);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        failureCauses.add(failureCause);
        errorStatus = true;
        publisher.publish(dataStorageEvent);
    }

    @Override
    public void deletionSucceed(DataFile dataFile) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.SUCCESSFULL);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent);
    }

    @Override
    public void restoreSucceed(DataFile dataFile, Path restoredFilePath) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.SUCCESSFULL);
        dataStorageEvent.setRestorationPath(restoredFilePath);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent);
    }

    @Override
    public void restoreFailed(DataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.FAILED);
        failureCauses.add(failureCause);
        errorStatus = true;
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent);
    }

    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    public Collection<DataFile> getFailedDataFile() {
        return failedDataFile;
    }

    public Collection<DataFile> getHandledDataFile() {
        return handledDataFile;
    }
}
