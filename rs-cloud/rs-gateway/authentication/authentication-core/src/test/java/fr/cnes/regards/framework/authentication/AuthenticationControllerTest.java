/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
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

import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.LdapAuthenticationPlugin;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;

/**
 *
 * Class AuthenticationControllerTest
 *
 * Test REST endpoints to manage Identity provider plugins
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan("fr.cnes.regards.framework.authentication")
public class AuthenticationControllerTest extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationControllerTest.class);

    /**
     * Access route
     */
    private static final String IDPS_URL = "/authentication/idps";

    /**
     * Access route
     */
    private static final String IDP_URL = "/authentication/idps/{idp_id}";

    /**
     * Generated token for tests
     */
    private static String token = "";

    /**
     * Plugin id for this test
     */
    private static final Long PLUGIN_ID_TEST = 0L;

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
    private static final String PLUGIN_ID_LDAP = "LdapAuthenticationPlugin";

    /**
     * Repository stub
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     *
     * Init repository and resource accesses
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(0L);
        pluginConfRepo.save(conf);

        manageDefaultSecurity(IDPS_URL, RequestMethod.GET);
        manageDefaultSecurity(IDPS_URL, RequestMethod.POST);

        manageDefaultSecurity(IDP_URL, RequestMethod.GET);
        manageDefaultSecurity(IDP_URL, RequestMethod.PUT);
        manageDefaultSecurity(IDP_URL, RequestMethod.DELETE);

        token = generateToken("teszt@regards.fr", "", DEFAULT_ROLE);
    }

    /**
     *
     * Integration test to retrieve all configured Identity Provider plugins of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve all configured Identity Provider plugins of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveIdentityProviders() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        performGet(IDPS_URL, token, expectations, "Error getting identity providers");
    }

    /**
     *
     * TODO
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve one configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveIdentityProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        performGet(IDPS_URL + URL_PATH_SEPARATOR + PLUGIN_ID_TEST, token, expectations,
                   "retrieveIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to retrieve one configured Identity Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to retrieve one configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void retrieveInexistantIdentityProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performGet(IDPS_URL + "/123", token, expectations,
                   "retrieveInexistantIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to create a configured Identity Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to create a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void createIdentityProvider() {

        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, "Plugin2", 0);
        conf.setId(1L);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        performPost(IDPS_URL, token, conf, expectations, "createIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to update a configured Identity Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateIdentityProvider() {

        final String newVersion = "2.0";

        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(newVersion);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(0L);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".version",
                                                        org.hamcrest.Matchers.is(newVersion)));
        performPut(IDPS_URL + URL_PATH_SEPARATOR + PLUGIN_ID_TEST, token, conf, expectations,
                   "updateIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to update a configured Identity Provider plugin of the Authentication module with error
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInexistantIdentityProvider() {
        final Long testId = new Long(1234);
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(testId);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performPut(IDPS_URL + "/1234", token, conf, expectations,
                   "updateInexistantIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to update a configured Identity Provider plugin of the Authentication module with eror
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to update a configured Identity Provider plugin of the Authentication module with eror")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void updateInvalidIdentityProvider() {
        final Long testId = new Long(123);
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, DEFAULT_PLUGIN_LABEL, 0);
        conf.setId(testId);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST_400));
        performPut(IDPS_URL + "/12", token, conf, expectations,
                   "updateInvalidIdentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to delete a configured Identity Provider plugin of the Authentication module
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to delete a configured Identity Provider plugin of the Authentication module")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteIndentityProvider() {
        final Long testId = new Long(10);
        final PluginMetaData metadata = new PluginMetaData();
        metadata.setPluginId(PLUGIN_ID_LDAP);
        metadata.setPluginClassName(LdapAuthenticationPlugin.class.getName());
        metadata.setVersion(DEFAULT_PLUGIN_VERSION);
        final PluginConfiguration conf = new PluginConfiguration(metadata, "PluginToDelete", 0);
        conf.setId(testId);
        pluginConfRepo.save(conf);
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(IDPS_URL + "/10", token, expectations,
                      "deleteIndentityProvider : Error getting identity provider");
    }

    /**
     *
     * Integration test to delete a configured Identity Provider plugin of the Authentication module with error
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Integration test to delete a configured Identity Provider plugin of the Authentication module with error")
    @Requirement("REGARDS_DSL_ADM_ARC_010")
    @Requirement("REGARDS_DSL_ADM_ARC_020")
    @Test
    public void deleteInexistantIndentityProvider() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND_404));
        performDelete(IDPS_URL + "/1000", token, expectations, "Error getting identity provider");

    }

}
