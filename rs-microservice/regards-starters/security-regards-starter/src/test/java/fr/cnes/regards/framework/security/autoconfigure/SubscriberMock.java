/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 *
 * Class SubscriberMock
 *
 * Test class to mock AMQP Subscriber
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class SubscriberMock implements ISubscriber {

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.framework.amqp.ISubscriber#subscribeTo(java.lang.Class,
     * fr.cnes.regards.framework.amqp.domain.IHandler)
     */
    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        // Nothing to do

    }

}
