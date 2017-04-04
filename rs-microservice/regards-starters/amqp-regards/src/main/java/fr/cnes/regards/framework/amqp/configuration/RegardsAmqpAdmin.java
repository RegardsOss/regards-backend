/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * This class manage tenant AMQP administration. Each tenant is hosted in an AMQP virtual host.<br/>
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class RegardsAmqpAdmin {

    /**
     * Default exchange name
     */
    public static final String DEFAULT_EXCHANGE_NAME = "regards";

    /**
     * This constant allows to defined a message to send with a high priority
     */
    public static final Integer MAX_PRIORITY = 255;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsAmqpAdmin.class);

    /**
     * _
     */
    private static final String UNDERSCORE = "_";

    /**
     * Bean allowing us to declare queue, exchange, binding
     */
    @Autowired
    private RabbitAdmin rabbitAdmin;

    /**
     * Microservice type identifier
     */
    private final String microserviceTypeId;

    /**
     * Microservice instance identifier
     */
    private final String microserviceInstanceId;

    public RegardsAmqpAdmin(String pMicroserviceTypeId, String pMicroserviceInstanceId) {
        super();
        microserviceTypeId = pMicroserviceTypeId;
        microserviceInstanceId = pMicroserviceInstanceId;
    }

    /**
     * Declare an exchange for each event so we use its name to instantiate it.
     *
     *
     * @param pTenant
     *            tenant
     * @param pEventType
     *            event type
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @param pTarget
     *            {@link Target}
     * @return a new {@link Exchange} related to current tenant and event
     */
    public Exchange declareExchange(String pTenant, Class<?> pEventType, WorkerMode pWorkerMode, Target pTarget) {

        LOGGER.debug("Declaring exchange for : tenant {} / event {} / target {} / mode {}", pTenant,
                     pEventType.getName(), pTarget, pWorkerMode);

        Exchange exchange;
        switch (pWorkerMode) {
            case SINGLE:
                exchange = new DirectExchange(getExchangeName(DEFAULT_EXCHANGE_NAME, pTarget), true, false);
                break;
            case ALL:
                exchange = new FanoutExchange(getExchangeName(pEventType.getName(), pTarget), true, false);
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, pWorkerMode.name());
        }

        rabbitAdmin.declareExchange(exchange);
        return exchange;
    }

    /**
     *
     * Build exchange name
     *
     * @param pName
     *            base name
     * @param pTarget
     *            {@link Target}
     * @return prefixed name according to communication target
     */
    public String getExchangeName(String pName, Target pTarget) {
        StringBuilder builder = new StringBuilder();

        // Prefix exchange with microservice id to create a dedicated exchange
        switch (pTarget) {
            case ALL:
                // No prefix cause no target restriction
                break;
            case MICROSERVICE:
                builder.append(microserviceTypeId).append(UNDERSCORE);
                break;
            case INSTANCE:
                builder.append(microserviceTypeId).append(UNDERSCORE).append(microserviceInstanceId);
                break;
            default:
                throw new EnumConstantNotPresentException(Target.class, pTarget.name());
        }

        builder.append(pName);
        return builder.toString();
    }

    /**
     *
     * Declare a queue that can handle priority {@link RegardsAmqpAdmin}{@link #MAX_PRIORITY}
     *
     * @param pTenant
     *            tenant for which the queue is created
     * @param pEventType
     *            class token corresponding to the message types the queue will receive, used for naming convention
     * @param pWorkerMode
     *            {@link WorkerMode} used for naming convention
     * @param pTarget
     *            {@link Target} used for naming convention
     * @return instance of queue
     */
    public Queue declareQueue(String pTenant, Class<?> pEventType, WorkerMode pWorkerMode, Target pTarget) {

        LOGGER.debug("Declaring queue for : tenant {} / event {} / target {} / mode {}", pTenant, pEventType.getName(),
                     pTarget, pWorkerMode);

        // Create queue
        final Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", MAX_PRIORITY);
        Queue queue = new Queue(getQueueName(pEventType, pWorkerMode, pTarget), true, false, false, args);

        rabbitAdmin.declareQueue(queue);

        return queue;
    }

    /**
     * Declare a queue by event handler. Only useful for publish/subscribe mode.
     *
     * @param pTenant
     *            tenant
     * @param pEventHandler
     *            event handler
     * @param pWorkerMode
     *            {@link WorkerMode} used for naming convention
     * @param pTarget
     *            {@link Target} used for naming convention
     * @return instance of queue
     */
    public Queue declareSubscribeQueue(String pTenant, Class<?> pEventHandler, WorkerMode pWorkerMode, Target pTarget) {

        LOGGER.debug("Declaring queue for : tenant {} / handler {} / target {} / mode {}", pTenant,
                     pEventHandler.getName(), pTarget, pWorkerMode);

        // Create queue
        final Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", MAX_PRIORITY);
        Queue queue = new Queue(getQueueName(pEventHandler, pWorkerMode, pTarget), true, false, false, args);

        rabbitAdmin.declareQueue(queue);

        return queue;
    }

    /**
     * Purge the queue that manages the specified event
     *
     * @param pType
     *            handler type for subscribable events, else event type
     * @param noWait
     *            true to not await completion of the purge
     */
    public void purgeQueue(Class<?> pType, boolean noWait) {
        WorkerMode mode;
        if (ISubscribable.class.isAssignableFrom(pType)) {
            mode = WorkerMode.ALL;
        } else if (IPollable.class.isAssignableFrom(pType)) {
            mode = WorkerMode.SINGLE;
        } else {
            throw new UnsupportedOperationException();
        }
        rabbitAdmin.purgeQueue(getQueueName(pType, mode, EventUtils.getCommunicationTarget(pType)), noWait);
    }

    /**
     * Computing queue name according to {@link WorkerMode}, {@link Target} and a discrimator type.
     *
     * @param pType
     *            type
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @param pTarget
     *            {@link Target}
     * @return queue name according to {@link WorkerMode} and {@link Target}
     */
    public String getQueueName(Class<?> pType, WorkerMode pWorkerMode, Target pTarget) {
        StringBuilder builder = new StringBuilder();

        // Prefix queue with microservice id if target restricted to microservice
        switch (pTarget) {
            case ALL:
                // No prefix cause no target restriction
                break;
            case MICROSERVICE:
                builder.append(microserviceTypeId).append(UNDERSCORE);
                break;
            case INSTANCE:
                builder.append(microserviceTypeId).append(UNDERSCORE).append(microserviceInstanceId);
                break;
            default:
                throw new EnumConstantNotPresentException(Target.class, pTarget.name());
        }

        switch (pWorkerMode) {
            case SINGLE:
                builder.append(pType.getName());
                break;
            case ALL:
                builder.append(pType.getName());
                builder.append(UNDERSCORE);
                builder.append(microserviceInstanceId);
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, pWorkerMode.name());
        }

        return builder.toString();
    }

    /**
     *
     * Declare binding to link {@link Queue} and {@link Exchange}
     *
     * @param pTenant
     *            tenant
     * @param pQueue
     *            {@link Queue} to bind
     * @param pExchange
     *            {@link Exchange} to bind
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @return {@link Binding} between {@link Queue} and {@link Exchange}
     */
    public Binding declareBinding(String pTenant, Queue pQueue, Exchange pExchange, WorkerMode pWorkerMode) {

        LOGGER.debug("Declaring binding for : tenant {} / queue {} / exchange {} / mode {}", pTenant, pQueue.getName(),
                     pExchange.getName(), pWorkerMode);

        Binding binding;
        switch (pWorkerMode) {
            case SINGLE:
                binding = BindingBuilder.bind(pQueue).to((DirectExchange) pExchange)
                        .with(getRoutingKey(pQueue.getName(), WorkerMode.SINGLE));
                break;
            case ALL:
                binding = BindingBuilder.bind(pQueue).to((FanoutExchange) pExchange);
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, pWorkerMode.name());
        }

        rabbitAdmin.declareBinding(binding);
        return binding;
    }

    /**
     *
     * @param pQueueName
     *            queue name
     * @param pWorkerMode
     *            communication target
     * @return routing key
     */
    public String getRoutingKey(String pQueueName, WorkerMode pWorkerMode) {
        final String routingKey;
        switch (pWorkerMode) {
            case SINGLE:
                routingKey = pQueueName;
                break;
            case ALL:
                routingKey = "";
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, pWorkerMode.name());
        }
        return routingKey;
    }

    public String getMicroserviceTypeId() {
        return microserviceTypeId;
    }

    public String getMicroserviceInstanceId() {
        return microserviceInstanceId;
    }

}
