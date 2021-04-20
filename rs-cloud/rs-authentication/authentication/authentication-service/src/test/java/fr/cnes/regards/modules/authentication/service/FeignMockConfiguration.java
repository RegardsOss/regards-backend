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
package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignMockConfiguration {

    @Bean
    public IAccessRightSettingClient accessSettingsClient() {
        return Mockito.mock(IAccessRightSettingClient.class);
    }

    @Bean
    public IAccountsClient accountsClient() {
        return Mockito.mock(IAccountsClient.class);
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IModelAttrAssocClient modelAttrAssocClient() {
        return Mockito.mock(IModelAttrAssocClient.class);
    }

    @Bean
    public IModelClient modelClient() {
        return Mockito.mock(IModelClient.class);
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

    @Bean
    public IRegistrationClient registrationClient() {
        return Mockito.mock(IRegistrationClient.class);
    }

    @Bean
    public IRolesClient rolesClient() {
        return Mockito.mock(IRolesClient.class);
    }

    @Bean
    public IUserClient userClient() {
        return Mockito.mock(IUserClient.class);
    }
}
