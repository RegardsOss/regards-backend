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
package fr.cnes.regards.modules.search.client;

import java.util.ArrayList;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessGroupClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessRightClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 * Module-wide configuration for integration tests.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class SearchClientITConfiguration {

    @Bean
    public IAttributeModelClient attributeModelClient() {
        IAttributeModelClient attributeModelClient = Mockito.mock(IAttributeModelClient.class);
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(SearchClientTestUtils.ATTRIBUTE_MODEL_CLIENT_RESPONSE);
        return attributeModelClient;
    }

    @Bean
    public IUserClient userClient() {
        IUserClient userClient = Mockito.mock(IUserClient.class);
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(SearchClientTestUtils.USER_CLIENT_RESPONSE);
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.eq(SearchClientTestUtils.OTHER_USER_EMAIL),
                                                           Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(SearchClientTestUtils.USER_CLIENT_OTHER_RESPONSE);
        return userClient;
    }

    @Bean
    public IAccessGroupClient groupClient() {
        IAccessGroupClient accessGroupClient = Mockito.mock(IAccessGroupClient.class);

        // Build accessGroupMock mock
        final PagedResources.PageMetadata md = new PagedResources.PageMetadata(0, 0, 0);
        final PagedResources<Resource<AccessGroup>> pagedResources = new PagedResources<>(new ArrayList<>(), md,
                new ArrayList<>());
        final ResponseEntity<PagedResources<Resource<AccessGroup>>> pageResponseEntity = ResponseEntity
                .ok(pagedResources);
        Mockito.when(accessGroupClient.retrieveAccessGroupsList(Mockito.anyBoolean(), Mockito.anyInt(),
                                                                Mockito.anyInt()))
                .thenReturn(pageResponseEntity);
        return accessGroupClient;
    }

    @Bean
    public IAccessRightClient accessRightClient() {
        return Mockito.mock(IAccessRightClient.class);
    }

    @Bean
    public IDatasetClient datasetClient() {
        return Mockito.mock(IDatasetClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        IProjectUsersClient projectUsersClient = Mockito.mock(IProjectUsersClient.class);
        Mockito.when(projectUsersClient.isAdmin(Mockito.anyString()))
                .thenReturn(SearchClientTestUtils.PROJECT_USERS_CLIENT_RESPONSE);
        Mockito.when(projectUsersClient.isAdmin(Mockito.eq(SearchClientTestUtils.ADMIN_USER_EMAIL)))
                .thenReturn(SearchClientTestUtils.PROJECT_USERS_CLIENT_RESPONSE_ADMIN);
        return projectUsersClient;
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }
}
