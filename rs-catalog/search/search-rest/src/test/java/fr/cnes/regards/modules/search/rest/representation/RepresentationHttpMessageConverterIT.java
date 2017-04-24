/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.representation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsITWithoutMockedCots;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.modules.search.rest.CatalogControllerTestUtils;
import fr.cnes.regards.modules.search.rest.plugin.MarkdownRepresentation;
import fr.cnes.regards.plugins.utils.PluginUtils;

@TestPropertySource(locations = { "classpath:dao.properties", "classpath:test-representation.properties" })
public class RepresentationHttpMessageConverterIT extends AbstractRegardsITWithoutMockedCots {

    private static final Logger LOG = LoggerFactory.getLogger(RepresentationHttpMessageConverterIT.class);

    /**
     * A dummy collection
     */
    public static final Collection COLLECTION = new Collection(null, DEFAULT_TENANT, "mycollection");

    /**
     * header accept value
     */
    private String acceptToUse;

    /**
     *
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * ElasticSearch repository
     */
    @Autowired
    private IEsRepository esRepository;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private final Set<Long> pluginConfToDelete = Sets.newHashSet();

    @Before
    public void setUp() {
        pluginService.addPluginPackage(MarkdownRepresentation.class.getPackage().getName());
        COLLECTION.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);
        // Populate the ElasticSearch repository
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        if (esRepository.indexExists(DEFAULT_TENANT)) {
            esRepository.deleteIndex(DEFAULT_TENANT);
        }
        esRepository.createIndex(DEFAULT_TENANT);
        esRepository.save(DEFAULT_TENANT, COLLECTION);
    }

    @Requirement("REGARDS_DSL_DAM_ARC_210")
    @Requirement("REGARDS_DSL_DAM_ARC_230")
    @Purpose("The system has a plugin Representation allowing to transform the result of a request search according to a MIME type")
    @Test
    public void test() throws ModuleException, InterruptedException {
        // lets get a collection as geo+json
        acceptToUse = "application/geo+json";
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations
                .add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mycollection")));
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
        // now lets try again with a newly created puglin configuration
        acceptToUse = new MediaType("text", "markdown").toString();
        expectations.clear();
        // create a pluginConfiguration for GeoJson
        PluginMetaData markdownMeta = PluginUtils
                .createPluginMetaData(MarkdownRepresentation.class,
                                      Lists.newArrayList(IRepresentation.class.getPackage().getName(),
                                                         MarkdownRepresentation.class.getPackage().getName()));

        PluginConfiguration markdownConf = new PluginConfiguration(markdownMeta, "dummy reprensentation plugin conf");
        markdownConf = pluginService.savePluginConfiguration(markdownConf);
        pluginConfToDelete.add(markdownConf.getId());
        // lets build the byte array we should have :
        MarkdownRepresentation expectedUsed = pluginService.getPlugin(markdownConf.getId());
        byte[] expectedContent = expectedUsed.transform(COLLECTION, StandardCharsets.UTF_8);
        // lets wait so amqp message is received and handled with some luck
        Thread.sleep(5000);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MarkdownRepresentation.MEDIA_TYPE));
        expectations.add(MockMvcResultMatchers.content().bytes(expectedContent));
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
        // now that we have seen that the creation of the plugin was taken into account lets desactivate it and take the
        // exception!
        markdownConf.setIsActive(false);
        pluginService.updatePluginConfiguration(markdownConf);
        Thread.sleep(5000);
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotAcceptable());
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
        // now lets reactivate it
        markdownConf.setIsActive(true);
        pluginService.updatePluginConfiguration(markdownConf);
        Thread.sleep(5000);
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MarkdownRepresentation.MEDIA_TYPE));
        expectations.add(MockMvcResultMatchers.content().bytes(expectedContent));
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
        // now lets delete it
        pluginService.deletePluginConfiguration(markdownConf.getId());
        Thread.sleep(5000);
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotAcceptable());
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
    }

    @Test
    public void testNotActivatedForNonAbstractEntity() {
        acceptToUse = "application/geo+json";
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        // \" and \" are to be added because the body is "..." and not just the string
        expectations.add(MockMvcResultMatchers.content().bytes(("\"" + TestController.TEST_BODY + "\"").getBytes()));
        performDefaultGet(TestController.TEST_PATH, expectations, "error getting the test hello world");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected MockHttpServletRequestBuilder getRequestBuilder(final String pAuthToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final Object... pUrlVars) {

        final MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(pHttpMethod, pUrlTemplate,
                                                                                            pUrlVars);
        addSecurityHeader(requestBuilder, pAuthToken);

        requestBuilder.header(HttpConstants.CONTENT_TYPE, "application/json");
        requestBuilder.header(HttpConstants.ACCEPT, acceptToUse);

        return requestBuilder;
    }

}
