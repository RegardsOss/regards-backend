package fr.cnes.regards.modules.storage.plugin;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;

public class ProgressManager {

    private IPublisher publisher;

    private Set<String> failureCauses = Sets.newHashSet();

    private boolean errorStatus = false;

    private Collection<DataFile> failedDataFile = Sets.newHashSet();

    public ProgressManager(IPublisher publisher) {
        this.publisher = publisher;
    }

    public void storageSucceed(DataFile dataFile, URL storedUrl) {
        dataFile.setOriginUrl(storedUrl);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE, StorageEventType.SUCCESSFUL);
        //hell yeah this is not the usual publish method, but i know what i'm doing to trust me!
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public void storageFailed(DataFile dataFile, String cause) {
        failureCauses.add(cause);
        errorStatus = true;
        failedDataFile.add(dataFile);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE, StorageEventType.FAILED);
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    public void deletionFailed(DataFile dataFile, String failureCause) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION, StorageEventType.FAILED);
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
        //FIXME: set failure into the failureCauses or into another set?
    }

    public void deletionSucceed(DataFile dataFile) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION, StorageEventType.SUCCESSFUL);
        publisher.publish(dataStorageEvent, WorkerMode.SINGLE, Target.MICROSERVICE, 0);
    }

    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    public Collection<DataFile> getFailedDataFile() {
        return failedDataFile;
    }
}
