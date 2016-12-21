/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.regards.RegardsInternalAuthenticationPlugin;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Class RegardsInternalAuthenticationPluginTest
 *
 * Test plugin for Regards internal authentication
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class RegardsInternalAuthenticationPluginTest {

    private static RegardsInternalAuthenticationPlugin plugin;

    /**
     *
     * Initialize Authentication plugin thought plugin utilities.
     *
     * @since 1.0-SNAPSHOT
     */
    @BeforeClass
    public static void init() {

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = new ArrayList<>();
        try {
            // instantiate plugin
            plugin = PluginUtils
                    .getPlugin(parameters, RegardsInternalAuthenticationPlugin.class,
                               Arrays.asList("fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos"));
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }

    }

    /**
     *
     * Check a valid authentication throught the Regards internal authentication system
     *
     * @throws EntityNotFoundException
     *             test error.
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Purpose("Check a valid authentication throught the Regards internal authentication system")
    @Test
    public void testValidAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {
            final JWTService service = new JWTService();
            service.setSecret("123456789");
            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("jwtService");
            privateField.setAccessible(true);
            privateField.set(plugin, service);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("microserviceName");
            privateField.setAccessible(true);
            privateField.set(plugin, "test");

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<AccountStatus> response = new ResponseEntity<>(AccountStatus.ACTIVE, HttpStatus.OK);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getEmail().equals("test@regards.fr"));
        Assert.assertNull(response.getErrorMessage());
        Assert.assertTrue(response.getStatus().equals(AuthenticationStatus.ACCESS_GRANTED));

    }

    /**
     *
     * Check a authentication throught the Regards internal authentication system with error
     *
     * @throws EntityNotFoundException
     *             test error.
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check a authentication throught the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Test
    public void testErrorAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {
            final JWTService service = new JWTService();
            service.setSecret("123456789");
            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("jwtService");
            privateField.setAccessible(true);
            privateField.set(plugin, service);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("microserviceName");
            privateField.setAccessible(true);
            privateField.set(plugin, "test");

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<AccountStatus> response = new ResponseEntity<>(AccountStatus.INACTIVE,
                    HttpStatus.UNAUTHORIZED);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getErrorMessage());
        Assert.assertTrue(response.getStatus().equals(AuthenticationStatus.ACCESS_DENIED));

    }

    /**
     *
     * Check a authentication throught the Regards internal authentication system with error
     *
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check a authentication throught the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Test
    public void testRequestErrorAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {
            final JWTService service = new JWTService();
            service.setSecret("123456789");
            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("jwtService");
            privateField.setAccessible(true);
            privateField.set(plugin, service);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("microserviceName");
            privateField.setAccessible(true);
            privateField.set(plugin, "test");

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<AccountStatus> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getErrorMessage());
        Assert.assertTrue(response.getStatus().equals(AuthenticationStatus.ACCESS_DENIED));

    }

    /**
     *
     * Check a authentication throught the Regards internal authentication system with error
     *
     * @throws EntityNotFoundException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check a authentication throught the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Test
    public void testRequestErrorAuthenticationException() throws EntityNotFoundException {

        Field privateField;
        try {
            final JWTService service = new JWTService();
            service.setSecret("123456789");
            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("jwtService");
            privateField.setAccessible(true);
            privateField.set(plugin, service);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("microserviceName");
            privateField.setAccessible(true);
            privateField.set(plugin, "test");

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<AccountStatus> response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getErrorMessage());
        Assert.assertTrue(response.getStatus().equals(AuthenticationStatus.ACCOUNT_NOT_FOUND));

    }
}
