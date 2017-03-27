/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos.KerberosServiceProviderPlugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class AuthenticationControllerIT
 *
 * Test REST endpoints to manage Service provider plugins
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 *
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan("fr.cnes.regards.framework.authentication")
public class ExternalAuthenticationControllerIT extends AbstractRegardsIT {

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

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     *
     * Init the context of the tests
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);

        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setInterfaceName("fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin");
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        aPluginConfSaved = pluginConfRepo.save(conf);

        manageDefaultSecurity(SPS_URL, RequestMethod.GET);
        manageDefaultSecurity(SPS_URL, RequestMethod.POST);

        manageDefaultSecurity(SP_URL, RequestMethod.GET);
        manageDefaultSecurity(SP_URL, RequestMethod.PUT);
        manageDefaultSecurity(SP_URL, RequestMethod.DELETE);
        manageDefaultSecurity(AUTHENTICATE_URL, RequestMethod.POST);
    }

    /**
     *
     * Integration test to retrieve all configured Service Provider plugins of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve all configured Service Provider plugins of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveServiceProviders() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        performDefaultGet(SPS_URL, expectations, "Error getting Service providers");
    }

    /**
     *
     * Integration test to retrieve one configured Service Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve one configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveServiceProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        performDefaultGet(SPS_URL + URL_PATH_SEPARATOR + aPluginConfSaved.getId().toString(), expectations,
                          "retrieveServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to retrieve one configured Service Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve one configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveInexistantServiceProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performDefaultGet(SPS_URL + "/123", expectations,
                          "retrieveInexistantServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to create a configured Service Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to create a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void createServiceProvider() {

        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, "Plugin2", 0);
        conf.setId(1L);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        performDefaultPost(SPS_URL, conf, expectations, "createServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to update a configured Service Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateServiceProvider() {
        final String newVersion = "2.0";
        aPluginConfSaved.setVersion(newVersion);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".version",
                                                        org.hamcrest.Matchers.is(newVersion)));
        performDefaultPut(SPS_URL + URL_PATH_SEPARATOR + aPluginConfSaved.getId().toString(), aPluginConfSaved,
                          expectations, "updateServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to update a configured Service Provider plugin of the Authentication module with error
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInexistantServiceProvider() {
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration unSavedPluginConf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        unSavedPluginConf.setId(12345L);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performDefaultPut(SPS_URL + URL_PATH_SEPARATOR + unSavedPluginConf.getId().toString(), unSavedPluginConf,
                          expectations, "updateInexistantServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to update a configured Service Provider plugin of the Authentication module with eror
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Service Provider plugin of the Authentication module with eror")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInvalidServiceProvider() {
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(123L);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST_400));
        performDefaultPut(SPS_URL + "/12", conf, expectations,
                          "updateInvalidServiceProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to delete a configured Service Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to delete a configured Service Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteIdentityProvider() {
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_KERBEROS);
        metadata.setPluginClassName(KerberosServiceProviderPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        PluginConfiguration aPluginConfToDelete = new PluginConfiguration(metadata, "PluginToDelete", 0);
        aPluginConfToDelete = pluginConfRepo.save(aPluginConfToDelete);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(SPS_URL + URL_PATH_SEPARATOR + aPluginConfToDelete.getId().toString(), expectations,
                             "deleteIdentityProvider : Error getting Service provider");
    }

    /**
     *
     * Integration test to delete a configured Service Provider plugin of the Authentication module with error
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to delete a configured Service Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteInexistantIndentityProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performDefaultDelete(SPS_URL + "/1000", expectations, "Error getting Service provider");

    }

    @Ignore
    @Test
    public void authenticateKerberosServiceProvider() {

        final ExternalAuthenticationInformations infos = new ExternalAuthenticationInformations("usernma",
                DEFAULT_TENANT, "ticket".getBytes(), "key");

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPost("/authentication/sps/0/authenticate", infos, expectations,
                           "kerberos authenticate : Authentication error");

    }

}
