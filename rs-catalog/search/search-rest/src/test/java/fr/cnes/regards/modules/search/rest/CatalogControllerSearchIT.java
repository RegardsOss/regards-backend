/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.IntegerAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;

/**
 *
 * Query search
 *
 * Lucene special characters must be escaped :
 * + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
 *
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { CatalogITConfiguration.class })
@MultitenantTransactional
public class CatalogControllerSearchIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogControllerSearchIT.class);

    private static final String OPENSEARCH_COLON = ":";

    /**
     * The mock attribute model client
     */
    @Autowired
    private IAttributeModelClient attributeModelClient;

    @Autowired
    private IProjectUsersClient projectUserClient;

    /**
     * ElasticSearch repository
     */
    @Autowired
    private IEsRepository esRepository;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory multitenantFlattenedFactory;

    @Before
    public void setUp() throws Exception {

        // Admin request = no group filtering
        Mockito.when(projectUserClient.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(true));

        // Manage index
        if (esRepository.indexExists(DEFAULT_TENANT)) {
            esRepository.deleteIndex(DEFAULT_TENANT);
        }
        esRepository.createIndex(DEFAULT_TENANT);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void searchTags() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q",
                       "(tags:(URN\\:AIP\\:DATASET\\:project1\\:3874e16e\\-f729\\-49c0\\-bae0\\-2c6903ec64e4\\:V1))");
        performDefaultGet(CatalogController.DATAOBJECTS_SEARCH, expectations, "Error searching dataobjects", builder);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void searchQuotedTags() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", "(tags:\"URN:AIP:DATASET:project1:3874e16e-f729-49c0-bae0-2c6903ec64e4:V1\")");
        performDefaultGet(CatalogController.DATAOBJECTS_SEARCH, expectations, "Error searching dataobjects", builder);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    @Ignore // Maybe problem with attribute caching! Investigate later!
    public final void searchTagsAndFragmentAttribute() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        String fragmentName = "FragmentDataobject";
        String attributeName = "Name";

        // Mock required attribute
        Fragment fragment = new Fragment();
        fragment.setName(fragmentName);
        AttributeModel attModel = AttributeModelBuilder.build(attributeName, AttributeType.STRING, null)
                .fragment(fragment).get();
        List<AttributeModel> attModels = Lists.newArrayList(attModel);
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(ResponseEntity.ok(HateoasUtils.wrapList(attModels)));

        RequestParamBuilder builder = RequestParamBuilder.build().param("q", "("
                + attModel.buildJsonPath(StaticProperties.PROPERTIES)
                + ":\"lmlml*\" AND (tags:(URN\\:AIP\\:DATASET\\:project1\\:3874e16e\\-f729\\-49c0\\-bae0\\-2c6903ec64e4\\:V1)))");
        performDefaultGet(CatalogController.DATAOBJECTS_SEARCH, expectations, "Error searching dataobjects", builder);
    }

    @Test
    @Ignore
    public final void search() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        // Mock required attribute
        String attributeName = "Number";
        AttributeModel attModel = AttributeModelBuilder.build(attributeName, AttributeType.INTEGER, null)
                .defaultFragment().get();
        List<AttributeModel> attModels = Lists.newArrayList(attModel);
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(ResponseEntity.ok(HateoasUtils.wrapList(attModels)));

        // Register attributes in serialization factory
        multitenantFlattenedFactory.registerSubtype(DEFAULT_TENANT, IntegerAttribute.class, attModel.getName());

        // DATA
        List<DataObject> dos = new ArrayList<>();
        OffsetDateTime creationDate = OffsetDateTime.now();
        for (int i = 0; i < 10000; i++) {
            DataObject dataObject = new DataObject(null, DEFAULT_TENANT, "do" + i);
            Set<AbstractAttribute<?>> ppties = new HashSet<>();
            ppties.add(AttributeBuilder.buildInteger(attModel.getName(), i));
            dataObject.setCreationDate(creationDate);
            dataObject.setProperties(ppties);
            dos.add(dataObject);
        }
        esRepository.saveBulk(DEFAULT_TENANT, dos);

        // REQUEST
        RequestParamBuilder builder = RequestParamBuilder.build();
        // builder.param("q", "(" + attModel.buildJsonPath(StaticProperties.PROPERTIES) + ":[* TO 10])");
        // builder.param("facets", attModel.buildJsonPath(StaticProperties.PROPERTIES));
        builder.param("facets", "creationDate");
        builder.param("facets", attModel.buildJsonPath(StaticProperties.PROPERTIES));
        performDefaultGet(CatalogController.DATAOBJECTS_SEARCH, expectations, "Error searching dataobjects", builder);
    }

    @Test
    public void searchWildcardTags() {

        // DATA
        List<DataObject> data = new ArrayList<>();
        data.add(new DataObject(null, DEFAULT_TENANT, "wildcardtag"));

        doSearchDataObjects(null, data, null, "tags:\"URN*\"");
    }

    @SuppressWarnings("unused")
    private final void searchDataObjects(List<AttributeModel> dynAttModels, List<DataObject> data,
            List<AttributeModel> facets, Map<AttributeModel, String> queries) {

        // Manage Q
        if (queries != null) {
            // Final OpenSearch query
            StringBuffer aggregateQuery = new StringBuffer();

            for (Entry<AttributeModel, String> query : queries.entrySet()) {
                AttributeModel queryable = query.getKey();
                String openSearchQuery = query.getValue();

                if (aggregateQuery.length() > 0) {
                    aggregateQuery.append(" AND ");
                }

                if (queryable.isDynamic()) {
                    aggregateQuery.append(queryable.buildJsonPath(StaticProperties.PROPERTIES));
                } else {
                    aggregateQuery.append(queryable.getName());
                }
                aggregateQuery.append(OPENSEARCH_COLON);
                aggregateQuery.append(openSearchQuery);
            }
            doSearchDataObjects(dynAttModels, data, facets, aggregateQuery.toString());
        } else {
            doSearchDataObjects(dynAttModels, data, facets, null);
        }
    }

    /**
     *
     * Utility method to build and launch query
     *
     * {@link https://lucene.apache.org/core/6_6_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description}
     * {@link https://lucene.apache.org/core/6_6_0/queryparser/index.html}
     *
     * @param dynAttModels
     * @param facets
     * @param data
     * @param openSearchQuery
     */
    private final void doSearchDataObjects(List<AttributeModel> dynAttModels, List<DataObject> data,
            List<AttributeModel> facets, String openSearchQuery) {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        // Mock dynamic attributes retrieval
        if (dynAttModels != null) {
            Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                    .thenReturn(ResponseEntity.ok(HateoasUtils.wrapList(dynAttModels)));
        }

        // Manage JSON (de)serialization registering all attributes in factory
        multitenantFlattenedFactory.registerAttributes(DEFAULT_TENANT, dynAttModels);

        // Index DATA
        esRepository.saveBulk(DEFAULT_TENANT, data); // Auto-refresh

        // Build REQUEST
        RequestParamBuilder builder = RequestParamBuilder.build();

        // Manage FACETS
        if (facets != null) {
            for (AttributeModel facetable : facets) {
                if (facetable.isDynamic()) {
                    builder.param("facets", facetable.buildJsonPath(StaticProperties.PROPERTIES));
                } else {
                    builder.param("facets", facetable.getName());
                }
            }
        }

        // Manage Q
        if (openSearchQuery != null) {
            builder.param("q", openSearchQuery);
        }

        // Launch request
        performDefaultGet(CatalogController.DATAOBJECTS_SEARCH, expectations, "Error searching dataobjects", builder);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
