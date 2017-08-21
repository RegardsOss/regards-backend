/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.search.client.ISearchAllClient;
import fr.cnes.regards.modules.search.client.ISearchAllWithFacetsClient;
import fr.cnes.regards.modules.search.client.ISearchCollectionsClient;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("test")
@Configuration
public class BackendForFrontendTestConfiguration {

    @Bean
    @Primary
    public IServiceAggregatorClient serviceAggregatorClient() {
        IServiceAggregatorClient mock = Mockito.mock(IServiceAggregatorClient.class);
        Mockito.when(mock.retrieveServices(BackendForFrontendTestUtils.DATASET_0.getIpId().toString(), null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_0);
        Mockito.when(mock.retrieveServices(BackendForFrontendTestUtils.DATASET_1.getIpId().toString(), null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_1);
        return mock;
    }

    @Bean
    @Primary
    public ISearchAllClient searchAllClient() {
        ISearchAllClient mock = Mockito.mock(ISearchAllClient.class);
        Mockito.when(mock.searchAll(Mockito.any())).thenReturn(BackendForFrontendTestUtils.SEARCH_ALL_RESULT);
        return mock;
    }

    @Bean
    @Primary
    public ISearchAllWithFacetsClient searchAllWithFacetsClient() {
        ISearchAllWithFacetsClient mock = Mockito.mock(ISearchAllWithFacetsClient.class);
        Mockito.when(mock.searchAll(Mockito.any(), Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_ALL_RESULT);
        return mock;
    }

    @Bean
    @Primary
    public ISearchCollectionsClient searchCollectionsClient() {
        ISearchCollectionsClient mock = Mockito.mock(ISearchCollectionsClient.class);
        Mockito.when(mock.searchCollections(Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_COLLECTIONS_RESULT);
        return mock;
    }

}
