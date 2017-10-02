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
package fr.cnes.regards.modules.order.rest;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
@PropertySource(value = "classpath:test.properties")
public class OrderConfiguration {
    @Bean
    public ICatalogClient catalogClient() {
        return Mockito.mock(ICatalogClient.class);
    }

    @Bean
    public IAipClient aipClient() {
        return new IAipClient() {

            @Override
            public InputStream downloadFile(String aipId, String checksum) {
                return getClass().getResourceAsStream("/files/" + checksum);
            }

            @Override
            public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(AIPState pState, OffsetDateTime pFrom,
                    OffsetDateTime pTo, int pPage, int pSize) {
                return null;
            }

            @Override
            public HttpEntity<Set<UUID>> createAIP(Set<AIP> aips) {
                return null;
            }

            @Override
            public HttpEntity<List<DataObject>> retrieveAIPFiles(UniformResourceName pIpId) {
                return null;
            }

            @Override
            public HttpEntity<List<String>> retrieveAIPVersionHistory(UniformResourceName pIpId, int pPage,
                    int pSize) {
                return null;
            }

            @Override
            public HttpEntity<AvailabilityResponse> makeFilesAvailable(AvailabilityRequest availabilityRequest) {
                return null;
            }
        };
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }
}
