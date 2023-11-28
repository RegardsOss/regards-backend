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
package fr.cnes.regards.modules.order.test;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.mockito.Mockito.mock;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order.service" })
@EnableAutoConfiguration
@EnableScheduling
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
                ignoreResourceNotFound = true)
public class ServiceConfiguration {

    @Bean
    public IComplexSearchClient mockSearchClient() {
        return new SearchClientMock();
    }

    @Bean
    public IStorageRestClient storageRestClient() {
        return mock(IStorageRestClient.class);
    }

    @Bean
    public IStorageSettingClient storageSettingClient() {
        return Mockito.mock(IStorageSettingClient.class);
    }

    @Bean
    public ILegacySearchEngineClient legacyClientMock() {
        return new LegacySearchClientMock();
    }

    @Bean
    public IProjectsClient mockProjectsClient() {
        return mock(IProjectsClient.class);
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return mock(IAttributeModelClient.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return mock(IModelAttrAssocClient.class);
    }

    @Bean
    @Primary
    public IStorageClient storageClient(IStorageFileListener listener) {
        return new StorageClientMock(listener, true);
    }

    @Bean
    public IAuthenticationResolver mockAuthResolver() {
        return mock(IAuthenticationResolver.class);
    }

    @Bean
    public IEmailClient mockEmailClient() {
        return mock(IEmailClient.class);
    }

    @Bean
    public IProcessingRestClient processingRestClient() {
        return mock(IProcessingRestClient.class);
    }

    @Bean
    public IProcessingEventSender processingEventSender() {
        return mock(IProcessingEventSender.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return mock(IProjectUsersClient.class);
    }

    @Bean
    public IDatasetClient datasetClient() {
        return mock(IDatasetClient.class);
    }

    @Bean
    public IAttachmentClient attachmentClient() {
        return mock(IAttachmentClient.class);
    }
}
