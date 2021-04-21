/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Maps;

import feign.Request;
import feign.Response;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;

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
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IModelAttrAssocClient attributeModelAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
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
    public IStorageRestClient storageRestClient() {
        IStorageRestClientProxy aipClientProxy = new IStorageRestClientProxy();
        InvocationHandler handler = (proxy, method, args) -> {
            for (Method aipClientProxyMethod : aipClientProxy.getClass().getMethods()) {
                if (aipClientProxyMethod.getName().equals(method.getName())) {
                    return aipClientProxyMethod.invoke(aipClientProxy, args);
                }
            }
            return null;
        };
        return (IStorageRestClient) Proxy.newProxyInstance(IStorageRestClient.class.getClassLoader(),
                                                           new Class<?>[] { IStorageRestClient.class }, handler);
    }

    private class IStorageRestClientProxy {

        Map<String, Collection<String>> headers = new HashMap<>();

        @SuppressWarnings("unused")
        public Response downloadFile(String checksum) {
            return Response.builder().status(200).headers(headers)
                    .request(Request.create(feign.Request.HttpMethod.GET, "", Maps.newHashMap(), null))
                    .body(getClass().getResourceAsStream("/files/" + checksum), 1000).build();
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
    public IProcessingRestClient processingRestClient() {
        return Mockito.mock(IProcessingRestClient.class);
    }

    @Bean
    public IProcessingEventSender processingEventSender() {
        return Mockito.mock(IProcessingEventSender.class);
    }
}
