/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * {@link IPoller} allows to poll {@link IPollable} events for a particular tenant.<br/>
 *
 * Before polling an event, you must tell the poller which tenant is concerned using {@link IPoller#bind(String)}
 * method. After that, poll your event using {@link IPoller#poll(String, Class)}. Finally, call
 * {@link IPoller#unbind(String)} to release tenant binding.<br/>
 *
 * To guaranty that a tenant is properly unbound, use a try-finally block.<br/>
 *
 * To poll an event in a {@link Transactional} method not to lose it, you have to bind and unbind tenant respectively
 * before and after the transaction.
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 *
 */
public interface IPoller {

    /**
     *
     * @param <T>
     *            {@link IPollable} event
     * @param pTenant
     *            tenant
     * @param pEvent
     *            {@link IPollable} event
     * @return {@link IPollable} event in a tenant wrapper
     */
    <T extends IPollable> TenantWrapper<T> poll(String pTenant, Class<T> pEvent);

    /**
     * Indicated the tenant to use to resolve vhost connection.
     *
     * @param pTenant
     *            tenant
     */
    void bind(String pTenant);

    /**
     * Unbind tenant connection resolution
     *
     */
    void unbind();
}