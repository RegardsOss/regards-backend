package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * This is a subscribable event but not on a full broadcast mode: full broadcast means that it will be sent to EVERY instance of microservices.
 * Here we need this event to be handled by only ONE instance of microservice.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.MICROSERVICE)
public class DataStorageEvent implements ISubscribable {

    private DataFile dataFile;

    private StorageAction storageAction;

    private StorageEventType type;

    public DataStorageEvent() {
    }

    public DataStorageEvent(DataFile dataFile, StorageAction storageAction, StorageEventType type) {
        this.dataFile = dataFile;
        this.storageAction = storageAction;
        this.type = type;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public StorageAction getStorageAction() {
        return storageAction;
    }

    public void setStorageAction(StorageAction storageAction) {
        this.storageAction = storageAction;
    }

    public StorageEventType getType() {
        return type;
    }

    public void setType(StorageEventType type) {
        this.type = type;
    }
}
