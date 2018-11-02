/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;

import feign.Response;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.dam.client.models.IAttributeModelClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order.service" })
@EnableAutoConfiguration
@EnableScheduling
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class ServiceConfiguration {

    @Autowired
    private IPublisher publisher;

    @Bean
    public IComplexSearchClient mockSearchClient() {
        return new SearchClientMock();
    }

    @Bean
    public ILegacySearchEngineClient legacyClientMock() {
        return new LegacySearchClientMock();
    }

    @Bean
    public IProjectsClient mockProjectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IAipClient mockAipClient() {
        final AipClientProxy aipClientProxy = new AipClientProxy(publisher);
        InvocationHandler handler = (proxy, method, args) -> {
            for (Method aipClientProxyMethod : aipClientProxy.getClass().getMethods()) {
                if (aipClientProxyMethod.getName().equals(method.getName())) {
                    return aipClientProxyMethod.invoke(aipClientProxy, args);
                }
            }
            return null;
        };
        return (IAipClient) Proxy.newProxyInstance(IAipClient.class.getClassLoader(),
                                                   new Class<?>[] { IAipClient.class }, handler);
    }

    private class AipClientProxy {

        private final IPublisher publisher;

        public AipClientProxy(IPublisher publisher) {
            this.publisher = publisher;
        }

        @SuppressWarnings("unused")
        public ResponseEntity<AvailabilityResponse> makeFilesAvailable(AvailabilityRequest availabilityRequest) {
            for (String checksum : availabilityRequest.getChecksums()) {
                if (((int) (Math.random() * 10) % 2) == 0) {
                    publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, checksum));
                } else {
                    publisher.publish(new DataFileEvent(DataFileEventState.ERROR, checksum));
                }
            }
            return ResponseEntity.ok(new AvailabilityResponse(Collections.emptySet(), Collections.emptySet(),
                    Collections.emptySet()));
        }

        @SuppressWarnings("unused")
        public Response downloadFile(String aipId, String checksum) {
            Response mockResp = Mockito.mock(Response.class);
            try {
                Mockito.when(mockResp.body().asInputStream())
                        .thenReturn(getClass().getResourceAsStream("/files/" + checksum));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mockResp;
        }

    }

    @Bean
    public IAuthenticationResolver mockAuthResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

    @Bean
    public IEmailClient mockEmailClient() {
        return Mockito.mock(IEmailClient.class);
    }

    @Bean
    public INotificationClient notificationClient() {
        return Mockito.mock(INotificationClient.class);
    }
}
