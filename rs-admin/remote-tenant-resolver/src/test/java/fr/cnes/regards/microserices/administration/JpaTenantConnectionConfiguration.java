/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microserices.administration;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microserices.administration.stubs.ProjectConnectionClientStub;
import fr.cnes.regards.microservices.administration.RemoteClientAutoConfiguration;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

/**
 * Class JpaTenantConnectionConfiguration
 * <p>
 * Test configuratiob class
 *
 * @author Sébastien Binda
 */
@Configuration
@EnableAutoConfiguration(exclude = { RemoteClientAutoConfiguration.class, DataSourceAutoConfiguration.class })
@PropertySource("classpath:dao.properties")
public class JpaTenantConnectionConfiguration {

    /**
     * Stub administration client
     *
     * @return IProjectsClient
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

    /**
     * Stub administration client
     *
     * @return IProjectsClient
     */
    @Bean
    @Primary
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

    /**
     * Stub administration client
     *
     * @return {@link IProjectsClient}
     */
    @Bean
    @Primary
    public IRolesClient roleClient() {
        final IRolesClient rolesClientMock = Mockito.mock(IRolesClient.class);
        Mockito.when(rolesClientMock.getAllRoles()).thenReturn(ResponseEntity.ok(new ArrayList<>()));
        return rolesClientMock;
    }

    /**
     * Mock AMQP Publisher
     *
     * @return {@link IPublisher}
     */
    @Bean
    IPublisher publisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     * Mock AMQP Subscriber
     *
     * @return {@link ISubscriber}
     */
    @Bean
    ISubscriber subsriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     * Stub administration client
     *
     * @return IProjectsClient
     */
    @Bean
    @Primary
    public IMicroserviceResourceClient resourceClient() {
        final ResponseEntity<Void> response = ResponseEntity.ok(null);
        final IMicroserviceResourceClient mock = Mockito.mock(IMicroserviceResourceClient.class);
        Mockito.when(mock.registerMicroserviceEndpoints(Mockito.anyString(), Mockito.any())).thenReturn(response);

        final PageMetadata md = new PageMetadata(0, 0, 0);
        final PagedModel<EntityModel<ResourcesAccess>> pagedResources = PagedModel.of(new ArrayList<>(),
                                                                                      md,
                                                                                      new ArrayList<>());
        final ResponseEntity<PagedModel<EntityModel<ResourcesAccess>>> resourcesResponse = ResponseEntity.ok(
            pagedResources);
        Mockito.when(mock.getAllResourceAccessesByMicroservice(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
               .thenReturn(resourcesResponse);
        return mock;
    }

}
