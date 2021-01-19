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
package fr.cnes.regards.modules.authentication.rest;

import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.authentication.plugins.impl.ldap.LdapAuthenticationPlugin;

/**
 * Class AuthenticationControllerIT Test REST endpoints to manage Identity provider plugins
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = FeignMockConfiguration.class)
@MultitenantTransactional
public class AuthenticationControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationControllerIT.class);

    /**
     * Access route
     */
    private static final String IDP_URL = InternalAuthenticationController.TYPE_MAPPING + "/{idp_id}";
    
    /**
     * Default plugin label
     */
    private static final String DEFAULT_PLUGIN_LABEL = "plugin1";

    /**
     * Repository stub
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

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
        PluginConfiguration conf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                           0,
                                                           LdapAuthenticationPlugin.class.getAnnotation(Plugin.class)
                                                                   .id());

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_HOST, "test"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_PORT, "8080"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_CN, "ou=people,ou=commun"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_USER_EMAIL_ATTTRIBUTE, "email"));

        conf.setParameters(parameters);
        aPluginConfSaved = pluginService.savePluginConfiguration(conf);
    }

    /**
     * Integration test to retrieve all configured Identity Provider plugins of the Authentication module
     */
    @Purpose("Integration test to retrieve all configured Identity Provider plugins of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveIdentityProviders() {
        performDefaultGet(InternalAuthenticationController.TYPE_MAPPING,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT).expectIsArray(JSON_PATH_ROOT),
                          "Error getting identity providers");
    }

    /**
     * Integration test to retrieve one configured Identity Provider plugin of the Authentication module
     */
    @Purpose("Integration test to retrieve one configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveIdentityProvider() {
        performDefaultGet(IDP_URL,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS),
                          "retrieveIdentityProvider : Error getting identity provider",
                          aPluginConfSaved.getId());
    }

    /**
     * Integration test to retrieve one configured Identity Provider plugin of the Authentication module
     */
    @Purpose("Integration test to retrieve one configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveInexistantIdentityProvider() {
        performDefaultGet(IDP_URL,
                          customizer().expectStatusNotFound(),
                          "retrieveInexistantIdentityProvider : Error getting identity provider",
                          123);
    }

    /**
     * Integration test to create a configured Identity Provider plugin of the Authentication module
     */
    @Purpose("Integration test to create a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void createIdentityProvider() {
        PluginConfiguration conf = new PluginConfiguration("Plugin2",
                                                           0,
                                                           LdapAuthenticationPlugin.class.getAnnotation(Plugin.class)
                                                                   .id());

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_HOST, "test"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_PORT, "8080"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_CN, "ou=people,ou=commun"),
                     IPluginParam.build(LdapAuthenticationPlugin.PARAM_LDAP_USER_EMAIL_ATTTRIBUTE, "email"));

        conf.setParameters(parameters);

        performDefaultPost(InternalAuthenticationController.TYPE_MAPPING,
                           conf,
                           customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                   .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS),
                           "createIdentityProvider : Error getting identity provider");
    }

    /**
     * Integration test to update a configured Identity Provider plugin of the Authentication module
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    @Ignore("FIXME : manage version upgrade")
    public void updateIdentityProvider() {
        String newVersion = "2.0";
        aPluginConfSaved.setVersion(newVersion);
        performDefaultPut(IDP_URL,
                          aPluginConfSaved,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_CONTENT)
                                  .expectIsNotEmpty(JSON_PATH_LINKS).expectIsArray(JSON_PATH_LINKS)
                                  .expectValue(JSON_PATH_CONTENT + ".version", newVersion),
                          "updateIdentityProvider : Error getting identity provider",
                          aPluginConfSaved.getId());
    }

    /**
     * Integration test to update a configured Identity Provider plugin of the Authentication module with error
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInexistantIdentityProvider() {
        PluginConfiguration unSavedPluginConf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                                        0,
                                                                        LdapAuthenticationPlugin.class
                                                                                .getAnnotation(Plugin.class).id());
        unSavedPluginConf.setId(12345L);
        performDefaultPut(IDP_URL,
                          unSavedPluginConf,
                          customizer().expectStatusNotFound(),
                          "updateInexistantIdentityProvider : Error getting identity provider",
                          unSavedPluginConf.getId());
    }

    /**
     * Integration test to update a configured Identity Provider plugin of the Authentication module with eror
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module with eror")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInvalidIdentityProvider() {
        PluginConfiguration conf = new PluginConfiguration(DEFAULT_PLUGIN_LABEL,
                                                           0,
                                                           LdapAuthenticationPlugin.class.getAnnotation(Plugin.class)
                                                                   .id());
        conf.setId(123L);
        performDefaultPut(IDP_URL,
                          conf,
                          customizer().expectStatusBadRequest(),
                          "updateInvalidIdentityProvider : Error getting identity provider",
                          12);
    }

    /**
     * Integration test to delete a configured Identity Provider plugin of the Authentication module
     */
    @Purpose("Integration test to delete a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteIdentityProvider() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        performDefaultDelete(IDP_URL,
                             customizer().expectStatusOk(),
                             "deleteIdentityProvider : Error getting identity provider",
                             aPluginConfSaved.getBusinessId());
    }

    /**
     * Integration test to delete a configured Identity Provider plugin of the Authentication module with error
     */
    @Purpose("Integration test to delete a configured Identity Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteInexistantIndentityProvider() {
        performDefaultDelete(IDP_URL, customizer().expectStatusNotFound(), "Error getting identity provider", "plop");

    }

}
