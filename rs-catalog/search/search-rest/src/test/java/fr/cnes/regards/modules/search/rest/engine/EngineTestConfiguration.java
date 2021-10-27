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
package fr.cnes.regards.modules.search.rest.engine;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessRightClient;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class EngineTestConfiguration {

    @Bean
    public IDatasetClient datasetClient() {
        IDatasetClient client = Mockito.mock(IDatasetClient.class);
        Model mockedModel = new Model();
        mockedModel.setName("MockedModel");
        Dataset mockDataset = new Dataset(mockedModel, "tenant", "DSMOCK",
                "Mocked dataset response from mock dataset dam client");
        mockDataset.setId(1L);
        mockDataset.setIpId(UniformResourceName
                .fromString("URN:AIP:DATASET:tenant:27de606c-a6cd-411f-a5ba-bd1b2f29c965:V1"));
        Mockito.when(client.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(HateoasUtils.wrap(mockDataset), HttpStatus.OK));
        return client;
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IToponymsClient toponymesClient() {
        return Mockito.mock(IToponymsClient.class);
    }

    @Bean
    public IAccessGroupClient groupClient() {
        IAccessGroupClient accessGroupClient = Mockito.mock(IAccessGroupClient.class);

        // Build accessGroupMock mock
        PagedModel.PageMetadata md = new PagedModel.PageMetadata(0, 0, 0);
        PagedModel<EntityModel<AccessGroup>> pagedResources = new PagedModel<>(new ArrayList<>(), md,
                new ArrayList<>());
        ResponseEntity<PagedModel<EntityModel<AccessGroup>>> pageResponseEntity = ResponseEntity.ok(pagedResources);
        Mockito.when(accessGroupClient.retrieveAccessGroupsList(Mockito.anyBoolean(), Mockito.anyInt(),
                                                                Mockito.anyInt()))
                .thenReturn(pageResponseEntity);
        return accessGroupClient;
    }

    @Bean
    public IAccessRightClient accessClient() {
        return Mockito.mock(IAccessRightClient.class);
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

    @Bean
    public IAttachmentClient attachmentClient() {
        return Mockito.mock(IAttachmentClient.class);
    }

}
