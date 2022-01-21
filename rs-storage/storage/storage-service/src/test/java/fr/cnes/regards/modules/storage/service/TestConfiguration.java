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
package fr.cnes.regards.modules.storage.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.storage.service.cache.CacheScheduler;

/**
 * Global test configuration for storage tests
 *
 * @author Sébastien Binda
 */
@Configuration
@TestPropertySource(locations = { "classpath:src/test/resources/application-test.properties" })
public class TestConfiguration {

    @Bean
    public DiscoveryClient discoveryClient() throws URISyntaxException {

        DiscoveryClient client = Mockito.mock(DiscoveryClient.class);
        List<ServiceInstance> response = Lists.newArrayList();
        ServiceInstance service = Mockito.mock(ServiceInstance.class);
        Mockito.when(service.getUri()).thenReturn(new URI("http://localhost:7777"));
        response.add(service);
        Mockito.when(client.getInstances(Mockito.anyString())).thenReturn(response);
        return client;

    }

    @Bean
    @ConditionalOnMissingBean
    public CacheScheduler cahceScheduler() {
        return Mockito.mock(CacheScheduler.class);
    }
}
