package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Events mainly for rs-order, gives information on StorageDataFile granularity, not AIP.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ALL)
public class DataFileEvent implements ISubscribable {

    /**
     * Data file state
     */
    private DataFileEventState state;

    /**
     * Data file checksum
     */
    private String checksum;

    /**
     * Default constructor
     */
    protected DataFileEvent() {
    }

    /**
     * Constructor setting the parameters as attributes
     * @param state
     * @param checksum
     */
    public DataFileEvent(DataFileEventState state, String checksum) {
        this.state = state;
        this.checksum = checksum;
    }

    /**
     * @return the state
     */
    public DataFileEventState getState() {
        return state;
    }

    /**
     * Set the state
     * @param state
     */
    public void setState(DataFileEventState state) {
        this.state = state;
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
}
