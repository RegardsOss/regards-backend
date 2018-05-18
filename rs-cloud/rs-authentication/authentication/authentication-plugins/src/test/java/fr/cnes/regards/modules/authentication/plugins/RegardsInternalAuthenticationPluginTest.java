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
package fr.cnes.regards.modules.authentication.plugins;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.plugins.regards.RegardsInternalAuthenticationPlugin;

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
            plugin = PluginUtils.getPlugin(parameters,
                                           RegardsInternalAuthenticationPlugin.class,
                                           Arrays.asList(
                                                   "fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos"),
                                           new HashMap<>());
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsRuntimeException | IllegalArgumentException | SecurityException e) {
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
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Purpose("Check a valid authentication throught the Regards internal authentication system")
    @Test
    public void testValidAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
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
        Assert.assertTrue(response.getAccessGranted());

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
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testErrorAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(Boolean.FALSE, HttpStatus.OK);
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
        Assert.assertFalse(response.getAccessGranted());

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
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testRequestErrorAuthentication() throws EntityNotFoundException {

        Field privateField;
        try {

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
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
        Assert.assertFalse(response.getAccessGranted());

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
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testRequestErrorAuthenticationException() throws EntityNotFoundException {

        Field privateField;
        try {

            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
        Assert.assertFalse(response.getAccessGranted());

    }
}
