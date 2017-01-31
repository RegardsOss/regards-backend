/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

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
    private String microserviceInstanceId;

    public RegardsAmqpAdmin(String pMicroserviceTypeId) {
        super();
        microserviceTypeId = pMicroserviceTypeId;
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

        LOGGER.info("Declaring exchange for : tenant {} / event {} / target {} / mode {}", pTenant,
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
                builder.append(microserviceTypeId);
                builder.append(UNDERSCORE);
                break;
            default:
                throw new EnumConstantNotPresentException(Target.class, pTarget.name());
        }

        builder.append(pName);
        return builder.toString();
    }

    /**
     *
     * Declare a queue that can handle 255 priority
     *
     * @param pTenant
     *            tenant for which the queue is created
     * @param pEventType
     *            class token corresponding to the message types the queue will receive, used for naming convention
     * @param pWorkerMode
     *            {@link WorkerMode} used for naming convention
     * @param pTarget
     *            {@link Target} used for naming convention
     * @return instance of the queue
     */
    public Queue declareQueue(String pTenant, Class<?> pEventType, WorkerMode pWorkerMode, Target pTarget) {

        LOGGER.info("Declaring queue for : tenant {} / event {} / target {} / mode {}", pTenant, pEventType.getName(),
                    pTarget, pWorkerMode);

        // Create queue
        final Map<String, Object> args = new HashMap<>();
        final Integer maxPriority = 255;
        args.put("x-max-priority", maxPriority);
        Queue queue = new Queue(getQueueName(pEventType, pWorkerMode, pTarget), true, false, false, args);

        rabbitAdmin.declareQueue(queue);

        return queue;
    }

    /**
     * Computing queue name according to {@link WorkerMode} and {@link Target}
     *
     * @param pEvtClass
     *            event type
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @param pTarget
     *            {@link Target}
     * @return queue name according to {@link WorkerMode} and {@link Target}
     */
    public String getQueueName(Class<?> pEvtClass, WorkerMode pWorkerMode, Target pTarget) {
        StringBuilder builder = new StringBuilder();

        // Prefix queue with microservice id if target restricted to microservice
        switch (pTarget) {
            case MICROSERVICE:
                builder.append(microserviceTypeId).append(UNDERSCORE);
                break;
            case ALL:
                // No prefix cause no target restriction
                break;
            default:
                throw new EnumConstantNotPresentException(Target.class, pTarget.name());
        }

        switch (pWorkerMode) {
            case SINGLE:
                builder.append(pEvtClass.getName());
                break;
            case ALL:
                builder.append(pEvtClass.getName());
                builder.append(UNDERSCORE);
                builder.append(getMicroserviceInstanceId());
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

        LOGGER.info("Declaring binding for : tenant {} / queue {} / exchange {} / mode {}", pTenant, pQueue.getName(),
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

    /**
     * Use a random UUID to identify instance at runtime
     *
     * @return instance identifier
     */
    public String getMicroserviceInstanceId() {
        if (microserviceInstanceId == null) {
            microserviceInstanceId = UUID.randomUUID().toString();
        }
        return microserviceInstanceId;
    }

    /**
     * Bind {@link ConnectionFactory} to tenant (and vhost) before declaring an AMQP element
     *
     * @param pTenant
     *            tenant to bind
     */
    public void bind(String pTenant) {
        SimpleResourceHolder.bind(rabbitAdmin.getRabbitTemplate().getConnectionFactory(),
                                  RabbitVirtualHostAdmin.getVhostName(pTenant));
    }

    /**
     * Unbind {@link ConnectionFactory} from tenant (and vhost
     *
     */
    public void unbind() {
        SimpleResourceHolder.unbind(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
    }

}
