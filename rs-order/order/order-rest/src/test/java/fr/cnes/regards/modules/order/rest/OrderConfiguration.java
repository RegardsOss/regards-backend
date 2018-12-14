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
package fr.cnes.regards.modules.order.rest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.dam.client.models.IAttributeModelClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
@PropertySource(value = "classpath:test.properties")
public class OrderConfiguration {

    @Bean
    public IAuthenticationResolver authResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IComplexSearchClient searchClient() {
        IComplexSearchClient searchClient = Mockito.mock(IComplexSearchClient.class);
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(0l);
        summary.addFilesCount(0l);
        summary.addFilesSize(0l);
        Mockito.when(searchClient.computeDatasetsSummary(Mockito.any())).thenReturn(ResponseEntity.ok(summary));
        return searchClient;
    }

    @Bean
    public ILegacySearchEngineClient legacyClient() {
        return Mockito.mock(ILegacySearchEngineClient.class);
    }

    @Bean
    public IAipClient aipClient() {
        AipClientProxy aipClientProxy = new AipClientProxy();
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

        @SuppressWarnings("unused")
        public Response downloadFile(String aipId, String checksum) {
            return Response.builder().status(200).body(getClass().getResourceAsStream("/files/" + checksum), 1000)
                    .build();
        }
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IEmailClient emailClient() {
        return Mockito.mock(IEmailClient.class);
    }

    @Bean
    public INotificationClient notificationClient() {
        return Mockito.mock(INotificationClient.class);
    }
}
