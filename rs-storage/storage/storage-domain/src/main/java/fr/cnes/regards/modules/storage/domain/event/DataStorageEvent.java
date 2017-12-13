package fr.cnes.regards.modules.storage.domain.event;

import java.net.URL;
import java.nio.file.Path;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * This is a subscribable event but not on a full broadcast mode: full broadcast means that it will be sent to EVERY
 * instance of microservices.
 * Here we need this event to be handled by only ONE instance of microservice.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.MICROSERVICE, mode = WorkerMode.UNICAST)
public class DataStorageEvent implements ISubscribable {

    /**
     * The data file size
     */
    private Long fileSize;

    /**
     * The data file checksum
     */
    private String checksum;

    /**
     * the data storage plugin configuration id
     */
    private Long storageConfId;

    /**
     * The data file id
     */
    private Long dataFileId;

    /**
     * The storage action
     */
    private StorageAction storageAction;

    /**
     * The storage event type
     */
    private StorageEventType type;

    /**
     * The data file new url
     */
    private URL newUrl;

    /**
     * The data file restoration path
     */
    private Path restorationPath;

    /**
     * Default constructor
     */
    public DataStorageEvent() {
    }

    /**
     * Constructor initializing the event from the parameters
     * @param dataFile
     * @param storageAction
     * @param type
     */
    public DataStorageEvent(StorageDataFile dataFile, StorageAction storageAction, StorageEventType type) {
        this.dataFileId = dataFile.getId();
        this.newUrl = dataFile.getUrl();
        this.storageAction = storageAction;
        this.type = type;
        this.storageConfId = dataFile.getDataStorageUsed().getId();
        this.fileSize = dataFile.getFileSize();
        this.checksum = dataFile.getChecksum();
    }

    /**
     * @return the storage action
     */
    public StorageAction getStorageAction() {
        return storageAction;
    }

    /**
     * Set the storage action
     * @param storageAction
     */
    public void setStorageAction(StorageAction storageAction) {
        this.storageAction = storageAction;
    }

    /**
     * @return the event type
     */
    public StorageEventType getType() {
        return type;
    }

    /**
     * Set the event type
     * @param type
     */
    public void setType(StorageEventType type) {
        this.type = type;
    }

    /**
     * @return the data file id
     */
    public Long getDataFileId() {
        return dataFileId;
    }

    /**
     * Set the data file id
     * @param dataFileId
     */
    public void setDataFileId(Long dataFileId) {
        this.dataFileId = dataFileId;
    }

    /**
     * @return the new url
     */
    public URL getNewUrl() {
        return newUrl;
    }

    /**
     * Set the new url
     * @param newUrl
     */
    public void setNewUrl(URL newUrl) {
        this.newUrl = newUrl;
    }

    /**
     * @return the data storage plugin configuration id
     */
    public Long getStorageConfId() {
        return storageConfId;
    }

    /**
     * Set the data storage plugin configuration id
     * @param storageConfId
     */
    public void setStorageConfId(Long storageConfId) {
        this.storageConfId = storageConfId;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     * @param fileSize
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     * @param checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the restoration path
     */
    public Path getRestorationPath() {
        return restorationPath;
    }

    /**
     * Set the restoration path
     * @param restorationPath
     */
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
