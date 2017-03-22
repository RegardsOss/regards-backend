/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
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
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
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
     * A dummy collection
     */
    public static final Collection COLLECTION = new Collection(null, DEFAULT_TENANT, "mycollection");

    /**
     * A dummy dataobject
     */
    public static final DataObject DATAOBJECT = new DataObject(null, DEFAULT_TENANT, "mydataobject");

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET = new Dataset(null, DEFAULT_TENANT, "mydataset");

    /**
     * An other dummy dataset
     */
    public static final Dataset DATASET_1 = new Dataset(null, DEFAULT_TENANT, "mydataset");

    /**
     * A dummy document
     */
    public static final Document DOCUMENT = new Document(null, DEFAULT_TENANT, "mydocument");

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(CatalogControllerTestUtils.CLIENT_RESPONSE);

        // Populate the ElasticSearch repository
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        if (esRepository.indexExists(DEFAULT_TENANT)) {
            esRepository.deleteIndex(DEFAULT_TENANT);
        }
        esRepository.createIndex(DEFAULT_TENANT);
        esRepository.save(DEFAULT_TENANT, COLLECTION);
        esRepository.save(DEFAULT_TENANT, DATASET);
        esRepository.save(DEFAULT_TENANT, DATASET_1);
        esRepository.save(DEFAULT_TENANT, DATAOBJECT);
        esRepository.save(DEFAULT_TENANT, DOCUMENT);
        esRepository.refresh(DEFAULT_TENANT);
    }

    /**
     * Le système doit permettre de réaliser une recherche par critères sur l’ensemble du catalogue.
     */
    @Test
    @Purpose("Le système doit permettre de réaliser une recherche par critères sur l’ensemble du catalogue.")
    @Requirement("REGARDS_DSL_DAM_CAT_510")
    public final void testSearchAll() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        performDefaultGet("/search", expectations, "Error searching all entities", builder);
    }

    /**
     * Le système doit permettre de fournir des facettes pour toute recherche dans le catalogue.
     */
    @Test
    @Purpose("Le système doit permettre de fournir des facettes pour toute recherche dans le catalogue.")
    @Requirement("REGARDS_DSL_DAM_CAT_610")
    public final void testSearchAll_withFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet("/search", expectations, "Error searching all entities", builder);
    }

    /**
     * Le système doit permettre de manière synchrone d’accéder aux informations d’une collection via son IP_ID.
     */
    @Test
    @Purpose("Le système doit permettre de manière synchrone d’accéder aux informations d’une collection via son IP_ID.")
    @Requirement("REGARDS_DSL_DAM_COL_310")
    public final void testGetCollection() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
    }

    /**
     * Le système doit permettre de manière synchrone de rechercher des collections à partir de critères basés sur des
     * éléments du modèle de données.
     */
    @Test
    @Purpose("Le système doit permettre de manière synchrone de rechercher des collections à partir de critères basés sur des éléments du modèle de données.")
    @Requirement("REGARDS_DSL_DAM_COL_410")
    public final void testSearchCollections_shouldFindOneWithoutFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                        Matchers.is("mycollection")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets").doesNotExist());
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        performDefaultGet("/collections/search", expectations, "Error searching collections", builder);
    }

    /**
     * Cette fonction permet de récupérer un jeu de données
     */
    @Test
    public final void testGetDataset() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet("/datasets/{urn}", expectations, "Error retrieving a dataset", DATASET.getIpId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDatasets(java.lang.String, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
     */
    @Test
    public final void testSearchDatasets_shouldFindTwoWithoutFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                        Matchers.is("mydataset")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[1].content.label",
                                                        Matchers.is("mydataset")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets").doesNotExist());
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet("/datasets/search", expectations, "Error searching datasets", builder);
    }

    /**
     * Le système doit permettre de consulter les métadonnées d’un objet de données du catalogue.
     */
    @Test
    @Purpose("Le système doit permettre de consulter les métadonnées d’un objet de données du catalogue.")
    @Requirement("REGARDS_DSL_DAM_CAT_470")
    public final void testGetDataobject() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations
                .add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mydataobject")));
        performDefaultGet("/dataobjects/{urn}", expectations, "Error retrieving a dataobject", DATAOBJECT.getIpId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjects(java.lang.String, java.util.List, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
     */
    @Test
    public final void testSearchDataobjects_shouldFindOneWithFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                        Matchers.is("mydataobject")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.notNullValue()));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DATAOBJECT);
        performDefaultGet("/dataobjects/search", expectations, "Error searching dataobjects", builder);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjectsReturnDatasets(java.lang.String, java.util.List, org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)}.
     */
    @Test
    public final void testSearchDataobjectsReturnDatasets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        performDefaultGet("/dataobjects/datasets/search", expectations, "Error searching datasets via dataobjects",
                          builder);
    }

    /**
     * Le système doit permettre de manière synchrone d’accéder aux informations d’un document via son IP_ID.
     */
    @Test
    @Purpose("Le système doit permettre de manière synchrone d’accéder aux informations d’un document via son IP_ID.")
    @Requirement("REGARDS_DSL_DAM_DOC_310")
    public final void testGetDocument() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet("/documents/{urn}", expectations, "Error retrieving a document", DOCUMENT.getIpId());
    }

    /**
     * Le système doit permettre de manière synchrone de rechercher des documents à partir de critères basés sur des
     * éléments du modèle de données.
     */
    @Test
    @Purpose("Le système doit permettre de manière synchrone de rechercher des documents à partir de critères basés sur des éléments du modèle de données.")
    @Requirement("REGARDS_DSL_DAM_DOC_510")
    public final void testSearchDocuments() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        performDefaultGet("/documents/search", expectations, "Error searching documents", builder);
    }

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
