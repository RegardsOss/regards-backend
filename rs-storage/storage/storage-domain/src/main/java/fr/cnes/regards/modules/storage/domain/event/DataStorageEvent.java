package fr.cnes.regards.modules.storage.domain.event;

import java.net.URL;

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

    private Long dataFileId;

    private StorageAction storageAction;

    private StorageEventType type;

    private URL newUrl;

    public DataStorageEvent() {
    }

    public DataStorageEvent(DataFile dataFile, StorageAction storageAction, StorageEventType type) {
        this.dataFileId = dataFile.getId();
        this.newUrl=dataFile.getOriginUrl();
        this.storageAction = storageAction;
        this.type = type;
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

    public Long getDataFileId() {
        return dataFileId;
    }

    public void setDataFileId(Long dataFileId) {
        this.dataFileId = dataFileId;
    }

    public URL getNewUrl() {
        return newUrl;
    }

    public void setNewUrl(URL newUrl) {
        this.newUrl = newUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DataStorageEvent that = (DataStorageEvent) o;

        if (dataFileId != null ? !dataFileId.equals(that.dataFileId) : that.dataFileId != null)
            return false;
        if (storageAction != that.storageAction)
            return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = dataFileId != null ? dataFileId.hashCode() : 0;
        result = 31 * result + (storageAction != null ? storageAction.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
