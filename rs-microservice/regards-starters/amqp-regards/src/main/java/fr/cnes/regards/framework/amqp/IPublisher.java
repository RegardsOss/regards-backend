/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;

/**
 * Interface for publishing events
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisher {

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
    <T> void publish(String pTenant, T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority);

}