package fr.cnes.regards.framework.amqp.test.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event for deserialization failure tests.
 * This must not extends AbstractEvent and have a different body (ie. message2 in place of message).
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ALL)
public class ErrorEvent implements ISubscribable {

    private long long1 = 132;

    public long getLong1() {
        return long1;
    }

    public void setLong1(long long1) {
        this.long1 = long1;
    }
}
