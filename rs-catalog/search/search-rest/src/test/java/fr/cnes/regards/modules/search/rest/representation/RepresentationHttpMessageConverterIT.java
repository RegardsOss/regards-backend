/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.representation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.search.rest.CatalogControllerTestUtils;
import fr.cnes.regards.modules.search.rest.SearchController;

@TestPropertySource(locations = { "classpath:test-representation.properties" })
@ActiveProfiles("testAmqp")
public class RepresentationHttpMessageConverterIT extends AbstractRegardsIT {

    /**
     * A dummy collection
     */
    public static final Collection COLLECTION = new Collection(null, DEFAULT_TENANT, "mycollection");

    private static final Logger LOG = LoggerFactory.getLogger(RepresentationHttpMessageConverterIT.class);

    private final Set<Long> pluginConfToDelete = Sets.newHashSet();

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

    @Autowired
    private IProjectUsersClient projectUserClient;

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

    @After
    public void cleanUp() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        PluginConfiguration geoJson = null;
        try {
            geoJson = pluginService
                    .getPluginConfigurationByLabel(RepresentationConfiguration.DEFAULT_GEO_JSON_CONFIGURATION_LABEL);
            pluginService.deletePluginConfiguration(geoJson.getId());
        } catch (ModuleException e) { // NOSONAR
        }
    }

    @Requirement("REGARDS_DSL_DAM_ARC_210")
    @Requirement("REGARDS_DSL_DAM_ARC_230")
    @Purpose(
            "The system has a plugin Representation allowing to transform the result of a request search according to a MIME type")
    @Test
    public void test() throws ModuleException, InterruptedException {
        Mockito.when(projectUserClient.isAdmin(DEFAULT_USER_EMAIL))
                .thenReturn(new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK));
        // lets get a collection as geo+json
        acceptToUse = "application/geo+json; charset=UTF-8";
        RequestBuilderCustomizer requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(acceptToUse));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        requestBuilderCustomizer.addExpectation(
                MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mycollection")));
        String jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(SearchController.PATH + SearchController.COLLECTIONS_URN, jwt, requestBuilderCustomizer,
                   "Error retrieving a collection", COLLECTION.getIpId());
        // now lets try again with a newly created puglin configuration
        acceptToUse = new MediaType("text", "markdown").toString();
        requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        // create a pluginConfiguration for GeoJson
        PluginMetaData markdownMeta = PluginUtils.createPluginMetaData(MarkdownRepresentation.class, Lists.newArrayList(
                IRepresentation.class.getPackage().getName(), MarkdownRepresentation.class.getPackage().getName()));

        PluginConfiguration markdownConf = new PluginConfiguration(markdownMeta, "dummy reprensentation plugin conf");
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        markdownConf = pluginService.savePluginConfiguration(markdownConf);
        pluginConfToDelete.add(markdownConf.getId());
        // lets build the byte array we should have :
        MarkdownRepresentation expectedUsed = pluginService.getPlugin(markdownConf.getId());
        byte[] expectedContent = expectedUsed.transform(COLLECTION, StandardCharsets.UTF_8);
        // lets wait so amqp message is received and handled with some luck
        Thread.sleep(5000);
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MarkdownRepresentation.MEDIA_TYPE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().bytes(expectedContent));
        jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(SearchController.PATH + SearchController.COLLECTIONS_URN, jwt, requestBuilderCustomizer,
                   "Error retrieving a collection", COLLECTION.getIpId());
        // now that we have seen that the creation of the plugin was taken into account lets desactivate it and take the
        // exception!
        markdownConf.setIsActive(false);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        pluginService.updatePluginConfiguration(markdownConf);
        Thread.sleep(5000);
        requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNotAcceptable());
        jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(SearchController.PATH + SearchController.COLLECTIONS_URN, jwt, requestBuilderCustomizer, "Error retrieving a collection",
                   COLLECTION.getIpId());
        // now lets reactivate it
        markdownConf.setIsActive(true);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        pluginService.updatePluginConfiguration(markdownConf);
        Thread.sleep(5000);
        requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.content().contentType(MarkdownRepresentation.MEDIA_TYPE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().bytes(expectedContent));
        jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(SearchController.PATH + SearchController.COLLECTIONS_URN, jwt, requestBuilderCustomizer, "Error retrieving a collection",
                   COLLECTION.getIpId());
        // now lets delete it
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        pluginService.deletePluginConfiguration(markdownConf.getId());
        Thread.sleep(5000);
        requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNotAcceptable());
        jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(SearchController.PATH + SearchController.COLLECTIONS_URN, jwt, requestBuilderCustomizer, "Error retrieving a collection",
                   COLLECTION.getIpId());
    }

    @Test
    public void testNotActivatedForNonAbstractEntity() {
        acceptToUse = "application/geo+json";
        RequestBuilderCustomizer requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // \" and \" are to be added because the body is "..." and not just the string
        requestBuilderCustomizer.addExpectation(
                MockMvcResultMatchers.content().bytes(("\"" + TestController.TEST_BODY + "\"").getBytes()));
        String jwt = manageDefaultSecurity(SearchController.PATH + SearchController.COLLECTIONS_URN, RequestMethod.GET);
        performGet(TestController.TEST_PATH, jwt, requestBuilderCustomizer, "error getting the test hello world");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT, Lists.newArrayList(acceptToUse));

        return headers;
    }

}
