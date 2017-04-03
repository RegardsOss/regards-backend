/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * {@link IPublisherContract} allows to publish {@link ISubscribable} or {@link IPollable} events. This interface
 * represents the common publisher contract whether we are in a multitenant or an instance context.
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisherContract {

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param pEvent
     *            {@link ISubscribable} event to publish
     */
    void publish(ISubscribable pEvent);

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param pEvent
     *            {@link ISubscribable} event to publish
     * @param pPriority
     *            event priority
     */
    void publish(ISubscribable pEvent, int pPriority);

    /**
     * Publish an {@link IPollable} event
     *
     * @param pEvent
     *            {@link IPollable} event to publish
     */
    void publish(IPollable pEvent);

    /**
     * Publish an {@link IPollable} event
     *
     * @param pEvent
     *            {@link IPollable} event to publish
     * @param pPriority
     *            event priority
     */
    void publish(IPollable pEvent, int pPriority);
}
