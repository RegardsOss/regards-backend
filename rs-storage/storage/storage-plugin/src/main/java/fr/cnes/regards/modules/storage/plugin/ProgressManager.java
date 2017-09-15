package fr.cnes.regards.modules.storage.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;

public class ProgressManager {

    private final IPublisher publisher;

    private final Set<String> failureCauses = Sets.newHashSet();

    private final IJob job;

    private boolean errorStatus = false;

    private final Collection<DataFile> failedDataFile = Sets.newHashSet();

    public ProgressManager(IPublisher publisher, IJob job) {
        this.publisher = publisher;
        this.job = job;
    }

    public void storageSucceed(DataFile dataFile, URL storedUrl) {
        dataFile.setUrl(storedUrl);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.SUCCESSFUL);
        job.advanceCompletion();
        //hell yeah this is not the usual publish method, but i know what i'm doing to trust me!
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public void storageFailed(DataFile dataFile, String cause) {
        failureCauses.add(cause);
        errorStatus = true;
        failedDataFile.add(dataFile);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.FAILED);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    public void deletionFailed(DataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.FAILED);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
        //FIXME: set failure into the failureCauses or into another set?
    }

    public void deletionSucceed(DataFile dataFile) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.SUCCESSFUL);
        job.advanceCompletion();
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public void restoreSucceed(DataFile dataFile, Path restoredFilePath) {

    }

    public void restoreFailed(DataFile dataFile) {

    }

    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    public Collection<DataFile> getFailedDataFile() {
        return failedDataFile;
    }
}
