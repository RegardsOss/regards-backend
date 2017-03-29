/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * {@link IPollerContract} allows to poll {@link IPollable} events for current tenant. This interface represents the
 * common poller contract whether we are in a multitenant or an instance context.
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 *
 */
@FunctionalInterface
public interface IPollerContract {

    /**
     *
     * @param <T>
     *            {@link IPollable} event
     * @param pEvent
     *            {@link IPollable} event
     * @return {@link IPollable} event in a tenant wrapper
     */
    <T extends IPollable> TenantWrapper<T> poll(Class<T> pEvent);
}
