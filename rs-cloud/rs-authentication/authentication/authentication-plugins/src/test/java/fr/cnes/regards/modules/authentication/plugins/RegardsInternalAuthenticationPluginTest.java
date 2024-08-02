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
package fr.cnes.regards.modules.authentication.plugins;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.plugins.identityprovider.regards.RegardsInternalAuthenticationPlugin;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class RegardsInternalAuthenticationPluginTest
 * <p>
 * Test plugin for Regards internal authentication
 *
 * @author Sébastien Binda
 */
public class RegardsInternalAuthenticationPluginTest {

    private static RegardsInternalAuthenticationPlugin plugin;

    /**
     * Initialize Authentication plugin thought plugin utilities.
     */
    @BeforeClass
    public static void init() {

        PluginUtils.setup();
        /*
         * Set all parameters
         */
        Set<IPluginParam> parameters = Sets.newHashSet();
        try {
            PluginConfiguration conf = PluginConfiguration.build(RegardsInternalAuthenticationPlugin.class,
                                                                 "",
                                                                 parameters);
            // instantiate plugin
            plugin = PluginUtils.getPlugin(conf, new ConcurrentHashMap<>());
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsRuntimeException | IllegalArgumentException | SecurityException |
                       NotAvailablePluginConfigurationException e) {
            Assert.fail();
        }

    }

    /**
     * Check a valid authentication through the Regards internal authentication system
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Purpose("Check a valid authentication through the Regards internal authentication system")
    @Test
    public void testValidAuthentication() {

        Field privateField;
        try {

            String email = "test@regards.fr";
            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);
            Account account = new Account(email, "firstName", "lastName", "password");
            account.setOrigin(Account.REGARDS_ORIGIN);
            final ResponseEntity<EntityModel<Account>> accountResponse = new ResponseEntity<>(EntityModel.of(account),
                                                                                              HttpStatus.OK);
            Mockito.when(client.retrieveAccountByEmail(email)).thenReturn(accountResponse);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertEquals("test@regards.fr", response.getEmail());
        Assert.assertNull(response.getErrorMessage());
        Assert.assertTrue(response.isAccessGranted());

    }

    @Test
    public void testAuthenticationWithEternalAccount() {

        Field privateField;
        try {

            String email = "test@regards.fr";
            Account extAccount = new Account(email, "firstName", "lastName", "password");
            extAccount.setOrigin("origin");
            final IAccountsClient client = Mockito.mock(IAccountsClient.class);
            final ResponseEntity<Boolean> response = new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
            Mockito.when(client.validatePassword(Mockito.anyString(), Mockito.anyString())).thenReturn(response);
            final ResponseEntity<EntityModel<Account>> accountResponse = new ResponseEntity<>(EntityModel.of(extAccount),
                                                                                              HttpStatus.OK);
            Mockito.when(client.retrieveAccountByEmail(email)).thenReturn(accountResponse);

            privateField = RegardsInternalAuthenticationPlugin.class.getDeclaredField("accountsClient");
            privateField.setAccessible(true);
            privateField.set(plugin, client);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        final AuthenticationPluginResponse response = plugin.authenticate("test@regards.fr", "password", "test1");
        Assert.assertNotNull(response);
        Assert.assertEquals("test@regards.fr", response.getEmail());
        Assert.assertNotNull(response.getErrorMessage());
        Assert.assertFalse(response.isAccessGranted());

    }

    /**
     * Check an authentication through the Regards internal authentication system with error
     */
    @Purpose("Check a authentication through the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testErrorAuthentication() {

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
        Assert.assertFalse(response.isAccessGranted());

    }

    /**
     * Check an authentication through the Regards internal authentication system with error
     */
    @Purpose("Check a authentication through the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testRequestErrorAuthentication() {

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
        Assert.assertFalse(response.isAccessGranted());

    }

    /**
     * Check an authentication through the Regards internal authentication system with error
     */
    @Purpose("Check a authentication through the Regards internal authentication system with error")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Requirement("REGARDS_DSL_ADM_ADM_620")
    @Requirement("REGARDS_DSL_ADM_PLG_200")
    @Test
    public void testRequestErrorAuthenticationException() {

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
        Assert.assertFalse(response.isAccessGranted());

    }
}
