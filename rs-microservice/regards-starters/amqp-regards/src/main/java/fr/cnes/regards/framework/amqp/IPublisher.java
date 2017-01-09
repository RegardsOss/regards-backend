/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
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
     * Publish an {@link IPollable} event
     *
     * @param <T>
     *            {@link IPollable} event
     * @param pEvent
     *            {@link IPollable} event to publish
     */
    <T extends IPollable> void publish(T pEvent);

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