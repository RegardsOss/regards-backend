/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;

/**
 *
 * Interface for message subscribing
 *
 * @author Sylvain Vissière-Guérinet
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@FunctionalInterface
public interface ISubscriber {

    /**
     *
     * Initialize any necessary container to listen to all tenant provided by the provider for the specified element
     *
     * @param <T>
     *            event type to which we subscribe
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event connection factory from context
     * @param pAmqpCommunicationMode
     *            {@link AmqpCommunicationMode}
     * @param pAmqpCommunicationTarget
     *            communication scope
     */
    <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget);

}
