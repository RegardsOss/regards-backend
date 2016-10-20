/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.connection.RegardsSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.RabbitMqVhostPermission;
import fr.cnes.regards.framework.amqp.domain.RabbitVhost;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author svissier
 *
 */
@Configuration
public class AmqpConfiguration {

    /**
     * \nSTATUS :
     */
    private static final String NEW_LINE_STATUS = "\nSTATUS :";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * :
     */
    private static final String COLON = ":";

    /**
     * instance IP
     */
    @Value("${server.address}")
    private String address;

    /**
     * instance Port
     */
    @Value("${server.port}")
    private int port;

    /**
     * username used to connect to the broker and it's manager
     */
    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUserName;

    @Value("${spring.rabbitmq.addresses}")
    private String rabbitAddresses;

    /**
     * password used to connect to the broker and it's manager
     */
    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    /**
     * value from the configuration file representing the host of the manager of the broker
     */
    @Value("${regards.amqp.management.host}")
    private String amqpManagementHost;

    /**
     * value from the configuration file representing the port on which the manager of the broker is listening
     */
    @Value("${regards.amqp.management.port}")
    private Integer amqpManagementPort;

    /**
     * template used to perform REST request
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * bean allowing us to declare queue, exchange, binding
     */
    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RegardsSimpleRoutingConnectionFactory simpleRoutingConnectionFactory;

    /**
     * List of vhost already known
     */
    private List<String> vhostList;

    /**
     *
     * GET Request to host/api/vhosts to know which Vhosts are already defined
     *
     *
     */
    public void retrieveVhostList() {
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RabbitVhost>> typeRef = new ParameterizedTypeReference<List<RabbitVhost>>() {

        };
        // CHECKSTYLE:ON
        final String host = getRabbitApiVhostEndpoint();
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        final ResponseEntity<List<RabbitVhost>> response = restTemplate.exchange(host, HttpMethod.GET, request,
                                                                                 typeRef);
        vhostList = response.getBody().stream().map(rVh -> rVh.getName()).collect(Collectors.toList());
    }

    /**
     * @return basic authentication to the broker
     */
    public String setBasic() {
        return "Basic " + encode(rabbitmqUserName, rabbitmqPassword);
    }

    /**
     * @param pRabbitmqUserName
     *            username
     * @param pRabbitmqPassword
     *            password
     * @return the encoded credential to give to the broker
     */
    private String encode(String pRabbitmqUserName, String pRabbitmqPassword) {
        final String fullCredential = pRabbitmqUserName + COLON + pRabbitmqPassword;
        final byte[] plainCredsBytes = fullCredential.getBytes();
        final byte[] base64CredsBytes = Base64.encode(plainCredsBytes);
        final String base64Creds = new String(base64CredsBytes);
        return base64Creds;
    }

    /**
     * @return complete url string representing rabbitMQ api endpoint for vhost
     */
    public String getRabbitApiVhostEndpoint() {
        return getRabbitApiEndpoint() + "/vhosts";
    }

    /**
     * @return parameterized url to /api of the broker
     */
    private String getRabbitApiEndpoint() {
        return "http://" + amqpManagementHost + COLON + amqpManagementPort + "/api";
    }

    /**
     *
     * PUT Request to /api/vhost/{name} to add this Vhost only if it is not already defined
     *
     * @param pName
     *            name of the Vhost you want to add
     * @param pConnectionFactory
     *            connection factory to which the virtual host should be bound to for further use
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public void addVhost(String pName, CachingConnectionFactory pConnectionFactory) throws RabbitMQVhostException {
        simpleRoutingConnectionFactory.addTargetConnectionFactory(pName, pConnectionFactory);
        addVhost(pName);
    }

    /**
     *
     * PUT Request to /api/vhost/{name} to add this Vhost only if it is not already defined
     *
     * @param pName
     *            name of the Vhost you want to add
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public void addVhost(String pName) throws RabbitMQVhostException {
        retrieveVhostList();
        if (!existVhost(pName)) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate.exchange(getRabbitApiVhostEndpoint() + SLASH + pName,
                                                                          HttpMethod.PUT, request, String.class);
            final int statusValue = response.getStatusCodeValue();
            if (!isSuccess(statusValue)) {
                throw new AddingRabbitMQVhostException(response.getBody() + NEW_LINE_STATUS + statusValue);
            }
            addPermissionToAccessVhost(pName);
            vhostList.add(pName);
        }
    }

    /**
     * @param pRabbitAddresses
     *            addresses from configuration file
     * @return {host, port}
     */
    private String[] parseRabbitAddresses(String pRabbitAddresses) {
        return rabbitAddresses.split(":");
    }

    public CachingConnectionFactory createConnectionFactory(String pVhost) {
        final String[] rabbitHostAndPort = parseRabbitAddresses(rabbitAddresses);
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHostAndPort[0],
                Integer.parseInt(rabbitHostAndPort[1]));
        connectionFactory.setVirtualHost(pVhost);
        return connectionFactory;
    }

    /**
     * @param pVhost
     *            vhost to add to our user permission
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     *
     */
    private void addPermissionToAccessVhost(String pVhost) throws AddingRabbitMQVhostPermissionException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, setBasic());
        final HttpEntity<RabbitMqVhostPermission> request = new HttpEntity<>(new RabbitMqVhostPermission(), headers);
        final ResponseEntity<String> response = restTemplate.exchange(getRabbitApiPermissionVhostEndpoint(pVhost),
                                                                      HttpMethod.PUT, request, String.class);
        final int statusValue = response.getStatusCodeValue();
        if (!isSuccess(statusValue)) {
            throw new AddingRabbitMQVhostPermissionException(response.getBody() + NEW_LINE_STATUS + statusValue);
        }

    }

    /**
     * @param pVhost
     *            vhost on which we want to add permission to our user
     * @return parameterized url to add permission on specified vhost to the user used to interact with the broker
     */
    private String getRabbitApiPermissionVhostEndpoint(String pVhost) {
        return getRabbitApiEndpoint() + "/permissions/" + pVhost + SLASH + rabbitmqUserName;
    }

    /**
     * @param pStatusValue
     *            status to examine
     * @return true if the code is 2xx
     */
    private boolean isSuccess(int pStatusValue) {
        final int hundred = 100;
        return (pStatusValue / hundred) == 2;
    }

    /**
     * @param pName
     *            name of the Vhost you want to check
     * @return true if the vhost is already known
     */
    public boolean existVhost(String pName) {
        return vhostList.stream().filter(rVhName -> rVhName.equals(pName)).findAny().isPresent();
    }

    /**
     * @return ip:port of microservice instance
     */
    public String getUniqueName() {
        return address + COLON + port;
    }

    public Exchange declareExchange(String pName, AmqpCommunicationMode pAmqpCommunicationMode, String pTenant) {
        final Exchange exchange = instantiateExchange(pName, pAmqpCommunicationMode);
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
     *            communication mode, used for naming conventing
     * @param pTenant
     *            tenant for who the queue is created
     * @return instance of the queue
     */
    public Queue declareQueue(Class<?> pEvtClass, AmqpCommunicationMode pAmqpCommunicationMode, String pTenant) {
        final Map<String, Object> args = new HashMap<>();
        final Integer maxPriority = 255;
        args.put("x-max-priority", maxPriority);
        final Queue queue = new Queue(getQueueName(pEvtClass, pAmqpCommunicationMode), true, false, false, args);
        SimpleResourceHolder.bind(rabbitAdmin.getRabbitTemplate().getConnectionFactory(), pTenant);
        rabbitAdmin.declareQueue(queue);
        SimpleResourceHolder.unbind(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
        return queue;
    }

    /**
     * @param pEvtClass
     *            event class token
     * @param pAmqpCommunicationMode
     *            communication mode
     * @return queue name according to communication mode
     */
    public String getQueueName(Class<?> pEvtClass, AmqpCommunicationMode pAmqpCommunicationMode) {
        final String queueName;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                queueName = pEvtClass.getName();
                break;
            case ONE_TO_MANY:
                queueName = getUniqueName();
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
        return queueName;
    }

    public Binding declareBinding(Queue pQueue, Exchange pExchange, String pBindingName,
            AmqpCommunicationMode pAmqpCommunicationMode, String pTenant) {
        final Binding binding = instantiateBinding(pQueue, pExchange, pBindingName, pAmqpCommunicationMode);
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
     * @param pBindingName
     *            binding name
     * @param pAmqpCommunicationMode
     *            communication mode
     * @return correct binding according to the communication mode
     */
    private Binding instantiateBinding(Queue pQueue, Exchange pExchange, String pBindingName,
            AmqpCommunicationMode pAmqpCommunicationMode) {
        final Binding binding;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                binding = BindingBuilder.bind(pQueue).to((DirectExchange) pExchange).with(pBindingName);
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
     * Instantiate the java object corresponding to an exchange
     *
     * @param pName
     *            name of exchange
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @return exchange type associate with the publish mode specified
     */
    public Exchange instantiateExchange(String pName, AmqpCommunicationMode pAmqpCommunicationMode) {
        final Exchange exchange;
        switch (pAmqpCommunicationMode) {
            case ONE_TO_ONE:
                exchange = new DirectExchange("REGARDS", true, false);
                break;
            case ONE_TO_MANY:
                exchange = new FanoutExchange(pName, true, false);
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
        return exchange;
    }

    /**
     * @return either the message broker is running or not
     */
    public boolean brokerRunning() {
        boolean isRunning = true;
        try {
            retrieveVhostList();
        } catch (ResourceAccessException e) {
            isRunning = false;
        }
        return isRunning;
    }

}
