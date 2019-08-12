package fr.cnes.regards.modules.storagelight.domain.event;

import java.util.Collection;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;

/**
 * Bus message event sent when a {@link FileReference} is created, modified or deleted.
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
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
     * Business request identifier associated to the {@link FileReference}. Those identifiers are the identifier of file request.
     * See {@link StorageFlowItem}, {@link DeletionFlowItem} and {@link ReferenceFlowItem} for more information about
     * file requests.
     */
    private final Set<String> requestIds = Sets.newHashSet();

    /**
     * Build a file reference event with a file location.
     * @param checksum
     * @param type
     * @param owners
     * @param message
     * @param location
     * @param requestIds
     * @return {@link FileReferenceEvent}
     */
    public static FileReferenceEvent build(String checksum, FileReferenceEventType type, Collection<String> owners,
            String message, FileLocation location, Collection<String> requestIds) {
        Assert.notNull(checksum, "Checksum is mandatory");
        Assert.notNull(type, "Type is mandatory");
        Assert.notNull(requestIds, "Type is mandatory");

        FileReferenceEvent event = new FileReferenceEvent();
        event.checksum = checksum;
        event.type = type;
        event.message = message;
        event.location = location;
        event.owners = owners;
        event.requestIds.addAll(requestIds);
        return event;
    }

    /**
     * Build a file reference event without file location
     * @param checksum
     * @param type
     * @param owners
     * @param message
     * @param requestIds
     * @return {@link FileReferenceEvent}
     */
    public static FileReferenceEvent build(String checksum, FileReferenceEventType type, Collection<String> owners,
            String message, Collection<String> requestIds) {
        Assert.notNull(checksum, "Checksum is mandatory");
        Assert.notNull(type, "Type is mandatory");
        Assert.notNull(requestIds, "Type is mandatory");

        FileReferenceEvent event = new FileReferenceEvent();
        event.checksum = checksum;
        event.type = type;
        event.message = message;
        event.owners = owners;
        event.requestIds.addAll(requestIds);

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

    public Set<String> getRequestIds() {
        return requestIds;
    }

    @Override
    public String toString() {
        return "FileReferenceEvent [" + (checksum != null ? "checksum=" + checksum + ", " : "")
                + (type != null ? "type=" + type + ", " : "") + (message != null ? "message=" + message + ", " : "")
                + (owners != null ? "owners=" + owners + ", " : "")
                + (location != null ? "location=" + location + ", " : "")
                + (requestIds != null ? "requestIds=" + requestIds : "") + "]";
    }

}
