/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 *
 * Common publisher methods
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractPublisher implements IPublisherContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPublisher.class);

    /**
     * Default routing key
     */
    private static final String DEFAULT_ROUTING_KEY = "";

    /**
     * bean allowing us to send message to the broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public AbstractPublisher(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        this.rabbitTemplate = pRabbitTemplate;
        this.regardsAmqpAdmin = pRegardsAmqpAdmin;
        this.rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    public void publish(ISubscribable pEvent) {
        publish(pEvent, 0);
    }

    @Override
    public void publish(ISubscribable pEvent, int pPriority) {
        Class<?> eventClass = pEvent.getClass();
        publish(pEvent, WorkerMode.ALL, EventUtils.getCommunicationTarget(eventClass), pPriority);
    }

    @Override
    public void publish(IPollable pEvent) {
        publish(pEvent, 0);
    }

    @Override
    public void publish(IPollable pEvent, int pPriority) {
        Class<?> eventClass = pEvent.getClass();
        publish(pEvent, WorkerMode.SINGLE, EventUtils.getCommunicationTarget(eventClass), pPriority);
    }

    /**
     * @param <T>
     *            event to be published
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pWorkerMode
     *            publishing mode
     * @param pTarget
     *            publishing scope
     */
    public final <T> void publish(final T pEvt, final WorkerMode pWorkerMode, final Target pTarget,
            final int pPriority) {

        LOGGER.debug("Publishing event {} (Target : {}, WorkerMode : {} )", pEvt.getClass(), pTarget, pWorkerMode);

        String tenant = resolveTenant();
        if (tenant != null) {
            publish(tenant, pEvt, pWorkerMode, pTarget, pPriority);
        } else {
            LOGGER.error("[AMQP Publisher] Unable to publish event {} because no tenant found.", pEvt.getClass());
        }
    }

    /**
     * @return the tenant on which we have to publish the event
     */
    protected abstract String resolveTenant();

    /**
     * @param <T>
     *            event to be published
     * @param pTenant
     *            the tenant name
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pWorkerMode
     *            publishing mode
     * @param pTarget
     *            publishing scope
     */
    protected final <T> void publish(final String pTenant, final T pEvt, final WorkerMode pWorkerMode,
            final Target pTarget, final int pPriority) {

        final Class<?> evtClass = pEvt.getClass();

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(pTenant);

            // Declare exchange
            Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, evtClass, pWorkerMode, pTarget);

            if (WorkerMode.SINGLE.equals(pWorkerMode)) {
                // Direct exchange needs a specific queue, a binding between this queue and exchange containing a
                // specific routing key
                Queue queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt.getClass(), WorkerMode.SINGLE, pTarget);
                regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);
                publishMessageByTenant(pTenant, exchange.getName(),
                                       regardsAmqpAdmin.getRoutingKey(queue.getName(), pWorkerMode), pEvt, pPriority);
            } else if (WorkerMode.ALL.equals(pWorkerMode)) {
                // Routing key useless ... always skipped with a fanout exchange
                publishMessageByTenant(pTenant, exchange.getName(), DEFAULT_ROUTING_KEY, pEvt, pPriority);
            } else {
                String errorMessage = String.format("Unexpected communication mode : %s.", pWorkerMode);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    /**
     * Publish event in tenant virtual host
     *
     * @param <T>
     *            event type
     * @param pTenant
     *            tenant
     * @param pExchangeName
     *            {@link Exchange} name
     * @param pRoutingKey
     *            routing key (really useful for direct exchange). Use {@link Publisher#DEFAULT_ROUTING_KEY} for fanout.
     * @param pEvt
     *            the event to publish
     * @param pPriority
     *            the event priority
     */
    private final <T> void publishMessageByTenant(final String pTenant, String pExchangeName, String pRoutingKey,
            final T pEvt, final int pPriority) {

        // Message to publish
        final TenantWrapper<T> messageSended = new TenantWrapper<>(pEvt, pTenant);

        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(pExchangeName, pRoutingKey, messageSended, pMessage -> {
            final MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(pPriority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
    }
}
