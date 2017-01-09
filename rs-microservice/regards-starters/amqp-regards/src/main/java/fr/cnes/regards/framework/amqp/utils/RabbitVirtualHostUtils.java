/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.utils;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.connection.RegardsSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.domain.RabbitMqVhostPermission;
import fr.cnes.regards.framework.amqp.domain.RabbitVhost;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * implementation compliant with RabbitMQ v3.6.5
 *
 * @author svissier
 *
 */
public class RabbitVirtualHostUtils implements IRabbitVirtualHostUtils {

    /**
     * :
     */
    private static final String COLON = ":";

    /**
     * \nSTATUS :
     */
    private static final String NEW_LINE_STATUS = "\nSTATUS :";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * namespace of REGARDS
     */
    private static final String REGARDS_NAMESPACE = "Regards.amqp.";

    /**
     * template used to perform REST request
     */
    private final RestTemplate restTemplate;

    /**
     * connection factory
     */
    private final RegardsSimpleRoutingConnectionFactory simpleRoutingConnectionFactory;

    /**
     * List of vhost already known
     */
    private List<String> vhostList;

    /**
     * username used to connect to the broker and it's manager
     */
    private final String rabbitmqUserName;

    /**
     * password used to connect to the broker and it's manager
     */
    private final String rabbitmqPassword;

    /**
     * value from the configuration file representing the host of the manager of the broker
     */
    private final String amqpManagementHost;

    /**
     * value from the configuration file representing the port on which the manager of the broker is listening
     */
    private final Integer amqpManagementPort;

    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     *
     * Constructor used to initialize properties from AmqpProperties
     *
     * @param pRabbitmqUserName
     *            user name
     * @param pRabbitmqPassword
     *            password
     * @param pAmqpManagementHost
     *            management host
     * @param pAmqpManagementPort
     *            management port
     * @param pRestTemplate
     *            client REST
     * @param pSimpleRoutingConnectionFactory
     *            connection factory to handle multi-tenancy
     */
    public RabbitVirtualHostUtils(String pRabbitmqUserName, String pRabbitmqPassword, String pAmqpManagementHost,
            Integer pAmqpManagementPort, RestTemplate pRestTemplate,
            RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory, RegardsAmqpAdmin pRegardsAmqpAdmin) {
        super();
        restTemplate = pRestTemplate;
        simpleRoutingConnectionFactory = pSimpleRoutingConnectionFactory;
        rabbitmqUserName = pRabbitmqUserName;
        rabbitmqPassword = pRabbitmqPassword;
        amqpManagementHost = pAmqpManagementHost;
        amqpManagementPort = pAmqpManagementPort;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
    }

    @Override
    public List<String> retrieveVhostList() {
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
        return vhostList;
    }

    @Override
    public String setBasic() {
        return "Basic " + encode(rabbitmqUserName, rabbitmqPassword);
    }

    @Override
    public String encode(String pRabbitmqUserName, String pRabbitmqPassword) {
        final String fullCredential = pRabbitmqUserName + COLON + pRabbitmqPassword;
        final byte[] plainCredsBytes = fullCredential.getBytes();
        return Base64.getEncoder().encodeToString(plainCredsBytes);
    }

    @Override
    public String getRabbitApiVhostEndpoint() {
        return getRabbitApiEndpoint() + "/vhosts";
    }

    @Override
    public String getRabbitApiEndpoint() {
        return "http" + COLON + "//" + amqpManagementHost + COLON + amqpManagementPort + "/api";
    }

    @Override
    public void addVhost(String pName, CachingConnectionFactory pConnectionFactory) throws RabbitMQVhostException {
        registerConnectionFactory(pName, pConnectionFactory);
        addVhost(pName);
    }

    /**
     * @param pName
     * @param pConnectionFactory
     */
    private void registerConnectionFactory(String pName, CachingConnectionFactory pConnectionFactory) {
        // if there is no registered connection factory for this vhost then register this one
        String registrationKey = getVhostName(pName);
        if (simpleRoutingConnectionFactory.getTargetConnectionFactory(registrationKey) == null) {
            simpleRoutingConnectionFactory.addTargetConnectionFactory(registrationKey, pConnectionFactory);
        }
    }

    @Override
    public void addVhost(String pName) throws RabbitMQVhostException {
        retrieveVhostList();
        final String fullyQualifiedVhostName = getVhostName(pName);
        if (!existVhost(fullyQualifiedVhostName)) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            // vhost on rabbitmq are defined by a PUT request on /vhosts/{name}
            final ResponseEntity<String> response = restTemplate
                    .exchange(getRabbitApiVhostEndpoint() + SLASH + fullyQualifiedVhostName, HttpMethod.PUT, request,
                              String.class);
            final int statusValue = response.getStatusCodeValue();
            if (!isSuccess(statusValue)) {
                throw new AddingRabbitMQVhostException(response.getBody() + NEW_LINE_STATUS + statusValue);
            }
            addPermissionToAccessVhost(pName);
            vhostList.add(fullyQualifiedVhostName);
        }
        registerConnectionFactory(pName, regardsAmqpAdmin.createConnectionFactory(pName));
    }

    @Override
    public void addPermissionToAccessVhost(String pVhost) throws AddingRabbitMQVhostPermissionException {
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

    @Override
    public String getRabbitApiPermissionVhostEndpoint(String pVhost) {
        return getRabbitApiEndpoint() + "/permissions/" + getVhostName(pVhost) + SLASH + rabbitmqUserName;
    }

    @Override
    public boolean isSuccess(int pStatusValue) {
        final int hundred = 100;
        return (pStatusValue / hundred) == 2;
    }

    @Override
    public boolean existVhost(String pName) {
        return vhostList.stream().filter(rVhName -> rVhName.equals(pName)).findAny().isPresent();
    }

    @Override
    public boolean brokerRunning() {
        boolean isRunning = true;
        try {
            retrieveVhostList();
        } catch (ResourceAccessException e) { // NOSONAR
            isRunning = false;
        }
        return isRunning;
    }

    /**
     * @param pTenant
     *            Tenant wanted
     * @return fully qualified name of vhost accord to namespace
     */
    public static String getVhostName(String pTenant) {
        return REGARDS_NAMESPACE + pTenant;
    }
}
