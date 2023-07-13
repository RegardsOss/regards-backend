/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.client.config;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.order.client.mocks.TestAuthenticationResolver;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Iliana Ghazali
 **/
@Configuration
public class TestConfiguration {

    @Bean
    IAuthenticationResolver authenticationResolver() {
        return new TestAuthenticationResolver();
    }

    @MockBean
    IComplexSearchClient searchMock;

    @MockBean
    ILegacySearchEngineClient legacyMock;

    @MockBean
    IProjectUsersClient projectUsersClient;

    @MockBean
    IProjectsClient projectClient;

    @MockBean
    private IProcessingRestClient processingRestClient;

    @MockBean
    private IDatasetClient datasetClient;

    @MockBean
    private IEmailClient emailClient;

    @MockBean
    private IStorageRestClient storageRestClient;

    @MockBean
    private IAttributeModelClient modelAttributeClient;

    @MockBean
    private IModelAttrAssocClient modelAttrAssocClient;

    @MockBean
    private IProcessingEventSender processingEventSender;

    @MockBean
    private IAttachmentClient attachmentClient;

}
