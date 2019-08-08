package fr.cnes.regards.modules.storagelight.domain.event;

import java.util.Collection;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;

/**
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FileReferenceEvent implements ISubscribable {

    @NotNull
    private final String checksum;

    @NotNull
    private final FileReferenceEventState state;

    private final String message;

    private Collection<String> owners = Sets.newHashSet();

    private FileLocation location;

    private final Set<String> requestIds = Sets.newHashSet();

    public FileReferenceEvent(@NotNull String checksum, @NotNull FileReferenceEventState state,
            Collection<String> owners, String message, FileLocation location,
            @NotNull @NotEmpty Collection<String> requestIds) {
        super();
        this.checksum = checksum;
        this.state = state;
        this.message = message;
        this.location = location;
        this.owners = owners;
        this.requestIds.addAll(requestIds);
    }

    public FileReferenceEvent(@NotNull String checksum, @NotNull FileReferenceEventState state,
            Collection<String> owners, String message, @NotNull @NotEmpty Collection<String> requestIds) {
        super();
        this.checksum = checksum;
        this.state = state;
        this.message = message;
        this.owners = owners;
        this.requestIds.addAll(requestIds);
    }

    public String getChecksum() {
        return checksum;
    }

    public FileReferenceEventState getState() {
        return state;
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

}
