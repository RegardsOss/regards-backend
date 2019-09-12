/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.modules.authentication.plugins.impl.kerberos.KerberosServiceProviderPlugin;

/**
 * Class AuthenticationControllerIT Test REST endpoints to manage Service provider plugins
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
// @EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
// @ComponentScan("fr.cnes.regards.framework.authentication")
@MultitenantTransactional
public class ExternalAuthenticationControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ExternalAuthenticationControllerIT.class);

    /**
     * Access route
     */
    private static final String SPS_URL = "/authentication/sps";

    /**
     * Access route
     */
    private static final String SP_URL = "/authentication/sps/{sp_id}";

    /**
     * Url to authenticate
     */
    @SuppressWarnings("unused")
    private static final String AUTHENTICATE_URL = "/authentication/sps/{sp_id}/authenticate";

    /**
     * Default plugin version
     */
    private static final String DEFAULT_PLUGIN_VERSION = "1.0";

    /**
     * Default plugin label
     */
    private static final String DEFAULT_PLUGIN_LABEL = "plugin1";

    /**
     * LDAP plugin id
     */
    private static final String PLUGIN_ID_KERBEROS = "KerberosServiceProviderPlugin";

    /**
     * Repository stub
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    /**
     * A {@link PluginConfiguration} used in the test
     */
    private PluginConfiguration aPluginConfSaved;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Init the context of the tests
     */
    @Before
    public void init() {
        PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.getInterfaceNames().add(IServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        aPluginConfSaved = pluginConfRepo.save(conf);
    }

    /**
     * Integration test to retrieve all configured Service Provider plugins of the Authentication module
     */
    @Purpose("Integration test to retrieve all configured Service Provider plugins of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveServiceProviders() {
        performDefaultGet(SPS_URL,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT).expectIsArray(JSON_PATH_ROOT),
                          "Error getting Service providers");
    }

    /**
     * Integration test to retrieve one configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to retrieve one configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveServiceProvider() {
        performDefaultGet(SPS_URL + URL_PATH_SEPARATOR + aPluginConfSaved.getId().toString(),
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS),
                          "retrieveServiceProvider : Error getting Service provider");
    }

    /**
     * Integration test to retrieve one configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to retrieve one configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveInexistantServiceProvider() {
        performDefaultGet(SP_URL, customizer().expectStatusNotFound(),
                          "retrieveInexistantServiceProvider : Error getting Service provider", 123);
    }

    /**
     * Integration test to create a configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to create a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    @Ignore("FIXME V3.0.0 : plugin validation")
    public void createServiceProvider() {

        PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration conf = new PluginConfiguration(metadata, "Plugin2", 0);
        conf.setId(1L);

        performDefaultPost(SPS_URL, conf,
                           customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                   .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS),
                           "createServiceProvider : Error getting Service provider");
    }

    /**
     * Integration test to update a configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    @Ignore("FIXME V3.0.0 : plugin validation")
    public void updateServiceProvider() {
        String newVersion = "2.0";
        aPluginConfSaved.setVersion(newVersion);

        performDefaultPut(SP_URL, aPluginConfSaved,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS)
                                  .expectValue(JSON_PATH_CONTENT + ".version", newVersion),
                          "updateServiceProvider : Error getting Service provider", aPluginConfSaved.getId());
    }

    /**
     * Integration test to update a configured Service Provider plugin of the Authentication module with error
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInexistantServiceProvider() {
        PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration unSavedPluginConf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        unSavedPluginConf.setId(12345L);
        performDefaultPut(SP_URL, unSavedPluginConf, customizer().expectStatusNotFound(),
                          "updateInexistantServiceProvider : Error getting Service provider",
                          unSavedPluginConf.getId());
    }

    /**
     * Integration test to update a configured Service Provider plugin of the Authentication module with eror
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module with eror")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInvalidServiceProvider() {
        PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(123L);
        performDefaultPut(SP_URL, conf, customizer().expectStatusBadRequest(),
                          "updateInvalidServiceProvider : Error getting Service provider", 12);
    }

    /**
     * Integration test to delete a configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to delete a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteIdentityProvider() {
        PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.getInterfaceNames().add("fr.cnes.regards.framework.some.modules.PluginToDelete");
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration aPluginConfToDelete = new PluginConfiguration(metadata, "PluginToDelete", 0);
        aPluginConfToDelete = pluginConfRepo.save(aPluginConfToDelete);
        performDefaultDelete(SP_URL, customizer().expectStatusOk(),
                             "deleteIdentityProvider : Error getting Service provider",
                             aPluginConfToDelete.getBusinessId());
    }

    /**
     * Integration test to delete a configured Service Provider plugin of the Authentication module with error
     */
    @Purpose("Integration test to delete a configured Service Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteInexistantIndentityProvider() {
        performDefaultDelete(SP_URL, customizer().expectStatusNotFound(), "Error getting Service provider", "plop");

    }

    @Ignore
    @Test
    public void authenticateKerberosServiceProvider() {

        ExternalAuthenticationInformations infos = new ExternalAuthenticationInformations("usernma", getDefaultTenant(),
                "ticket".getBytes(), "key");

        performDefaultPost("/authentication/sps/0/authenticate", infos, customizer().expectStatusOk(),
                           "kerberos authenticate : Authentication error");

    }

}
