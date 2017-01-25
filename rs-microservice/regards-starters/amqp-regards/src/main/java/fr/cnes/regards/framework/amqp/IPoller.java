/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * @author Marc Sordi
 *
 */
public interface IPoller {

    /**
     * TODO: desactivate auto-ack and add an ack method
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
}