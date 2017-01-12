/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribableEvent;

/**
 *
 * Interface for message subscribing
 *
 * @author Sylvain Vissière-Guérinet
 * @author Sébastien Binda
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public interface ISubscriber {

    /**
     * Subscribe to an {@link ISubscribableEvent} event
     *
     * @param <T>
     *            {@link ISubscribableEvent} event
     * @param pEvent
     *            {@link ISubscribableEvent} event
     * @param pReceiver
     *            event {@link IHandler}
     */
    <T extends ISubscribableEvent> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver);

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
    @Deprecated
    <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget);

}
