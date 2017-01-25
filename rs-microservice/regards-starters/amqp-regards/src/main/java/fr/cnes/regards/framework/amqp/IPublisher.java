/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * Interface for publishing events
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisher {

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param <T>
     *            {@link ISubscribable} event
     * @param pEvent
     *            {@link ISubscribable} event to publish
     */
    <T extends ISubscribable> void publish(T pEvent);

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param <T>
     *            {@link ISubscribable} event
     * @param pEvent
     *            {@link ISubscribable} event to publish
     * @param pPriority
     *            event priority
     */
    <T extends ISubscribable> void publish(T pEvent, int pPriority);

    /**
     * Publish an {@link IPollable} event
     *
     * @param <T>
     *            {@link IPollable} event
     * @param pEvent
     *            {@link IPollable} event to publish
     */
    <T extends IPollable> void publish(T pEvent);

    /**
     * Publish an {@link IPollable} event
     *
     * @param <T>
     *            {@link IPollable} event
     * @param pEvent
     *            {@link IPollable} event to publish
     * @param pPriority
     *            event priority
     */
    <T extends IPollable> void publish(T pEvent, int pPriority);
}