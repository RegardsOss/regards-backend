package fr.cnes.regards.modules.storagelight.domain.event;

import java.util.Collection;

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
@Event(target = Target.ALL)
public class FileReferenceEvent implements ISubscribable {

    @NotNull
    private String checksum;

    @NotNull
    private FileReferenceEventState state;

    private String message;

    private Collection<String> owners = Sets.newHashSet();

    private FileLocation location;

    public FileReferenceEvent(@NotNull String checksum, @NotNull FileReferenceEventState state,
            Collection<String> owners, String message, FileLocation location) {
        super();
        this.checksum = checksum;
        this.state = state;
        this.message = message;
        this.location = location;
        this.owners = owners;
    }

    public FileReferenceEvent(@NotNull String checksum, @NotNull FileReferenceEventState state,
            Collection<String> owners, String message) {
        super();
        this.checksum = checksum;
        this.state = state;
        this.message = message;
        this.owners = owners;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the state
     */
    public FileReferenceEventState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(FileReferenceEventState state) {
        this.state = state;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the location
     */
    public FileLocation getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(FileLocation location) {
        this.location = location;
    }

    /**
     * @return the owners
     */
    public Collection<String> getOwners() {
        return owners;
    }

}
