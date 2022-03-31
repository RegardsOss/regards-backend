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
package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.client.cache.CacheableServiceAggregatorClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineJsonClient;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class BackendForFrontendTestConfiguration {

    @Bean
    @Primary
    public IServiceAggregatorClient serviceAggregatorClient() {
        IServiceAggregatorClient mock = Mockito.mock(IServiceAggregatorClient.class);
        Mockito.when(mock.retrieveServices(Arrays.asList(BackendForFrontendTestUtils.DATASET_0.getIpId().toString()),
                                           null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_0);
        Mockito.when(mock.retrieveServices(Arrays.asList(BackendForFrontendTestUtils.DATASET_1.getIpId().toString()),
                                           null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_1);
        return mock;
    }

    @Bean
    public CacheableServiceAggregatorClient cacheableServiceAggregatorClient(IServiceAggregatorClient client) {
        return new CacheableServiceAggregatorClient(client);
    }

    @Bean
    public ILegacySearchEngineJsonClient searchClient() {
        ILegacySearchEngineJsonClient mock = Mockito.mock(ILegacySearchEngineJsonClient.class);
        Mockito.when(mock.searchAll(Mockito.any())).thenReturn(BackendForFrontendTestUtils.SEARCH_ALL_RESULT);
        Mockito.when(mock.searchAll(Mockito.any())).thenReturn(BackendForFrontendTestUtils.SEARCH_ALL_RESULT);
        Mockito.when(mock.searchCollections(Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_COLLECTIONS_RESULT);
        Mockito.when(mock.searchDatasets(Mockito.any())).thenReturn(BackendForFrontendTestUtils.SEARCH_DATASETS_RESULT);
        Mockito.when(mock.searchDataObjects(Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_DATAOBJECTS_RESULT);
        Mockito.when(mock.searchDataobjectsReturnDatasets(Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_DATASETS_RESULT);
        return mock;
    }

    @Bean
    public IToponymsClient toponymsClient() {
        return Mockito.mock(IToponymsClient.class);
    }
}
