package fr.cnes.regards.modules.storage.domain.event;

import java.net.URL;
import java.nio.file.Path;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * This is a subscribable event but not on a full broadcast mode: full broadcast means that it will be sent to EVERY
 * instance of microservices.
 * Here we need this event to be handled by only ONE instance of microservice.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.MICROSERVICE, mode = WorkerMode.UNICAST)
public class DataStorageEvent implements ISubscribable {

    private Long fileSize;

    private String checksum;

    private Long storageConfId;

    private Long dataFileId;

    private StorageAction storageAction;

    private StorageEventType type;

    private URL newUrl;

    private Path restorationPath;

    public DataStorageEvent() {
    }

    public DataStorageEvent(DataFile dataFile, StorageAction storageAction, StorageEventType type) {
        this.dataFileId = dataFile.getId();
        this.newUrl = dataFile.getUrl();
        this.storageAction = storageAction;
        this.type = type;
        this.storageConfId = dataFile.getDataStorageUsed().getId();
        this.fileSize = dataFile.getFileSize();
        this.checksum = dataFile.getChecksum();
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

    public Long getStorageConfId() {
        return storageConfId;
    }

    public void setStorageConfId(Long storageConfId) {
        this.storageConfId = storageConfId;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Path getRestorationPath() {
        return restorationPath;
    }

    public void setRestorationPath(Path restorationPath) {
        this.restorationPath = restorationPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        DataStorageEvent that = (DataStorageEvent) o;

        if (dataFileId != null ? !dataFileId.equals(that.dataFileId) : that.dataFileId != null) {
            return false;
        }
        if (storageAction != that.storageAction) {
            return false;
        }
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = dataFileId != null ? dataFileId.hashCode() : 0;
        result = (31 * result) + (storageAction != null ? storageAction.hashCode() : 0);
        result = (31 * result) + (type != null ? type.hashCode() : 0);
        return result;
    }
}
