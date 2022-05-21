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
package fr.cnes.regards.modules.search.client;

import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessRightClient;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

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
    public IAccessGroupClient groupClient() {
        IAccessGroupClient accessGroupClient = Mockito.mock(IAccessGroupClient.class);

        // Build accessGroupMock mock
        final PagedModel.PageMetadata md = new PagedModel.PageMetadata(0, 0, 0);
        final PagedModel<EntityModel<AccessGroup>> pagedResources = PagedModel.of(new ArrayList<>(),
                                                                                  md,
                                                                                  new ArrayList<>());
        final ResponseEntity<PagedModel<EntityModel<AccessGroup>>> pageResponseEntity = ResponseEntity.ok(pagedResources);
        Mockito.when(accessGroupClient.retrieveAccessGroupsList(Mockito.anyBoolean(),
                                                                Mockito.anyInt(),
                                                                Mockito.anyInt())).thenReturn(pageResponseEntity);
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
    public IStorageRestClient storageRestClient() {
        return Mockito.mock(IStorageRestClient.class);
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean
    public IToponymsClient toponymsClient() {
        return Mockito.mock(IToponymsClient.class);
    }

    @Primary
    @Bean
    public ProjectGeoSettings mockProjectGeoSettings() {
        return Mockito.mock(ProjectGeoSettings.class);
    }

    @Bean
    public IAttachmentClient attachmentClient() {
        return Mockito.mock(IAttachmentClient.class);
    }

    @Bean
    public ILicenseClient licenseClient() {
        return Mockito.mock(ILicenseClient.class);
    }
}
