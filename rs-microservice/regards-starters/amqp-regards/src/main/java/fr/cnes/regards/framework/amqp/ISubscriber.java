/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 *
 * {@link ISubscriber} allows to subscribe to {@link ISubscribable} events.
 *
 * @author Sylvain Vissière-Guérinet
 * @author Sébastien Binda
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
@FunctionalInterface
public interface ISubscriber {

    /**
     * Subscribe to an {@link ISubscribable} event
     *
     * @param <T>
     *            {@link ISubscribable} event
     * @param pEvent
     *            {@link ISubscribable} event
     * @param pReceiver
     *            event {@link IHandler}
     */
    <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver);

}
