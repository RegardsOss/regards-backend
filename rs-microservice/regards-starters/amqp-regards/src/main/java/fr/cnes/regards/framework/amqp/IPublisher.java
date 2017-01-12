/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.event.IPollableEvent;
import fr.cnes.regards.framework.amqp.event.ISubscribableEvent;

/**
 * Interface for publishing events
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisher {

    /**
     * Publish an {@link ISubscribableEvent} event
     *
     * @param <T>
     *            {@link ISubscribableEvent} event
     * @param pEvent
     *            {@link ISubscribableEvent} event to publish
     */
    <T extends ISubscribableEvent> void publish(T pEvent);

    /**
     * Publish an {@link IPollableEvent} event
     *
     * @param <T>
     *            {@link IPollableEvent} event
     * @param pEvent
     *            {@link IPollableEvent} event to publish
     */
    <T extends IPollableEvent> void publish(T pEvent);

    /**
     * @param <T>
     *            event to be published
     * @param pEvt
     *            the event you want to publish
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            publishing scope
     */
    @Deprecated
    <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget);

    /**
     * @param <T>
     *            event to be published
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            publishing scope
     */
    @Deprecated
    <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority);

    /**
     * @param <T>
     *            event to be published
     * @param pTenant
     *            the tenant name
     * @param pEvt
     *            the event you want to publish
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            publishing scope
     */
    @Deprecated
    <T> void publish(String pTenant, T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget);

    /**
     * @param <T>
     *            event to be published
     * @param pTenant
     *            the tenant name
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            publishing scope
     */
    @Deprecated
    <T> void publish(String pTenant, T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority);

}