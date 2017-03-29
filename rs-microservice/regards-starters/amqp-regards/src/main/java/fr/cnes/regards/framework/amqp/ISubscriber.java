/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

/**
 * {@link ISubscriber} allows to subscribe to multitenant events.
 *
 * @author Marc Sordi
 *
 */
public interface ISubscriber extends ISubscriberContract {

    /**
     * Add new tenant listener
     *
     * @param tenant
     *            new tenant to manage
     */
    void addTenant(String tenant);

    /**
     * Remove tenant listener
     * 
     * @param tenant
     *            tenant to manage
     */
    void removeTenant(String tenant);
}
