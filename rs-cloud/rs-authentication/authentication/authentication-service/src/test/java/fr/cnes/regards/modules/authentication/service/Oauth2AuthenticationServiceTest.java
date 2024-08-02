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
package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.authentication.domain.data.AuthenticationStatus;
import fr.cnes.regards.modules.authentication.domain.exception.oauth2.AuthenticationException;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.service.oauth2.Oauth2AuthenticationService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

/**
 * Class Oauth2AuthenticationServiceTest
 * Test Regards pseudo-Oauth2 authentication process
 *
 * @author Sébastien Binda
 * @author Olivier Rousselot
 */
public class Oauth2AuthenticationServiceTest {

    /**
     * Authentication service to test
     */
    private static Oauth2AuthenticationService oauth2AuthenticationService;

    /**
     * JWT authentication to use during test
     */
    private static JWTAuthentication auth;

    /**
     * Mock for user accounts manager dealing with administration services.
     */
    private static IUserAccountManager userAccountManagerMock;

    /**
     * Mock for accounts client from administration service.
     */
    private static IAccountsClient accountsClientMock;

    /**
     * Mock to retrieve project users
     */
    private static IProjectUsersClient projectUsersClientMock;

    /**
     * Mock for authentication plugin
     */
    private static IAuthenticationPlugin plugin;

    /**
     * JwtService
     */
    private static JWTService jwtService = new JWTService();
    static {
        try {
            jwtService.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Bean factory mock
     */
    private static BeanFactory beanFactoryMock;

    /**
     * Test valid Account
     */
    private static Account validAccount;

    /**
     * Test valid user
     */
    private static ProjectUser validUser;

    /**
     * Mocks initialization
     */
    @BeforeClass
    public static void init() {
        // Create mock for default authentication plugin
        plugin = Mockito.mock(IAuthenticationPlugin.class);
        Mockito.when(plugin.authenticate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(new AuthenticationPluginResponse(true, "test@regards.fr"));

        // Create mock for runtimeTenantResolver
        IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);

        // Initialize valid account
        validAccount = new Account("test@regards.fr", "test", "test", "test");
        validAccount.setStatus(AccountStatus.ACTIVE);
        validAccount.setOrigin(Account.REGARDS_ORIGIN);

        final Role role = new Role();
        role.setName("USER");
        validUser = new ProjectUser();
        validUser.setEmail("test@regards.fr");
        validUser.setRole(role);
        validUser.setStatus(UserStatus.ACCESS_GRANTED);

        // Mock Spring beanFactory to provide needed beans
        beanFactoryMock = Mockito.mock(BeanFactory.class);

        // Mock Plugins service
        final IPluginService pluginServiceMock = Mockito.mock(IPluginService.class);
        Mockito.when(beanFactoryMock.getBean(IPluginService.class)).thenReturn(pluginServiceMock);

        // Mock Administration Projects client
        final IProjectsClient projectsClientMock = Mockito.mock(IProjectsClient.class);
        final ResponseEntity<EntityModel<Project>> response = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(projectsClientMock.retrieveProject(Mockito.anyString())).thenReturn(response);
        Mockito.when(beanFactoryMock.getBean(IProjectsClient.class)).thenReturn(projectsClientMock);

        // Mock Administration Projects client
        accountsClientMock = Mockito.mock(IAccountsClient.class);
        final EntityModel<Account> entityModel = EntityModel.of(validAccount);
        Mockito.when(accountsClientMock.retrieveAccountByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(entityModel, HttpStatus.OK));
        Mockito.when(accountsClientMock.validatePassword(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        userAccountManagerMock = Mockito.mock(IUserAccountManager.class);

        Mockito.when(beanFactoryMock.getBean(IAccountsClient.class)).thenReturn(accountsClientMock);
        Mockito.when(beanFactoryMock.getBean(IUserAccountManager.class)).thenReturn(userAccountManagerMock);

        projectUsersClientMock = Mockito.mock(IProjectUsersClient.class);
        final EntityModel<ProjectUser> entityModelUser = EntityModel.of(validUser);
        final ResponseEntity<EntityModel<ProjectUser>> resp = new ResponseEntity<>(entityModelUser, HttpStatus.OK);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(resp);
        Mockito.when(beanFactoryMock.getBean(IProjectUsersClient.class)).thenReturn(projectUsersClientMock);

        jwtService = Mockito.mock(JWTService.class);

        oauth2AuthenticationService = new Oauth2AuthenticationService(runtimeTenantResolver,
                                                                      projectUsersClientMock,
                                                                      pluginServiceMock,
                                                                      projectsClientMock,
                                                                      accountsClientMock,
                                                                      userAccountManagerMock,
                                                                      jwtService,
                                                                      "root@test.fr");

    }

    /**
     * Check that an account is created after success authentication by plugin if account does not exits
     */
    @Purpose("Check that an account is created after success authentication by plugin if account does not exits")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testNewAccountAuthentication() {
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        // Mock return a valid account
        Mockito.when(accountsClientMock.retrieveAccountByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        try {
            oauth2AuthenticationService.doAuthentication("name", "password", "tenant");
            //            oauth2AuthenticationService.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (AuthenticationException e) {
            Assert.assertEquals(e.getAdditionalInformation().get("error"),
                                AuthenticationStatus.ACCOUNT_UNKNOWN.toString());
        }
        // Account creation is done only for plugins. No for regards internal plugin
        Mockito.verify(userAccountManagerMock, Mockito.times(0)).createUserWithAccountAndGroups(any(), any());
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin
     */
    @Purpose("Check valid authentication process.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testAuthentication() {
        // Mock valid Authentication parameters
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("test@regards.fr");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        // Mock a valid project user
        final EntityModel<ProjectUser> entityModelUser = EntityModel.of(validUser);
        final ResponseEntity<EntityModel<ProjectUser>> resp = new ResponseEntity<>(entityModelUser, HttpStatus.OK);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(resp);

        // Mock a valid account
        final EntityModel<Account> entityModel = EntityModel.of(validAccount);
        Mockito.when(accountsClientMock.retrieveAccountByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(entityModel, HttpStatus.OK));

        Mockito.when(jwtService.generateToken(Mockito.anyString(),
                                              Mockito.anyString(),
                                              Mockito.anyString(),
                                              Mockito.anyString(), Mockito.any(),
                                              Mockito.anyMap(),
                                              Mockito.anyString(), Mockito.anyBoolean())).thenReturn("token");
        Mockito.when(jwtService.getExpirationDate(Mockito.any())).thenReturn(OffsetDateTime.now().plusHours(1));


        // Run authentication process
        oauth2AuthenticationService.doAuthentication("test@regards.fr", "password", "tenant");
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin
     */
    @Purpose("Check error authentication process with projectUser not defined.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testAuthenticationUserUnknown() {
        // Mock valid Authentication parameters
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("test@regards.fr");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        // Mock unknown user
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        // Run authentication process
        try {
            oauth2AuthenticationService.doAuthentication("test@regards.fr", "password", "tenant");
            //            oauth2AuthenticationService.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (AuthenticationException e) {
            Assert.assertEquals(e.getAdditionalInformation().get("error"),
                                AuthenticationStatus.USER_UNKNOWN.toString());
        }
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin
     */
    @Purpose("Check error authentication process with projectUser not validated yet.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testAuthenticationUserPending() {
        // Mock valid Authentication parameters
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("test@regards.fr");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        // Mock pending user
        final ProjectUser user = new ProjectUser();
        user.setEmail("test@regards.fr");
        user.setRole(new Role());
        user.setStatus(UserStatus.WAITING_ACCESS);

        final EntityModel<ProjectUser> entityModel = EntityModel.of(user);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(entityModel, HttpStatus.OK));

        // Run authentication process
        try {
            oauth2AuthenticationService.doAuthentication("test@regards.fr", "password", "tenant");
            //            oauth2AuthenticationService.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (AuthenticationException e) {
            Assert.assertEquals(e.getAdditionalInformation().get("error"),
                                AuthenticationStatus.USER_WAITING_ACCESS.toString());
        }
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin
     */
    @Purpose("Error during oauth2 authentication. Default authentication plugin. Invalid authentication parameters.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testIncompleteAuthentication() {
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn(null);
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        try {
            oauth2AuthenticationService.doAuthentication(null, "password", "tenant");
            //            oauth2AuthenticationService.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (BadCredentialsException e) {
            // Nothing to do
        }
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin. Invalid authentication
     * parameters.
     */
    @Purpose("Error during oauth2 authentication. Default authentication plugin. Invalid authentication parameters.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testIncompleteAuthentication2() {
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("no_scope_param", "value");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        try {
            oauth2AuthenticationService.doAuthentication("name", "password", null);
            //            oauth2AuthenticationService.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (BadCredentialsException e) {
            // Nothing to do
        }
    }

}
