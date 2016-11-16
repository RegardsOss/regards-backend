/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;

/**
 * @author svissier
 *
 */
public class RegardsAmqpAdmin {

    /**
     * _
     */
    private static final String UNDERSCORE = "_";

    /**
     * :
     */
    private static final String COLON = ":";

    /**
     * bean allowing us to declare queue, exchange, binding
     */
    @Autowired
    private RabbitAdmin rabbitAdmin;

    /**
     * type identifier
     */
    private final String typeIdentifier;

    /**
     * instance identifier
     */
    private final String instanceIdentifier;

    /**
     * addresses configured to
     */
    private final String rabbitAddresses;

    public RegardsAmqpAdmin(String pTypeIdentifier, String pInstanceIdentifier, String pRabbitAddresses) {
        super();
        typeIdentifier = pTypeIdentifier;
        instanceIdentifier = pInstanceIdentifier;
        rabbitAddresses = pRabbitAddresses;
    }

    /**
     * @param pRabbitAddresses
     *            addresses from configuration file
     * @return {host, port}
     */
    protected String[] parseRabbitAddresses(String pRabbitAddresses) {
        return pRabbitAddresses.split(COLON);
    }

    public CachingConnectionFactory createConnectionFactory(String pVhost) {
        final String[] rabbitHostAndPort = parseRabbitAddresses(rabbitAddresses);
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHostAndPort[0],
                Integer.parseInt(rabbitHostAndPort[1]));
        connectionFactory.setVirtualHost(pVhost);
        return connectionFactory;
    }

    /**
     * @return ip:port of microservice instance
     */
    public String getUniqueName() {
        return instanceIdentifier;
    }

    public Exchange declareExchange(Class<?> pEvt, AmqpCommunicationMode pAmqpCommunicationMode, String pTenant,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final Exchange exchange = instantiateExchange(pEvt.getName(), pAmqpCommunicationMode,
                                                      pAmqpCommunicationTarget);
        SimpleResourceHolder.bind(rabbitAdmin.getRabbitTemplate().getConnectionFactory(), pTenant);
        rabbitAdmin.declareExchange(exchange);
        SimpleResourceHolder.unbind(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
        return exchange;
    }

    /**
     *
     * Declare a queue that can handle 255 priority
     *
     * @param pEvtClass
     *            class token corresponding to the message types the queue will receive, used for naming convention
     * @param pAmqpCommunicationMode
     *            communication mode, used for naming convention
     * @param pAmqpCommunicationTarget
     *            communication target, used for naming convention
     * @param pTenant
     *            tenant for who the queue is created
     * @return instance of the queue
     */
    public Queue declareQueue(Class<?> pEvtClass, AmqpCommunicationMode pAmqpCommunicationMode, String pTenant,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final Queue queue = instanciateQueue(pEvtClass, pAmqpCommunicationMode, pAmqpCommunicationTarget);
        SimpleResourceHolder.bind(rabbitAdmin.getRabbitTemplate().getConnectionFactory(), pTenant);
        rabbitAdmin.declareQueue(queue);
        SimpleResourceHolder.unbind(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
        return queue;
    }

    /**
     * @param pEvtClass
     *            class token corresponding to the message types the queue will receive, used for naming convention
     * @param pAmqpCommunicationMode
     *            communication mode, used for naming convention
     * @param pAmqpCommunicationTarget
     *            communication target, used for naming convention
     * @return instance of the queue
     */
    protected Queue instanciateQueue(Class<?> pEvtClass, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final Map<String, Object> args = new HashMap<>();
        final Integer maxPriority = 255;
        args.put("x-max-priority", maxPriority);
        return new Queue(getQueueName(pEvtClass, pAmqpCommunicationMode, pAmqpCommunicationTarget), true, false, false,
                args);

    }

    /**
     * @param pEvtClass
     *            event class token
     * @param pAmqpCommunicationMode
     *            communication mode
     * @param pAmqpCommunicationTarget
     *            scope of message origin
     * @return queue name according to communication mode and target
     */
    public String getQueueName(Class<?> pEvtClass, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final String queueName;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                queueName = getQueueNameOneToOne(pEvtClass, pAmqpCommunicationTarget);
                break;
            case ONE_TO_MANY:
                queueName = getQueueNameOneToMany(pEvtClass, pAmqpCommunicationTarget);
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
        return queueName;
    }

    /**
     *
     * @param pEvtClass
     *            event class token
     * @param pAmqpCommunicationTarget
     *            scope of message origin
     * @return queue name according to communication target for mode ONE_TO_MANY
     */
    protected String getQueueNameOneToMany(Class<?> pEvtClass, AmqpCommunicationTarget pAmqpCommunicationTarget) {
        return getQueueNamePrefix(pAmqpCommunicationTarget) + pEvtClass.getName() + UNDERSCORE + getUniqueName();
    }

    /**
     *
     * @param pEvtClass
     *            event class token
     * @param pAmqpCommunicationTarget
     *            scope of message origin
     * @return queue name according to communication target for mode ONE_TO_ONE
     */
    protected String getQueueNameOneToOne(Class<?> pEvtClass, AmqpCommunicationTarget pAmqpCommunicationTarget) {
        return getQueueNamePrefix(pAmqpCommunicationTarget) + pEvtClass.getName();
    }

    /**
     *
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return queue name prefix according to communication target
     */
    protected String getQueueNamePrefix(AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final String queueNamePrefix;
        switch (pAmqpCommunicationTarget) {
            case INTERNAL:
                queueNamePrefix = typeIdentifier + UNDERSCORE;
                break;
            case EXTERNAL:
                queueNamePrefix = "";
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationTarget.class,
                        pAmqpCommunicationTarget.name());
        }
        return queueNamePrefix;
    }

    public Binding declareBinding(Queue pQueue, Exchange pExchange, AmqpCommunicationMode pAmqpCommunicationMode,
            String pTenant) {
        final Binding binding = instantiateBinding(pQueue, pExchange, pAmqpCommunicationMode);
        SimpleResourceHolder.bind(rabbitAdmin.getRabbitTemplate().getConnectionFactory(), pTenant);
        rabbitAdmin.declareBinding(binding);
        SimpleResourceHolder.unbind(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
        return binding;
    }

    /**
     * @param pQueue
     *            queue instance
     * @param pExchange
     *            exchange instance
     * @param pAmqpCommunicationMode
     *            communication mode
     * @return correct binding according to the communication mode
     */
    protected Binding instantiateBinding(Queue pQueue, Exchange pExchange,
            AmqpCommunicationMode pAmqpCommunicationMode) {
        final Binding binding;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                binding = BindingBuilder.bind(pQueue).to((DirectExchange) pExchange)
                        .with(getRoutingKey(pQueue.getName(), AmqpCommunicationMode.ONE_TO_ONE));
                break;
            case ONE_TO_MANY:
                binding = BindingBuilder.bind(pQueue).to((FanoutExchange) pExchange);
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }

        return binding;
    }

    /**
     *
     * @param pQueueName
     *            queue name
     * @param pAmqpCommunicationMode
     *            communication target
     * @return routing key
     */
    public String getRoutingKey(String pQueueName, AmqpCommunicationMode pAmqpCommunicationMode) {
        final String routingKey;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                routingKey = pQueueName;
                break;
            case ONE_TO_MANY:
                routingKey = "";
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
        return routingKey;
    }

    /**
     *
     * Instantiate the java object corresponding to an exchange
     *
     * @param pName
     *            name of exchange
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return exchange type associate with the publish mode specified
     */
    protected Exchange instantiateExchange(String pName, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final Exchange exchange;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                exchange = new DirectExchange(getExchangeName("REGARDS", pAmqpCommunicationTarget), true, false);
                break;
            case ONE_TO_MANY:
                exchange = new FanoutExchange(getExchangeName(pName, pAmqpCommunicationTarget), true, false);
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
        return exchange;
    }

    /**
     * @param pName
     *            base name
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return prefixed name according to communication target
     */
    public String getExchangeName(String pName, AmqpCommunicationTarget pAmqpCommunicationTarget) {
        return getExchangeNamePrefix(pAmqpCommunicationTarget) + pName;
    }

    /**
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return prefix according to the communication target
     */
    protected String getExchangeNamePrefix(AmqpCommunicationTarget pAmqpCommunicationTarget) {
        final String exchangeNamePrefix;
        switch (pAmqpCommunicationTarget) {
            case EXTERNAL:
                exchangeNamePrefix = "";
                break;
            case INTERNAL:
                exchangeNamePrefix = typeIdentifier + UNDERSCORE;
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationTarget.class,
                        pAmqpCommunicationTarget.name());
        }
        return exchangeNamePrefix;
    }

}
