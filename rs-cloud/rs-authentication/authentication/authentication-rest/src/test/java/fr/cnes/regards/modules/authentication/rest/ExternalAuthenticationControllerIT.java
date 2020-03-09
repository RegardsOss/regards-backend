/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.modules.authentication.plugins.impl.kerberos.KerberosSPParameters;
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
     * Default plugin label
     */
    private static final String DEFAULT_PLUGIN_LABEL = "plugin1";

    /**
     * Repository stub
     */
    @Autowired
    private IPluginService pluginService;

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
    public void init() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(KerberosSPParameters.PRINCIPAL_PARAMETER, "principal"),
                     IPluginParam.build(KerberosSPParameters.LDAP_ADRESS_PARAMETER, "test"),
                     IPluginParam.build(KerberosSPParameters.LDAP_PORT_PARAMETER, "8080"),
                     IPluginParam.build(KerberosSPParameters.PARAM_LDAP_CN, "ou=people,ou=commun"),
                     IPluginParam.build(KerberosSPParameters.PARAM_LDAP_EMAIL_ATTTRIBUTE, "email"),
                     IPluginParam.build(KerberosSPParameters.REALM_PARAMETER, "realm"),
                     IPluginParam.build(KerberosSPParameters.PARAM_LDAP_USER_FILTER_ATTTRIBUTE, "userfilter"),
                     IPluginParam.build(KerberosSPParameters.PARAM_LDAP_USER_LOGIN_ATTTRIBUTE, "userLogin"),
                     IPluginParam.build(KerberosSPParameters.KRB5_FILEPATH_PARAMETER, "krb5FilePath"),
                     IPluginParam.build(KerberosSPParameters.KEYTAB_FILEPATH_PARAMETER, "keytabFilePath"));

        PluginConfiguration conf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                           parameters,
                                                           0,
                                                           KerberosServiceProviderPlugin.class
                                                                   .getAnnotation(Plugin.class).id());
        aPluginConfSaved = pluginService.savePluginConfiguration(conf);
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
        performDefaultGet(SPS_URL + "/{sp_id}",
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS),
                          "retrieveServiceProvider : Error getting Service provider", aPluginConfSaved.getId().toString());
    }

    /**
     * Integration test to retrieve one configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to retrieve one configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveInexistantServiceProvider() {
        performDefaultGet(SP_URL,
                          customizer().expectStatusNotFound(),
                          "retrieveInexistantServiceProvider : Error getting Service provider",
                          123);
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
        PluginConfiguration conf = new PluginConfiguration("Plugin2",
                                                           0,
                                                           KerberosServiceProviderPlugin.class
                                                                   .getAnnotation(Plugin.class).id());
        conf.setId(1L);

        performDefaultPost(SPS_URL,
                           conf,
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

        performDefaultPut(SP_URL,
                          aPluginConfSaved,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS)
                                  .expectValue(JSON_PATH_CONTENT + ".version", newVersion),
                          "updateServiceProvider : Error getting Service provider",
                          aPluginConfSaved.getId());
    }

    /**
     * Integration test to update a configured Service Provider plugin of the Authentication module with error
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInexistantServiceProvider() {
        PluginConfiguration unSavedPluginConf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                                        0,
                                                                        KerberosServiceProviderPlugin.class
                                                                                .getAnnotation(Plugin.class).id());
        unSavedPluginConf.setId(12345L);
        performDefaultPut(SP_URL,
                          unSavedPluginConf,
                          customizer().expectStatusNotFound(),
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
        PluginConfiguration conf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                           0,
                                                           KerberosServiceProviderPlugin.class
                                                                   .getAnnotation(Plugin.class).id());
        conf.setId(123L);
        performDefaultPut(SP_URL,
                          conf,
                          customizer().expectStatusBadRequest(),
                          "updateInvalidServiceProvider : Error getting Service provider",
                          12);
    }

    /**
     * Integration test to delete a configured Service Provider plugin of the Authentication module
     */
    @Purpose("Integration test to delete a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteIdentityProvider() {
        performDefaultDelete(SP_URL,
                             customizer().expectStatusOk(),
                             "deleteIdentityProvider : Error getting Service provider",
                             aPluginConfSaved.getBusinessId());
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

        ExternalAuthenticationInformations infos = new ExternalAuthenticationInformations("usernma",
                                                                                          getDefaultTenant(),
                                                                                          "ticket".getBytes(),
                                                                                          "key");

        performDefaultPost("/authentication/sps/0/authenticate",
                           infos,
                           customizer().expectStatusOk(),
                           "kerberos authenticate : Authentication error");

    }

}
