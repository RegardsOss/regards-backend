/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.rest;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

/**
 * Class AuthenticationTestConfiguration
 * <p>
 * Test configuration class
 *
 * @author Sébastien Binda
 */
@ComponentScan(basePackages = {"fr.cnes.regards.modules"})
@Configuration
@PropertySource("classpath:test.properties")
public class AuthenticationTestConfiguration {

    public final static String PROJECT_TEST_NAME = "PROJECT";

    public final static String VALID_PASSWORD = "valid";

    public final static String INVALID_PASSWORD = "invalid";

    @Bean
    @Primary
    public IPluginConfigurationRepository pluginConfigurationRepo() {
        return Mockito.mock(IPluginConfigurationRepository.class);
    }

    @Bean
    @Primary
    public IProjectUsersClient client() {
        IProjectUsersClient client = Mockito.mock(IProjectUsersClient.class);

        Role testRole = new Role();
        testRole.setName("TEST");
        ProjectUser testUser = new ProjectUser();
        testUser.setEmail("test@regards.fr");
        testUser.setRole(testRole);
        testUser.setStatus(UserStatus.ACCESS_GRANTED);

        ResponseEntity<EntityModel<ProjectUser>> response = new ResponseEntity<>(EntityModel.of(testUser,
                                                                                                new ArrayList<>()),
                                                                                 HttpStatus.OK);
        Mockito.when(client.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(response);

        return client;
    }

    @Bean
    @Primary
    public IProjectsClient projectsClient() {
        IProjectsClient client = Mockito.mock(IProjectsClient.class);

        ResponseEntity<EntityModel<Project>> response = new ResponseEntity<>(EntityModel.of(new Project("",
                                                                                                        "",
                                                                                                        true,
                                                                                                        PROJECT_TEST_NAME)),
                                                                             HttpStatus.OK);
        Mockito.when(client.retrieveProject(Mockito.anyString())).thenReturn(response);
        return client;
    }

    @Bean
    public IAccountsClient accountsClient() {
        final IAccountsClient mock = Mockito.mock(IAccountsClient.class);
        final Account account = new Account("email@test.fr", "name", "lastname", "password");
        account.setStatus(AccountStatus.ACTIVE);
        account.setOrigin(Account.REGARDS_ORIGIN);
        final EntityModel<Account> resource = HateoasUtils.wrap(account);
        final ResponseEntity<EntityModel<Account>> response = ResponseEntity.ok(resource);

        Mockito.when(mock.retrieveAccountByEmail(Mockito.any())).thenReturn(response);

        // Password validation depends on password only for testing
        Mockito.when(mock.validatePassword(Mockito.anyString(), Mockito.eq(INVALID_PASSWORD)))
               .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));
        Mockito.when(mock.validatePassword(Mockito.anyString(), Mockito.eq(VALID_PASSWORD)))
               .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));
        return mock;
    }

}
