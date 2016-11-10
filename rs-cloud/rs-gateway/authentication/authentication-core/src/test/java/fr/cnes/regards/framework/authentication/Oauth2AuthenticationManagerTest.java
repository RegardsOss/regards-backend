/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class Oauth2AuthenticationManagerTest
 *
 * Test Regards Oauth2 authentication process
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
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
     *
     * Mocks initialization
     *
     * @throws EntityException
     *             test error.
     * @since 1.0-SNAPSHOT
     */
    @BeforeClass
    public static void init() throws EntityException {

        // Create mock for default authentication plugin
        final IAuthenticationPlugin plugin = Mockito.mock(IAuthenticationPlugin.class);
        Mockito.when(plugin.authenticate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new AuthenticationPluginResponse(AuthenticationStatus.ACCESS_GRANTED, "test@regards.fr"));

        // Create mock for default jwt service
        final JWTService service = Mockito.mock(JWTService.class);

        // Create manager
        manager = new Oauth2AuthenticationManager("test", plugin, service);

        // Mock Spring beanFactory to provide needed beans
        final BeanFactory beanFactoryMock = Mockito.mock(BeanFactory.class);

        // Mock Plugins service
        final IPluginService pluginServiceMock = Mockito.mock(IPluginService.class);
        Mockito.when(beanFactoryMock.getBean(IPluginService.class)).thenReturn(pluginServiceMock);

        // Mock Administration Projects client
        final IProjectsClient projectsClientMock = Mockito.mock(IProjectsClient.class);
        final ResponseEntity<Resource<Project>> response = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(projectsClientMock.retrieveProject(Mockito.anyString())).thenReturn(response);
        Mockito.when(beanFactoryMock.getBean(IProjectsClient.class)).thenReturn(projectsClientMock);

        final IProjectUsersClient projectUsersClientMock = Mockito.mock(IProjectUsersClient.class);
        final ProjectUser user = new ProjectUser();
        final Role role = new Role();
        role.setName("USER");
        user.setEmail("test@regards.fr");
        user.setRole(role);
        final Resource<ProjectUser> resource = new Resource<>(user);
        final ResponseEntity<Resource<ProjectUser>> resp = new ResponseEntity<Resource<ProjectUser>>(resource,
                HttpStatus.OK);
        Mockito.when(projectUsersClientMock.retrieveProjectUser(Mockito.anyString())).thenReturn(resp);
        Mockito.when(beanFactoryMock.getBean(IProjectUsersClient.class)).thenReturn(projectUsersClientMock);

        Field privateField;
        try {
            privateField = Oauth2AuthenticationManager.class.getDeclaredField("beanFactory");
            privateField.setAccessible(true);
            privateField.set(manager, beanFactoryMock);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check oauth2 authentication process using default authentication plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check oauth2 authentication process using default authentication plugin")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testOauth2AuthenticationDefaultProcess() {
        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("scope", "tenant");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        final Authentication authResult = manager.authenticate(auth);
        Assert.assertNotNull(authResult);
        Assert.assertTrue(authResult.isAuthenticated());
    }

    /**
     *
     * Check error during oauth2 authentication process using default authentication plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check error during oauth2 authentication process using default authentication plugin")
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
            final Authentication authResult = manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
            // Nothing to do
        }
    }

    /**
     *
     * Check error during oauth2 authentication process using default authentication plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check error during oauth2 authentication process using default authentication plugin")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testIncompleteAuthentication2() {

        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        final Map<String, String> mockedDetails = new HashMap<>();
        mockedDetails.put("exemple", "value");
        Mockito.when(auth.getDetails()).thenReturn(mockedDetails);

        try {
            final Authentication authResult = manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
            // Nothing to do
        }
    }

    /**
     *
     * Check error during oauth2 authentication process using default authentication plugin
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check error during oauth2 authentication process using default authentication plugin")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testIncompleteAuthentication3() {

        auth = Mockito.mock(JWTAuthentication.class);
        Mockito.when(auth.getName()).thenReturn("name");
        Mockito.when(auth.getCredentials()).thenReturn("password");
        Mockito.when(auth.getDetails()).thenReturn(null);

        try {
            final Authentication authResult = manager.authenticate(auth);
            Assert.fail("There should be an error here");
        } catch (final BadCredentialsException e) {
            // Nothing to do
        }
    }

}
