/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

import fr.cnes.regards.framework.amqp.domain.RabbitMqVhostPermission;
import fr.cnes.regards.framework.amqp.domain.RabbitVhost;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RemovingRabbitMQVhostException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * implementation compliant with RabbitMQ v3.6.5
 *
 * @author svissier
 * @author Marc Sordi
 */
public class RabbitVirtualHostAdmin implements IRabbitVirtualHostAdmin, InitializingBean {

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
    private final RestOperations restOperations;

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
     * Protocol used to connect to the broker manager API
     */
    private final String amqpManagementProtocol;

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
     * is true when rabbitAddresses must be accessed threw TLS
     */
    private final Boolean isSslEnabled;

    /**
     * Used to retrieve all tenants
     */
    private final ITenantResolver tenantResolver;

    /**
     * List of static tenant to manage at startup
     */
    private final String[] bootstrapTenants;

    private final VirtualHostMode mode;

    /**
     * Constructor used to initialize properties from AmqpProperties
     *
     * @param mode                           {@link VirtualHostMode}
     * @param tenantResolver                 retrieve all tenants
     * @param rabbitmqUserName               user name
     * @param rabbitmqPassword               password
     * @param amqpManagementProtocol         management protocol
     * @param amqpManagementHost             management host
     * @param amqpManagementPort             management port
     * @param restOperations                 client REST
     * @param simpleRoutingConnectionFactory connection factory to handle multi-tenancy
     * @param rabbitAddresses                server addresses
     * @param isSslEnabled                   server ssl is active
     * @param startupTenants                 tenant to manage at startup
     */
    public RabbitVirtualHostAdmin(VirtualHostMode mode,
                                  ITenantResolver tenantResolver,
                                  String rabbitmqUserName,
                                  String rabbitmqPassword,
                                  String amqpManagementProtocol,
                                  String amqpManagementHost,
                                  Integer amqpManagementPort,
                                  RestOperations restOperations,
                                  MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory,
                                  String rabbitAddresses,
                                  Boolean isSslEnabled,
                                  String[] startupTenants) {
        super();
        this.mode = mode;
        this.tenantResolver = tenantResolver;
        this.restOperations = restOperations;
        this.simpleRoutingConnectionFactory = simpleRoutingConnectionFactory;
        this.rabbitmqUserName = rabbitmqUserName;
        this.rabbitmqPassword = rabbitmqPassword;
        this.amqpManagementProtocol = amqpManagementProtocol;
        this.amqpManagementHost = amqpManagementHost;
        this.amqpManagementPort = amqpManagementPort;
        this.rabbitAddresses = rabbitAddresses;
        this.isSslEnabled = isSslEnabled;
        this.bootstrapTenants = startupTenants;
    }

    /**
     * Manage virtual hosts according to tenants
     */
    @Override
    public void afterPropertiesSet() {

        // Initialize AMQP instance manager VHOST
        addVhost(AmqpChannel.AMQP_INSTANCE_MANAGER);

        if (VirtualHostMode.SINGLE.equals(mode)) {
            // Initialize AMQP multitenant manager VHOST
            addVhost(AmqpChannel.AMQP_MULTITENANT_MANAGER);
        } else {
            // Check if we have startup tenant vhost to manage
            if (bootstrapTenants != null) {
                for (String tenant : bootstrapTenants) {
                    addVhost(RabbitVirtualHostAdmin.getVhostName(tenant));
                }
            }

            // Retrieve already configured tenant
            Set<String> tenants = tenantResolver.getAllTenants();
            if (tenants != null) {
                for (String tenant : tenants) {
                    addVhost(RabbitVirtualHostAdmin.getVhostName(tenant));
                }
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

        vhostList = new ArrayList<>();
        final ResponseEntity<List<RabbitVhost>> response = restOperations.exchange(host,
                                                                                   HttpMethod.GET,
                                                                                   request,
                                                                                   typeRef);
        List<RabbitVhost> rabbitVhosts = ResponseEntityUtils.extractBodyOrNull(response);
        if (rabbitVhosts != null) {
            vhostList = rabbitVhosts.stream().map(RabbitVhost::getName).collect(Collectors.toList());
        }
        return vhostList;
    }

    @Override
    public String setBasic() {
        return "Basic " + encode(rabbitmqUserName, rabbitmqPassword);
    }

    @Override
    public String encode(String rabbitmqUserName, String rabbitmqPassword) {
        final String fullCredential = rabbitmqUserName + COLON + rabbitmqPassword;
        final byte[] plainCredsBytes = fullCredential.getBytes();
        return Base64.getEncoder().encodeToString(plainCredsBytes);
    }

    @Override
    public String getRabbitApiVhostEndpoint() {
        return getRabbitApiEndpoint() + "/vhosts";
    }

    @Override
    public String getRabbitApiEndpoint() {
        return amqpManagementProtocol + COLON + "//" + amqpManagementHost + COLON + amqpManagementPort + "/api";
    }

    /**
     * Register {@link ConnectionFactory}
     *
     * @param virtualHost        virtual host
     * @param pConnectionFactory related vhost {@link ConnectionFactory}
     */
    private void registerConnectionFactory(String virtualHost, CachingConnectionFactory pConnectionFactory) {
        // if there is no registered connection factory for this vhost then register this one
        if (simpleRoutingConnectionFactory.getTargetConnectionFactory(virtualHost) == null) { // NOSONAR
            simpleRoutingConnectionFactory.addTargetConnectionFactory(virtualHost, pConnectionFactory);
        }
    }

    /**
     * Unregister {@link ConnectionFactory}
     *
     * @param virtualHost tenant
     */
    private void unregisterConnectionFactory(String virtualHost) {
        // if there is a connection factory for this vhost then unregister it
        if (simpleRoutingConnectionFactory.getTargetConnectionFactory(virtualHost) != null) { // NOSONAR
            simpleRoutingConnectionFactory.removeTargetConnectionFactory(virtualHost);
        }
    }

    @Override
    public ConnectionFactory getVhostConnectionFactory(String virtualHost) {
        return simpleRoutingConnectionFactory.getTargetConnectionFactory(virtualHost);
    }

    @Override
    public void addVhost(String virtualHost) {
        retrieveVhostList();

        LOGGER.info("Adding virtual host {}", virtualHost);

        if (!existVhost(virtualHost)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Add VHOST using a PUT request
            ResponseEntity<String> response = restOperations.exchange(getRabbitApiVhostEndpoint() + SLASH + virtualHost,
                                                                      HttpMethod.PUT,
                                                                      request,
                                                                      String.class);
            int statusValue = response.getStatusCodeValue();
            if (!isSuccess(statusValue)) {
                String errorMessage = String.format("Cannot add vhost %s (status %s) : %s",
                                                    virtualHost,
                                                    statusValue,
                                                    response.getBody());
                LOGGER.error(errorMessage);
                throw new RemovingRabbitMQVhostException(errorMessage);
            }
            addPermissionToAccessVhost(virtualHost);
            vhostList.add(virtualHost);
        }

        LOGGER.info("Creating connection factory for virtual host {}", virtualHost);

        URI uri = buildRabbitServerUri(rabbitAddresses, isSslEnabled);
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(uri);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setUsername(rabbitmqUserName);
        connectionFactory.setPassword(rabbitmqPassword);
        registerConnectionFactory(virtualHost, connectionFactory);
    }

    /**
     * @param rabbitAddresses addresses from configuration file
     * @param isSecurised     true when the rabbitmq server expects TLS, false OR null otherwise
     * @return URI to connect to RabbitMQ server
     */
    private static URI buildRabbitServerUri(String rabbitAddresses, Boolean isSecurised) {
        String protocol = Boolean.TRUE.equals(isSecurised) ? "amqps://" : "amqp://";
        return URI.create(protocol + rabbitAddresses);
    }

    @Override
    public void removeVhost(String virtualHost) {
        retrieveVhostList();

        LOGGER.info("Removing virtual host {}", virtualHost);

        if (existVhost(virtualHost)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, setBasic());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restOperations.exchange(getRabbitApiVhostEndpoint() + SLASH + virtualHost,
                                                                      HttpMethod.DELETE,
                                                                      request,
                                                                      String.class);
            int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                String errorMessage = String.format("Cannot remove vhost %s (status %s) : %s",
                                                    virtualHost,
                                                    statusValue,
                                                    response.getBody());
                LOGGER.error(errorMessage);
                throw new RemovingRabbitMQVhostException(errorMessage);
            }
        }

        LOGGER.info("Removing connection factory for virtual host {}", virtualHost);
        unregisterConnectionFactory(virtualHost);
    }

    private void addPermissionToAccessVhost(String virtualHost) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, setBasic());
        final HttpEntity<RabbitMqVhostPermission> request = new HttpEntity<>(new RabbitMqVhostPermission(), headers);
        final ResponseEntity<String> response = restOperations.exchange(getRabbitApiPermissionVhostEndpoint(virtualHost),
                                                                        HttpMethod.PUT,
                                                                        request,
                                                                        String.class);
        final int statusValue = response.getStatusCodeValue();
        if (!isSuccess(statusValue)) {
            throw new AddingRabbitMQVhostPermissionException(response.getBody() + NEW_LINE_STATUS + statusValue);
        }
    }

    private String getRabbitApiPermissionVhostEndpoint(String virtualHost) {
        return getRabbitApiEndpoint() + "/permissions/" + virtualHost + SLASH + rabbitmqUserName;
    }

    @Override
    public boolean isSuccess(int statusValue) {
        final int hundred = 100;
        return (statusValue / hundred) == 2;
    }

    @Override
    public boolean existVhost(String virtualHost) {
        return vhostList.stream().anyMatch(rVhName -> rVhName.equals(virtualHost));
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
     * @param tenant Tenant wanted
     * @return fully qualified name of vhost accord to namespace
     */
    public static String getVhostName(String tenant) {
        return REGARDS_NAMESPACE + tenant.toLowerCase();
    }

    @Override
    public void bind(String virtualHost) {
        SimpleResourceHolder.bind(simpleRoutingConnectionFactory, virtualHost);
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
    public boolean isBound(String virtualHost) {
        String boundVhost = (String) SimpleResourceHolder.get(simpleRoutingConnectionFactory);
        return (virtualHost != null) && virtualHost.equals(boundVhost);
    }

    @Override
    public VirtualHostMode getMode() {
        return mode;
    }
}
