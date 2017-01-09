/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.multitenant.IThreadTenantResolver;

/**
 * @author svissier
 * @author lmieulet
 * @author Marc Sordi
 *
 */
public class Publisher implements IPublisher {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    /**
     * bean allowing us to send message to the broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual host
     */
    private final IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * Resolve thread tenant
     */
    private final IThreadTenantResolver threadTenantResolver;

    public Publisher(final RabbitTemplate pRabbitTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostUtils pRabbitVirtualHostUtils, IThreadTenantResolver pThreadTenantResolver) {
        super();
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostUtils = pRabbitVirtualHostUtils;
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    public <T extends ISubscribable> void publish(T pEvent) {
        publish(threadTenantResolver.getTenant(), pEvent, AmqpCommunicationMode.ONE_TO_MANY,
                AmqpCommunicationTarget.EXTERNAL, 0);
    }

    @Override
    public <T extends IPollable> void publish(T pEvent) {
        publish(threadTenantResolver.getTenant(), pEvent, AmqpCommunicationMode.ONE_TO_ONE,
                AmqpCommunicationTarget.INTERNAL, 0);
    }

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
    @Override
    public final <T> void publish(final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget) {
        publish(pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget, 0);
    }

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
    @Override
    public final <T> void publish(final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget, final int pPriority) {

        String tenant = threadTenantResolver.getTenant();
        if (tenant != null) {
            publish(tenant, pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget, pPriority);
        } else {
            LOGGER.error("[AMQP Publisher] Unable to publish event {} because no tenant found.", pEvt.getClass());
        }
    }

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
    @Override
    public final <T> void publish(final String pTenant, final T pEvt,
            final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget) {
        publish(pTenant, pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget, 0);
    }

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
    @Override
    public final <T> void publish(final String pTenant, final T pEvt,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget,
            final int pPriority) {
        final Class<?> evtClass = pEvt.getClass();
        // add the Vhost corresponding to this tenant
        rabbitVirtualHostUtils.addVhost(pTenant);
        final Exchange exchange = regardsAmqpAdmin.declareExchange(evtClass, pAmqpCommunicationMode, pTenant,
                                                                   pAmqpCommunicationTarget);
        if (pAmqpCommunicationMode.equals(AmqpCommunicationMode.ONE_TO_ONE)) {
            final Queue queue = regardsAmqpAdmin.declareQueue(pEvt.getClass(), AmqpCommunicationMode.ONE_TO_ONE,
                                                              pTenant, pAmqpCommunicationTarget);
            regardsAmqpAdmin.declareBinding(queue, exchange, pAmqpCommunicationMode, pTenant);
        }
        final TenantWrapper<T> messageSended = new TenantWrapper<>(pEvt, pTenant);
        // bind the connection to the right vHost ie tenant to publish the message
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostUtils.getVhostName(pTenant));
        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchange.getName(), evtClass.getName(), messageSended, pMessage -> {
            final MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(pPriority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }
}
