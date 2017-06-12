/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.List;

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
import fr.cnes.regards.modules.entities.domain.StaticProperties;
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

    @Before
    public void setUp() throws Exception {

        // Admin request = no group filtering
        Mockito.when(projectUserClient.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(true));

        // Manage index
        if (!esRepository.indexExists(DEFAULT_TENANT)) {
            esRepository.createIndex(DEFAULT_TENANT);
        }
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

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
