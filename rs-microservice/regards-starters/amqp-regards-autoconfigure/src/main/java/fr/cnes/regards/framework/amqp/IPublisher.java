/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * 
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
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException;

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
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority) throws RabbitMQVhostException;

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
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    <T> void publish(String pTenant, T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException;

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
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    <T> void publish(String pTenant, T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority) throws RabbitMQVhostException;

}