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
package fr.cnes.regards.modules.search.rest.engine;

import java.util.ArrayList;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessGroupClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessRightClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class EngineConfiguration {

    @Bean
    public IDatasetClient datasetClient() {
        IDatasetClient client = Mockito.mock(IDatasetClient.class);
        Model mockedModel = new Model();
        mockedModel.setName("MockedModel");
        Dataset mockDataset = new Dataset(mockedModel, "tenant",
                "Mocked dataset response from mock dataset dam client");
        mockDataset.setId(1L);
        mockDataset.setIpId(UniformResourceName
                .fromString("URN:AIP:DATASET:tenant:27de606c-a6cd-411f-a5ba-bd1b2f29c965:V1"));
        Mockito.when(client.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<Resource<Dataset>>(HateoasUtils.wrap(mockDataset), HttpStatus.OK));
        return client;
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IUserClient userClient() {
        return Mockito.mock(IUserClient.class);
    }

    @Bean
    public IAccessGroupClient groupClient() {
        IAccessGroupClient accessGroupClient = Mockito.mock(IAccessGroupClient.class);

        // Build accessGroupMock mock
        PagedResources.PageMetadata md = new PagedResources.PageMetadata(0, 0, 0);
        PagedResources<Resource<AccessGroup>> pagedResources = new PagedResources<>(new ArrayList<>(), md,
                new ArrayList<>());
        ResponseEntity<PagedResources<Resource<AccessGroup>>> pageResponseEntity = ResponseEntity.ok(pagedResources);
        Mockito.when(accessGroupClient.retrieveAccessGroupsList(Mockito.anyBoolean(), Mockito.anyInt(),
                                                                Mockito.anyInt()))
                .thenReturn(pageResponseEntity);
        return accessGroupClient;
    }

    @Bean
    public IAccessRightClient accessClient() {
        IAccessRightClient accessGroupClient = Mockito.mock(IAccessRightClient.class);
        return accessGroupClient;
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

    @Bean
    public IPoller poller() {
        return Mockito.mock(IPoller.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

}
