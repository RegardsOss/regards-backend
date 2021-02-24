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
package fr.cnes.regards.modules.authentication.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.authentication.plugins.identityprovider.ldap.LdapAuthenticationPlugin;

/**
 * Class LdapAuthenticationPluginTest
 *
 * Test authentication through LDAP server
 * @author Sébastien Binda
 */
public class LdapAuthenticationPluginTest {

    /**
     * Test email to return by ldap mock
     */
    private static final String EMAIL = "test@regards.fr";

    /**
     * LDAP Authentication plugin to test
     */
    private static LdapAuthenticationPlugin plugin;

    /**
     * Initialize LDAP Authentication plugin thought plugin utilities.
     */
    @BeforeClass
    public static void init() {

        PluginUtils.setup();

        /*
         * Set all parameters
         */
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_HOST, "test"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_PORT, "8080"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_CN, "ou=people,ou=commun"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_USER_EMAIL_ATTTRIBUTE, "email"));
        try {
            PluginConfiguration conf = PluginConfiguration.build(LdapAuthenticationPlugin.class, "", parameters);
            // instantiate plugin
            plugin = PluginUtils.getPlugin(conf, new HashMap<>());
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsRuntimeException | NotAvailablePluginConfigurationException e) {
            Assert.fail();
        }
    }

    /**
     * Test valid authentication throught LDAP plugin
     */
    @Purpose("Test valid authentication throught LDAP plugin")
    @Test
    public void testLdapAuthentication() {

        Mockito.mock(LdapNetworkConnection.class);

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);
        final LdapConnection mockedConnection = getMockedLdapConnection(true);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Error authentication. Access should be granted.", response.getAccessGranted());
        Assert.assertEquals("Error authentication. Email is not valid", response.getEmail(), EMAIL);

    }

    /**
     * Test error authentication throught LDAP plugin
     * @throws LdapException test error.
     * @throws IOException   test error.
     */
    @Purpose("Test error authentication throught LDAP plugin")
    @Test
    public void testErrorConnection() throws LdapException, IOException {
        final LdapConnection mockedConnection = Mockito.mock(LdapConnection.class);
        Mockito.when(mockedConnection.connect()).thenReturn(false);

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("The authentication shoul not be granted.", !response.getAccessGranted());
    }

    /**
     * Test error authentication throught LDAP plugin
     * @throws LdapException test error.
     * @throws IOException   test error.
     */
    @Purpose("Test error authentication throught LDAP plugin")
    @Test
    public void testErrorAuthentication() throws LdapException, IOException {
        final LdapConnection mockedConnection = Mockito.mock(LdapConnection.class);
        Mockito.when(mockedConnection.connect()).thenReturn(true);
        Mockito.when(mockedConnection.isAuthenticated()).thenReturn(false);

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("The authentication shoul not be granted.", !response.getAccessGranted());
    }

    /**
     * Test error authentication throught LDAP plugin
     */
    @Purpose("Test error authentication throught LDAP plugin")
    @Test
    public void testErrorInvalidEmail() {
        Mockito.mock(LdapNetworkConnection.class);

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);
        final LdapConnection mockedConnection = getMockedLdapConnection(false);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("The authentication shoul not be granted.", !response.getAccessGranted());
    }

    /**
     * Test error authentication throught LDAP plugin
     * @throws LdapException test error.
     * @throws IOException   test error.
     */
    @Purpose("Test error authentication throught LDAP plugin")
    @Test
    public void testErrorLdapException() throws LdapException, IOException {
        final LdapConnection mockedConnection = Mockito.mock(LdapConnection.class);
        Mockito.when(mockedConnection.connect()).thenThrow(new LdapException("ldap exception test"));

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("The authentication shoul not be granted.", !response.getAccessGranted());

    }

    /**
     * Test error authentication throught LDAP plugin
     * @throws LdapException test error.
     * @throws IOException   test error.
     */
    @Purpose("Test error authentication throught LDAP plugin")
    @Test
    public void testErrorLdapException2() throws LdapException, IOException {
        final LdapConnection mockedConnection = Mockito.mock(LdapConnection.class);
        Mockito.when(mockedConnection.connect()).thenThrow(new LdapAuthenticationException("ldap exception test"));

        final LdapAuthenticationPlugin spy = Mockito.spy(plugin);

        Mockito.doReturn(mockedConnection).when(spy).getLdapConnection(Mockito.anyString(), Mockito.anyInt());

        final AuthenticationPluginResponse response = spy.authenticate("login", "password", "project");
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("The authentication shoul not be granted.", !response.getAccessGranted());

    }

    /**
     * Create LDAP connection mock for test
     * @param pValidEmail does the LDAP mock connection return a valid email or not
     * @return LdapConnection
     */
    private LdapConnection getMockedLdapConnection(final boolean pValidEmail) {

        final LdapConnection mockedConnection = Mockito.mock(LdapConnection.class);

        try {
            Mockito.when(mockedConnection.connect()).thenReturn(true);
            Mockito.when(mockedConnection.isAuthenticated()).thenReturn(true);

            final List<Entry> entries = new ArrayList<>();

            final Attribute mockedAttribute = Mockito.mock(Attribute.class);
            if (pValidEmail) {
                Mockito.when(mockedAttribute.getString()).thenReturn(EMAIL);
            }

            final Entry mockedEntry = Mockito.mock(Entry.class);
            Mockito.when(mockedEntry.get(Mockito.anyString())).thenReturn(mockedAttribute);

            entries.add(mockedEntry);
            final EntryCursorStub entry = new EntryCursorStub();
            entry.setEntries(entries);

            Mockito.when(mockedConnection.search(Mockito.anyString(), Mockito.anyString(),
                                                 Mockito.any(SearchScope.class), Mockito.anyString()))
                    .thenReturn(entry);
        } catch (LdapException | IOException e) {
            Assert.fail(e.getMessage());
        }

        return mockedConnection;

    }

}
