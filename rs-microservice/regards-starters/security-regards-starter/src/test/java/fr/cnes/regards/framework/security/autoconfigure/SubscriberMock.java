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

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.amqp.ISubscriber#unsubscribeFrom(java.lang.Class)
     */
    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> pEvent) {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.framework.amqp.ISubscriber#addTenant(java.lang.String)
     */
    @Override
    public void addTenant(String pTenant) {
        // Nothing to do

    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.framework.amqp.ISubscriber#removeTenant(java.lang.String)
     */
    @Override
    public void removeTenant(String pTenant) {
        // Nothing to do

    }
}
