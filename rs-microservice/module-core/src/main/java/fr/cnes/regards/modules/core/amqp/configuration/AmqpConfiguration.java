/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.configuration;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.modules.core.amqp.domain.RabbitMqVhostPermission;
import fr.cnes.regards.modules.core.amqp.utils.RabbitVhost;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;

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
     * username used to connect to the broker and it's manager
     */
    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUserName;

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
     * List of vhost already known
     */
    private List<String> vhostList;

    /**
     *
     * GET Request to host/api/vhosts to know which Vhosts are already defined
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
    private String setBasic() {
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
    private String getRabbitApiVhostEndpoint() {
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
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     */
    public void addVhost(String pName) throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
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
     * @param pVhost
     *            vhost we want to manage
     * @return a rabbitAdmin instance parameterized to connect and manage the right vhost ie tenant
     */
    public RabbitAdmin getRabbitAdminFor(String pVhost) {
        return new RabbitAdmin(getConnectionFactory(pVhost));
    }

    /**
     * @param pVhost
     *            vhost we want to connect to
     * @return a connection factory parameterized to connect to the specified tenant
     */
    public ConnectionFactory getConnectionFactory(String pVhost) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(amqpManagementHost, 5762);
        connectionFactory.setUsername(rabbitmqUserName);
        connectionFactory.setPassword(rabbitmqPassword);
        connectionFactory.setVirtualHost(pVhost);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory pConnectionFactory) {
        return new RabbitAdmin(pConnectionFactory);
    }

}
