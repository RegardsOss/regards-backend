/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microserices.administration.stubs.ProjectConnectionClientStub;
import fr.cnes.regards.microservices.administration.FeignInitialAdminClients;
import fr.cnes.regards.microservices.administration.MicroserviceClientsAutoConfiguration;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuratiob class
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableAutoConfiguration(exclude = { MicroserviceClientsAutoConfiguration.class, DataSourceAutoConfiguration.class })
@PropertySource("classpath:dao.properties")
public class JpaTenantConnectionConfiguration {

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Qualifier("initProjectConnectionsClient")
    @Primary
    public IProjectConnectionClient projectConnectionClient() {
        return new ProjectConnectionClientStub();
    }

    @Bean
    @Primary
    public DiscoveryClient discoveryClient() {
        final DiscoveryClient client = Mockito.mock(DiscoveryClient.class);
        Mockito.when(client.getInstances(Mockito.anyString())).thenReturn(new ArrayList<>());
        return client;
    }

    @Bean
    @Primary
    public FeignInitialAdminClients initialClients() {
        final FeignInitialAdminClients adminMocks = new FeignInitialAdminClients(discoveryClient());
        adminMocks.setProjectsClient(new ProjectClientStub());
        return adminMocks;
    }

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

    /**
     *
     * Stub administration client
     *
     * @return {@link IProjectsClient}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IRolesClient roleClient() {
        return Mockito.mock(IRolesClient.class);
    }

    /**
     *
     * Mock AMQP Publisher
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    IPublisher publisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     *
     * Mock AMQP Subscriber
     *
     * @return {@link ISubscriber}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    ISubscriber subsriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IResourcesClient resourceClient() {
        final ResponseEntity<List<ResourceMapping>> response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        final IResourcesClient mock = Mockito.mock(IResourcesClient.class);
        Mockito.when(mock.registerMicroserviceEndpoints(Mockito.any(), Mockito.any())).thenReturn(response);
        return mock;
    }

}
