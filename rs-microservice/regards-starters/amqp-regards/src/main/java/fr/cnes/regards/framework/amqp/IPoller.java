/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * @author Marc Sordi
 *
 */
public interface IPoller {

    /**
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
     *
     * TODO: desactivate auto-ack and add an ack method
     *
     * @param <T>
     *            event published
     * @param pTenant
     *            tenant to poll message for
     * @param pEvt
     *            event class token
     * @param pAmqpCommunicationMode
     *            communication mode
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return received message from the broker
     */
    @Deprecated
    <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget);
}