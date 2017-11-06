package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Events mainly for rs-order, gives information on DataFile granularity, not AIP.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ALL)
public class DataFileEvent implements ISubscribable {

    private DataFileEventState state;

    private String checksum;

    protected DataFileEvent() {
    }

    public DataFileEvent(DataFileEventState state, String checksum) {
        this.state = state;
        this.checksum = checksum;
    }

    public DataFileEventState getState() {
        return state;
    }

    public void setState(DataFileEventState state) {
        this.state = state;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
