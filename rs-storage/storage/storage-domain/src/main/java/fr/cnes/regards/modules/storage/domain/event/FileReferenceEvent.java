package fr.cnes.regards.modules.storage.domain.event;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Set;

/**
 * Bus message event sent when a {@link FileReference} is created, modified or deleted.
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FileReferenceEvent implements ISubscribable {

    /**
     * Checksum of the {@link FileReference}
     */
    private String checksum;

    /**
     * Event type
     */
    private FileReferenceEventType type;

    /**
     * Event message
     */
    private String message;

    /**
     * Owners of the {@link FileReference}
     */
    private Collection<String> owners = Sets.newHashSet();

    /**
     * location of the {@link FileReference}
     */
    private FileLocation location;

    /**
     * Meta information of the {@link FileReference}
     */
    private FileReferenceMetaInfo metaInfo;

    /**
     * Origine storage of the file requested
     */
    private String originStorage;

    /**
     * Business request identifier associated to the {@link FileReference}. Those identifiers are the identifier of file request.
     * See {@link StorageFlowItem}, {@link DeletionFlowItem} and {@link ReferenceFlowItem} for more information about
     * file requests.
     */
    private final Set<String> groupIds = Sets.newHashSet();

    /**
     * Build a file reference event with a file location.
     *
     * @return {@link FileReferenceEvent}
     */
    public static FileReferenceEvent build(String checksum,
                                           String originStorage,
                                           FileReferenceEventType type,
                                           Collection<String> owners,
                                           String message,
                                           FileLocation location,
                                           FileReferenceMetaInfo metaInfo,
                                           Collection<String> groupIds) {
        Assert.notNull(checksum, "Checksum is mandatory");
        Assert.notNull(type, "Type is mandatory");
        Assert.notNull(groupIds, "GroupIds is mandatory");

        FileReferenceEvent event = new FileReferenceEvent();
        event.checksum = checksum;
        event.type = type;
        event.message = message;
        event.location = location;
        event.owners = owners;
        event.groupIds.addAll(groupIds);
        event.metaInfo = metaInfo;
        event.originStorage = originStorage;
        return event;
    }

    public String getChecksum() {
        return checksum;
    }

    public FileReferenceEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public FileLocation getLocation() {
        return location;
    }

    public Collection<String> getOwners() {
        return owners;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public String getOriginStorage() {
        return originStorage;
    }

    public void setOriginStorage(String originStorage) {
        this.originStorage = originStorage;
    }

    @Override
    public String toString() {
        String cs = (checksum != null ? "checksum=" + checksum + ", " : "");
        String t = (type != null ? "type=" + type + ", " : "");
        String m = (message != null ? "message=" + message + ", " : "");
        String ow = (owners != null ? "owners=" + owners + ", " : "");
        String loc = (location != null ? "location=" + location + ", " : "");
        String gids = (groupIds != null ? "groupIds=" + groupIds : "");
        return "FileReferenceEvent [" + cs + t + m + ow + loc + gids + "]";
    }

}
