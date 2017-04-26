/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
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
@ContextConfiguration(classes = { CatalogITConfiguration.class })
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
     * The mock user client providing access groups
     */
    @Autowired
    private IUserClient userClient;

    @Autowired
    private IProjectUsersClient projectUserClient;

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
    private static final Collection COLLECTION = new Collection(null, DEFAULT_TENANT, "mycollection");

    /**
     * Another dummy collection with same label BUT with no group in common
     */
    private static final Collection COLLECTION2 = new Collection(null, DEFAULT_TENANT, "mycollection");

    /**
     * A dummy dataobject
     */
    private static final DataObject DATAOBJECT = new DataObject(null, DEFAULT_TENANT, "mydataobject");

    /**
     * A dummy dataset
     */
    private static final Dataset DATASET = new Dataset(null, DEFAULT_TENANT, "mydataset");

    /**
     * An other dummy dataset
     */
    private static final Dataset DATASET_1 = new Dataset(null, DEFAULT_TENANT, "mydataset");

    /**
     * A dummy document
     */
    private static final Document DOCUMENT = new Document(null, DEFAULT_TENANT, "mydocument");

    private static final String OTHER_USER_EMAIL = "other.user@regards.fr";

    private static final String ADMIN_USER_EMAIL = "admin.user@regards.fr";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Mock clients
        Mockito.when(attributeModelClient.getAttributes(Mockito.any(), Mockito.any()))
                .thenReturn(CatalogControllerTestUtils.ATTRIBUTE_MODEL_CLIENT_RESPONSE);

        // Order is important : wider first, then specific cases
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(CatalogControllerTestUtils.USER_CLIENT_RESPONSE);
        Mockito.when(
                userClient.retrieveAccessGroupsOfUser(Mockito.eq(OTHER_USER_EMAIL), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(CatalogControllerTestUtils.USER_CLIENT_OTHER_RESPONSE);

        Mockito.when(projectUserClient.isAdmin(Mockito.anyString()))
                .thenReturn(CatalogControllerTestUtils.PROJECT_USERS_CLIENT_RESPONSE);
        Mockito.when(projectUserClient.isAdmin(Mockito.eq(ADMIN_USER_EMAIL)))
                .thenReturn(CatalogControllerTestUtils.PROJECT_USERS_CLIENT_RESPONSE_ADMIN);

        // Set groups
        COLLECTION.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);
        DATAOBJECT.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);
        DATASET.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);
        DATASET_1.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);
        DOCUMENT.setGroups(CatalogControllerTestUtils.ACCESS_GROUP_NAMES_AS_SET);

        // Collections to test group restrictions
        COLLECTION2.setGroups(Sets.newHashSet(CatalogControllerTestUtils.ACCESS_GROUP_NAME_2));

        // Populate the ElasticSearch repository
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        if (esRepository.indexExists(DEFAULT_TENANT)) {
            esRepository.deleteIndex(DEFAULT_TENANT);
        }
        esRepository.createIndex(DEFAULT_TENANT);
        esRepository.save(DEFAULT_TENANT, COLLECTION);
        esRepository.save(DEFAULT_TENANT, COLLECTION2);
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
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
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
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchAll_withFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets",
                                                        Matchers.hasItem(Matchers.hasEntry("attributeName", "label"))));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets",
                                                        Matchers.hasItem(Matchers.hasEntry("type", "STRING"))));
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet("/searchwithfacets", expectations, "Error searching all entities", builder);
    }

    /**
     * Le système doit permettre de manière synchrone d’accéder aux informations d’une collection via son IP_ID.
     */
    @Test
    @Purpose(
            "Le système doit permettre de manière synchrone d’accéder aux informations d’une collection via son IP_ID.")
    @Requirement("REGARDS_DSL_DAM_COL_310")
    public final void testGetCollection() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations
                .add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mycollection")));
        performDefaultGet("/collections/{urn}", expectations, "Error retrieving a collection", COLLECTION.getIpId());
    }

    /**
     * Le système doit permettre de manière synchrone de rechercher des collections à partir de critères basés sur des
     * éléments du modèle de données.
     */
    @Test
    @Purpose(
            "Le système doit permettre de manière synchrone de rechercher des collections à partir de critères basés sur des éléments du modèle de données.")
    @Requirement("REGARDS_DSL_DAM_COL_410")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchCollections_shouldFindOneWithoutFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mycollection")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets").doesNotExist());
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        performDefaultGet("/collections/search", expectations, "Error searching collections", builder);
    }

    @Requirements({ @Requirement("REGARDS_DSL_DAM_COL_430"), @Requirement("REGARDS_DSL_DAM_COL_440") })
    @Purpose("Le résultat d’une recherche ne doit retourner que la liste des collections contenant des jeux de "
            + "données auxquels l’utilisateur courant a accès.\n"
            + "Si l’utilisateur est un administrateur, le résultat d’une recherche doit fournir toutes les collections (y "
            + "compris celles ne contenant pas de jeux de données)")
    @Test
    public final void testSearchCollectionsByTwoUsersWithDifferentsGroups() {
        // Default user should retrieve only collection with accessGroup0 and accessGroup1
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mycollection")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.groups.[0]",
                                                        Matchers.either(Matchers.is(
                                                                CatalogControllerTestUtils.ACCESS_GROUP_NAME_0))
                                                                .or(Matchers.is(
                                                                        CatalogControllerTestUtils.ACCESS_GROUP_NAME_1))));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        // Search collection with default user
        performDefaultGet("/collections/search", expectations, "Error searching collections", builder);

        // Other user should retrieve only collection with accessGroup2
        final List<ResultMatcher> expectations2 = new ArrayList<>();
        expectations2.add(MockMvcResultMatchers.status().isOk());
        expectations2.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations2.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations2.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                         Matchers.is("mycollection")));
        expectations2.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].content.groups.[0]",
                                                         Matchers.is(CatalogControllerTestUtils.ACCESS_GROUP_NAME_2)));

        // Search collection with second user
        performGet("/collections/search",
                   manageDefaultSecurity(OTHER_USER_EMAIL, "/collections/search", RequestMethod.GET), expectations2,
                   "Error searching collections", builder);

        // Admin user whould retrieve the two previous collections (the one with 2 groups and the one with only one)
        final List<ResultMatcher> expectations3 = new ArrayList<>();
        expectations3.add(MockMvcResultMatchers.status().isOk());
        expectations3.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations3.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations3.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.length", Matchers.is(2)));

        // Search collection with admin user
        performGet("/collections/search",
                   manageDefaultSecurity(ADMIN_USER_EMAIL, "/collections/search", RequestMethod.GET), expectations2,
                   "Error searching collections", builder);
    }

    /**
     * Cette fonction permet de récupérer un jeu de données
     */
    @Test
    public final void testGetDataset() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mydataset")));
        performDefaultGet("/datasets/{urn}", expectations, "Error retrieving a dataset", DATASET.getIpId());
    }

    /**
     * Le système doit permettre de désactiver la gestion des facettes pour des questions de performance.
     */
    @Test
    @Purpose("Le système doit permettre de désactiver la gestion des facettes pour des questions de performance.")
    @Requirement("REGARDS_DSL_DAM_CAT_620")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDatasets_shouldFindTwoWithoutFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydataset")));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[1].content.label", Matchers.is("mydataset")));
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
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjects(String, Map, Pageable)}.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDataobjects_shouldFindOneWithFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydataobject")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets",
                                                        Matchers.hasItem(Matchers.hasEntry("attributeName", "label"))));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets",
                                                        Matchers.hasItem(Matchers.hasEntry("type", "STRING"))));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DATAOBJECT)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet("/dataobjects/search", expectations, "Error searching dataobjects", builder);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.search.rest.CatalogController#searchDataobjectsReturnDatasets(String, Map, Pageable, PagedResourcesAssembler)}.
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
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mydocument")));
        performDefaultGet("/documents/{urn}", expectations, "Error retrieving a document", DOCUMENT.getIpId());
    }

    /**
     * Le système doit permettre de manière synchrone de rechercher des documents à partir de critères basés sur des
     * éléments du modèle de données.
     */
    @Test
    @Purpose(
            "Le système doit permettre de manière synchrone de rechercher des documents à partir de critères basés sur des éléments du modèle de données.")
    @Requirement("REGARDS_DSL_DAM_DOC_510")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDocuments() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydocument")));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DOCUMENT);
        performDefaultGet("/documents/search", expectations, "Error searching documents", builder);
    }

    /**
     * Check that the system can return a sorted page of results.
     */
    @Test
    @Purpose("Check that the system can return a sorted page of results.")
    @Requirement("REGARDS_DSL_DAM_DOC_510")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearch_withSort() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydataset")));

        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS)
                .param("sort", CatalogControllerTestUtils.SORT);
        performDefaultGet("/datasets/search", expectations, "Error searching documents", builder);
    }

    /**
     * Check that the system adds a self hateoas link on datasets pointing to the dataset/{urn} endpoint.
     */
    @Test
    @Purpose("Check that the system adds a self hateoas link on datasets pointing to the dataset/{urn} endpoint.")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDatasets_shouldHaveSelfLink() {
        // Prepare authorization
        setAuthorities("/datasets/{urn}", RequestMethod.GET, DEFAULT_ROLE);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].rel", Matchers.is("self")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].href",
                                                        Matchers.startsWith(
                                                                "http://localhost/datasets/URN:AIP:DATASET:")));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet("/datasets/search", expectations, "Error searching datasets", builder);
    }

    /**
     * Check that the system adds a hateoas link on datasets pointing their dataobjects via a search with the pre-filled
     * query.
     */
    @Test
    @Purpose(
            "Check that the system adds a hateoas link on datasets pointing their dataobjects via a search with the pre-filled query.")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDatasets_shouldHaveLinkNavigatingToDataobjects() {
        // Prepare authorization
        setAuthorities("/dataobjects/search", RequestMethod.GET, DEFAULT_ROLE);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].rel",
                                                        Matchers.either(Matchers.is("next")).or(Matchers.is("self"))));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].href", Matchers.either(
                Matchers.startsWith("http://localhost/dataobjects/search?q"))
                .or(Matchers.startsWith("http://localhost/datasets/"))));
        RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet("/datasets/search", expectations, "Error searching datasets", builder);
    }

    /*
     * (non-Javadoc)
     * @see fr.cnes.regards.framework.test.integration.AbstractRegardsIT#getLogger()
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
