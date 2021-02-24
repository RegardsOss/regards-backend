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
package fr.cnes.regards.framework.authentication;

import fr.cnes.regards.framework.authentication.exception.AuthenticationException;
import fr.cnes.regards.framework.authentication.internal.AuthenticationStatus;
import fr.cnes.regards.framework.authentication.internal.Oauth2AuthenticationManager;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Class Oauth2AuthenticationManagerTest
 *
 * Test Regards Oauth2 authentication process
 * @author Sébastien Binda
 */
public class Oauth2AuthenticationManagerTest {

    /**
     * Authentication manager to test
     */
    private static Oauth2AuthenticationManager manager;

    /**
     * JWT authentication to use during test
     */
    private static JWTAuthentication auth;

    /**
     * Mock for accounts client from administration service.
     */
    private static IAccountsClient accountsClientMock;

    /**
     * Mock for new registration
     */
    private static IRegistrationClient registrationClientMock;

    /**
     * Mock to retrieve project users
     */
    private static IProjectUsersClient projectUsersClientMock;

    /**
     * Mock for authentication plugin
     */
    private static IAuthenticationPlugin plugin;

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

        // Create mock for default jwt service
        final IRuntimeTenantResolver service = Mockito.mock(IRuntimeTenantResolver.class);

        // Create manager
        manager = new Oauth2AuthenticationManager(plugin, service, "root@test.fr");

        // Initialize valid account
        validAccount = new Account("test@regards.fr", "test", "test", "test");
        validAccount.setStatus(AccountStatus.ACTIVE);

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
        final EntityModel<Account> EntityModel = new EntityModel<>(validAccount);
        Mockito.when(accountsClientMock.retrieveAccounByEmail(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(EntityModel, HttpStatus.OK));

        registrationClientMock = Mockito.mock(IRegistrationClient.class);

        Mockito.when(beanFactoryMock.getBean(IAccountsClient.class)).thenReturn(accountsClientMock);
        Mockito.when(beanFactoryMock.getBean(IRegistrationClient.class)).thenReturn(registrationClientMock);

        projectUsersClientMock = Mockito.mock(IProjectUsersClient.class);
        final EntityModel<ProjectUser> EntityModelUser = new EntityModel<>(validUser);
        final ResponseEntity<EntityModel<ProjectUser>> resp = new ResponseEntity<>(EntityModelUser, HttpStatus.OK);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(resp);
        Mockito.when(beanFactoryMock.getBean(IProjectUsersClient.class)).thenReturn(projectUsersClientMock);

        try {
            final Field privateField = Oauth2AuthenticationManager.class.getDeclaredField("beanFactory");
            privateField.setAccessible(true);
            privateField.set(manager, beanFactoryMock);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
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
        Mockito.when(accountsClientMock.retrieveAccounByEmail(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        try {
            manager.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (final AuthenticationException e) {
            Assert.assertEquals(e.getAdditionalInformation().get("error"),
                                AuthenticationStatus.ACCOUNT_UNKNOWN.toString());
        }

        // Account creation is done only for plugins. No for regards internal plugin
        Mockito.verify(registrationClientMock, Mockito.times(0)).requestAccess(Mockito.any());
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
        final EntityModel<ProjectUser> EntityModelUser = new EntityModel<>(validUser);
        final ResponseEntity<EntityModel<ProjectUser>> resp = new ResponseEntity<>(EntityModelUser, HttpStatus.OK);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(resp);

        // Mock a valid account
        final EntityModel<Account> EntityModel = new EntityModel<>(validAccount);
        Mockito.when(accountsClientMock.retrieveAccounByEmail(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(EntityModel, HttpStatus.OK));

        // Run authentication process
        manager.authenticate(auth);
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
            manager.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (final AuthenticationException e) {
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

        final EntityModel<ProjectUser> EntityModel = new EntityModel<>(user);
        Mockito.when(projectUsersClientMock.retrieveProjectUserByEmail(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(EntityModel, HttpStatus.OK));

        // Run authentication process
        try {
            manager.authenticate(auth);
            Assert.fail("There should be an AuthenticationException thronw here");
        } catch (final AuthenticationException e) {
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
            manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
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
            manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
            // Nothing to do
        }
    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin. Invalid authentication
     * parameters.
     */
    @Purpose("Error during oauth2 authentication. Default authentication plugin. No authentication parameters.")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testIncompleteAuthentication3() {

        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        Mockito.when(auth.getDetails()).thenReturn(null);

        try {
            manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
            // Nothing to do
        }
    }

}
