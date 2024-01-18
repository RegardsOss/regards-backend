package fr.cnes.regards.modules.fileaccess.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * Bus message event sent when a FileReference is created, modified or deleted.
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FileReferenceEvent extends FileReferenceDto implements ISubscribable {

    /**
     * Event type
     */
    private FileReferenceEventType type;

    /**
     * Event message
     */
    private String message;

    public FileReferenceEvent(OffsetDateTime storageDate,
                              FileReferenceMetaInfoDto metaInfo,
                              FileLocationDto location,
                              Collection<String> owners,
                              Set<String> groupIds,
                              FileReferenceEventType type,
                              String message) {
        super(storageDate, metaInfo, location, owners);
        this.getGroupIds().addAll(groupIds);
        this.type = type;
        this.message = message;
    }

    public FileReferenceEvent(String checksum,
                              String originStorage,
                              FileReferenceEventType type,
                              Collection<String> owners,
                              String message,
                              FileLocationDto location,
                              FileReferenceMetaInfoDto metaInfo,
                              Collection<String> groupIds) {
        super(checksum, originStorage, metaInfo, location, owners, groupIds);
        this.type = type;
        this.message = message;

    }

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
                                           FileLocationDto location,
                                           FileReferenceMetaInfoDto metaInfo,
                                           Collection<String> groupIds) {
        return new FileReferenceEvent(checksum, originStorage, type, owners, message, location, metaInfo, groupIds);
    }

    public FileReferenceEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "FileReferenceEvent{" + "type=" + type + ", message='" + message + '\'' + '}';
    }
}
