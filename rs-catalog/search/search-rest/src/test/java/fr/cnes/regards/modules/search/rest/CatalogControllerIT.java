/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;

/**
 * Integration test for {@link CatalogController}
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class CatalogControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CatalogControllerIT.class);

    /**
     * The mock attribute model client
     */
    @Autowired
    private IAttributeModelClient attributeModelClient;

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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(CatalogControllerTestUtils.CLIENT_RESPONSE);

        // Populate the ElasticSearch repository
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        esRepository.createIndex(DEFAULT_TENANT);
        esRepository.save(DEFAULT_TENANT, CatalogControllerTestUtils.COLLECTION);
        esRepository.save(DEFAULT_TENANT, CatalogControllerTestUtils.DATASET);
        esRepository.save(DEFAULT_TENANT, CatalogControllerTestUtils.DATAOBJECT);
        esRepository.save(DEFAULT_TENANT, CatalogControllerTestUtils.DOCUMENT);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        esRepository.deleteIndex(DEFAULT_TENANT);
    }

    /**
     * Check that the system allows to se
     */
    @Test
    public final void testSearchAll_withAllParams() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet("/search", expectations, "Error searching all entities", builder);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#getCollection(fr.cnes.regards.modules.entities.urn.UniformResourceName)}.
     */
    //    @Test
    //    public final void testGetCollection() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchCollections(java.lang.String, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testSearchCollections() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#getDataset(fr.cnes.regards.modules.entities.urn.UniformResourceName)}.
    //     */
    //    @Test
    //    public final void testGetDataset() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDatasets(java.lang.String, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testSearchDatasets() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#getDataobject(fr.cnes.regards.modules.entities.urn.UniformResourceName)}.
    //     */
    //    @Test
    //    public final void testGetDataobject() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjects(java.lang.String, java.util.List, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testSearchDataobjects() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjectsReturnDatasets(java.lang.String, java.util.List, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testSearchDataobjectsReturnDatasets() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#getDocument(fr.cnes.regards.modules.entities.urn.UniformResourceName)}.
    //     */
    //    @Test
    //    public final void testGetDocument() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDocuments(java.lang.String, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testSearchDocuments() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }
    //
    //    /**
    //     * Test method for
    //     * {@link fr.cnes.regards.modules.search.rest.CatalogController#doSearch(java.lang.String, fr.cnes.regards.modules.search.rest.CatalogController.SearchType, java.lang.Class, java.util.List, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
    //     */
    //    @Test
    //    public final void testDoSearch() {
    //        Assert.fail("Not yet implemented"); // TODO
    //    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.test.integration.AbstractRegardsIT#getLogger()
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
