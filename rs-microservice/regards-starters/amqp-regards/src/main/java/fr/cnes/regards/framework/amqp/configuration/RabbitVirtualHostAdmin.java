/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.domain.RabbitMqVhostPermission;
import fr.cnes.regards.framework.amqp.domain.RabbitVhost;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RemovingRabbitMQVhostException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * implementation compliant with RabbitMQ v3.6.5
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class RabbitVirtualHostAdmin implements IRabbitVirtualHostAdmin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitVirtualHostAdmin.class);

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
    private static final String REGARDS_NAMESPACE = "regards.";

    /**
     * template used to perform REST request
     */
    private final RestTemplate restTemplate;

    /**
     * connection factory
     */
    private final MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory;

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

    /**
     * addresses configured to
     */
    private final String rabbitAddresses;

    /**
     * Used to retrieve all tenants
     */
    private final ITenantResolver tenantResolver;

    /**
     * List of static tenant to manage at startup
     */
    private final String[] bootstrapTenants;

    /**
     *
     * Constructor used to initialize properties from AmqpProperties
     *
     * @param pTenantResolver
     *            retrieve all tenants
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
     * @param pRabbitAddresses
     *            server addresses
     * @param pStartupTenants
     *            tenant to manage at startup
     */
    public RabbitVirtualHostAdmin(ITenantResolver pTenantResolver, String pRabbitmqUserName, String pRabbitmqPassword,
            String pAmqpManagementHost, Integer pAmqpManagementPort, RestTemplate pRestTemplate,
            MultitenantSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory, String pRabbitAddresses,
            String[] pStartupTenants) {
        super();
        this.tenantResolver = pTenantResolver;
        restTemplate = pRestTemplate;
        simpleRoutingConnectionFactory = pSimpleRoutingConnectionFactory;
        rabbitmqUserName = pRabbitmqUserName;
        rabbitmqPassword = pRabbitmqPassword;
        amqpManagementHost = pAmqpManagementHost;
        amqpManagementPort = pAmqpManagementPort;
        rabbitAddresses = pRabbitAddresses;
        this.bootstrapTenants = pStartupTenants;
    }

    /**
     * Manage virtual hosts according to tenants
     */
    @PostConstruct
    public void init() {
        // Initialize AMQP manager VHOST
        addVhost(AmqpConstants.AMQP_MANAGER);

        // Check if we have startup tenant vhost to manage
        if (bootstrapTenants != null) {
            for (String tenant : bootstrapTenants) {
                addVhost(tenant);
            }
        }

        // Retrieve already configured tenant
        Set<String> tenants = tenantResolver.getAllTenants();
        if (tenants != null) {
            for (String tenant : tenants) {
                addVhost(tenant);
            }
        }
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

    /**
     * Register {@link ConnectionFactory}
     *
     * @param pTenant
     *            tenant
     * @param pConnectionFactory
     *            related vhost {@link ConnectionFactory}
     */
    private void registerConnectionFactory(String pTenant, CachingConnectionFactory pConnectionFactory) {
        // if there is no registered connection factory for this vhost then register this one
        String registrationKey = getVhostName(pTenant);
        if (simpleRoutingConnectionFactory.getTargetConnectionFactory(registrationKey) == null) {
            simpleRoutingConnectionFactory.addTargetConnectionFactory(registrationKey, pConnectionFactory);
        }
    }

    /**
     * Unregister {@link ConnectionFactory}
     *
     * @param pTenant
     *            tenant
     */
    private void unregisterConnectionFactory(String pTenant) {
        // if there is a connection factory for this vhost then unregister it
        String registrationKey = getVhostName(pTenant);
        if (simpleRoutingConnectionFactory.getTargetConnectionFactory(registrationKey) == null) {
            simpleRoutingConnectionFactory.removeTargetConnectionFactory(registrationKey);
        }
    }

    @Override
    public ConnectionFactory getVhostConnectionFactory(String pTenant) {
        String registrationKey = getVhostName(pTenant);
        return simpleRoutingConnectionFactory.getTargetConnectionFactory(registrationKey);
    }

    @Override
    public void addVhost(String pTenant) {
        retrieveVhostList();
        final String fullyQualifiedVhostName = getVhostName(pTenant);

        LOGGER.info("Adding virtual host {} for tenant", fullyQualifiedVhostName, pTenant);

        if (!existVhost(fullyQualifiedVhostName)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Add VHOST using a PUT request
            ResponseEntity<String> response = restTemplate
                    .exchange(getRabbitApiVhostEndpoint() + SLASH + fullyQualifiedVhostName, HttpMethod.PUT, request,
                              String.class);
            int statusValue = response.getStatusCodeValue();
            if (!isSuccess(statusValue)) {
                String errorMessage = String.format("Cannot add vhost %s (status %s) : %s", fullyQualifiedVhostName,
                                                    statusValue, response.getBody());
                LOGGER.error(errorMessage);
                throw new RemovingRabbitMQVhostException(errorMessage);
            }
            addPermissionToAccessVhost(pTenant);
            vhostList.add(fullyQualifiedVhostName);
        }

        LOGGER.info("Creating connection factory for : tenant {}", pTenant);

        String[] rabbitHostAndPort = parseRabbitAddresses(rabbitAddresses);
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHostAndPort[0],
                Integer.parseInt(rabbitHostAndPort[1]));
        connectionFactory.setVirtualHost(RabbitVirtualHostAdmin.getVhostName(pTenant));

        registerConnectionFactory(pTenant, connectionFactory);
    }

    @Override
    public void removeVhost(String pTenant) {
        retrieveVhostList();
        String fullyQualifiedVhostName = getVhostName(pTenant);

        LOGGER.info("Removing virtual host {} for tenant", fullyQualifiedVhostName, pTenant);

        if (existVhost(fullyQualifiedVhostName)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate
                    .exchange(getRabbitApiVhostEndpoint() + SLASH + fullyQualifiedVhostName, HttpMethod.DELETE, request,
                              String.class);
            int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                String errorMessage = String.format("Cannot remove vhost %s (status %s) : %s", fullyQualifiedVhostName,
                                                    statusValue, response.getBody());
                LOGGER.error(errorMessage);
                throw new RemovingRabbitMQVhostException(errorMessage);
            }
        }

        LOGGER.info("Removing connection factory for : tenant {}", pTenant);
        unregisterConnectionFactory(pTenant);
    }

    /**
     * @param pRabbitAddresses
     *            addresses from configuration file
     * @return {host, port}
     */
    protected String[] parseRabbitAddresses(String pRabbitAddresses) {
        return pRabbitAddresses.split(COLON);
    }

    private void addPermissionToAccessVhost(String pVhost) {
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

    private String getRabbitApiPermissionVhostEndpoint(String pVhost) {
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
        return REGARDS_NAMESPACE + pTenant.toLowerCase();
    }

    @Override
    public void bind(String pTenant) {
        SimpleResourceHolder.bind(simpleRoutingConnectionFactory, getVhostName(pTenant));
    }

    @Override
    public void unbind() {
        SimpleResourceHolder.unbind(simpleRoutingConnectionFactory);
    }

    @Override
    public boolean isBound() {
        return SimpleResourceHolder.get(simpleRoutingConnectionFactory) != null;
    }

    @Override
    public boolean isBound(String pTenant) {
        String vhost = getVhostName(pTenant);
        String boundVhost = (String) SimpleResourceHolder.get(simpleRoutingConnectionFactory);
        return (vhost != null) && vhost.equals(boundVhost);
    }
}
