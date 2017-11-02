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
package fr.cnes.regards.modules.search.rest;

import static fr.cnes.regards.modules.search.rest.SearchController.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.StringAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Integration test for {@link SearchController}
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { CatalogITConfiguration.class })
@MultitenantTransactional
public class SearchControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SearchControllerIT.class);

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

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory factory;

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
        DATAOBJECT.setTags(Sets.newHashSet(DATASET.getIpId().toString()));
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

        Set<AbstractAttribute<?>> ppties = new HashSet<>();
        ppties.add(AttributeBuilder.buildString(CatalogControllerTestUtils.EXTRA_ATTRIBUTE_NAME, "extrafacet"));
        DATAOBJECT.setProperties(ppties);

        factory.registerSubtype(DEFAULT_TENANT, StringAttribute.class, CatalogControllerTestUtils.EXTRA_ATTRIBUTE_NAME);
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
        final RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        performDefaultGet(PATH, expectations, "Error searching all entities", builder);
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
        final RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet(PATH + SEARCH_WITH_FACETS, expectations,
                          "Error searching all entities", builder);
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
        performDefaultGet(PATH + COLLECTIONS_URN, expectations, "Error retrieving a collection",
                          COLLECTION.getIpId());
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
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        performDefaultGet(PATH + COLLECTIONS_SEARCH, expectations, "Error searching collections", builder);
    }

    @Requirements({ @Requirement("REGARDS_DSL_DAM_COL_430"), @Requirement("REGARDS_DSL_DAM_COL_440") })
    @Purpose("Le résultat d’une recherche ne doit retourner que la liste des collections contenant des jeux de "
            + "données auxquels l’utilisateur courant a accès.\n"
            + "Si l’utilisateur est un administrateur, le résultat d’une recherche doit fournir toutes les collections (y "
            + "compris celles ne contenant pas de jeux de données)")
    @Test
    public final void testSearchCollectionsByTwoUsersWithDifferentsGroups() {
        // Default user should retrieve only collection with accessGroup0 and accessGroup1
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                                  Matchers.is("mycollection")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".content.[0].content.groups.[0]",
                                                                  Matchers.either(Matchers.is(
                                                                          CatalogControllerTestUtils.ACCESS_GROUP_NAME_0))
                                                                          .or(Matchers.is(
                                                                                  CatalogControllerTestUtils.ACCESS_GROUP_NAME_1))));
        requestBuilderCustomizer.customizeRequestParam().param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        // Search collection with default user
        performDefaultGet(PATH + COLLECTIONS_SEARCH, requestBuilderCustomizer,
                          "Error searching collections");

        // Other user should retrieve only collection with accessGroup2
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label",
                                                                  Matchers.is("mycollection")));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".content.[0].content.groups.[0]",
                                                                  Matchers.is(
                                                                          CatalogControllerTestUtils.ACCESS_GROUP_NAME_2)));
        requestBuilderCustomizer.customizeRequestParam().param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);
        // Search collection with second user
        performGet(PATH + COLLECTIONS_SEARCH,
                   manageDefaultSecurity(OTHER_USER_EMAIL, PATH + COLLECTIONS_SEARCH, RequestMethod.GET),
                   requestBuilderCustomizer, "Error searching collections");

        // Admin user whould retrieve the two previous collections (the one with 2 groups and the one with only one)
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.length()", Matchers.is(2)));
        requestBuilderCustomizer.customizeRequestParam().param("q", CatalogControllerTestUtils.Q_FINDS_ONE_COLLECTION);

        // Search collection with admin user
        performGet(PATH + COLLECTIONS_SEARCH,
                   manageDefaultSecurity(ADMIN_USER_EMAIL, PATH + COLLECTIONS_SEARCH, RequestMethod.GET),
                   requestBuilderCustomizer, "Error searching collections");
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
        performDefaultGet(PATH + DATASETS_URN, expectations, "Error retrieving a dataset",
                          DATASET.getIpId());
    }

    /**
     * Cette fonction permet de récupérer un jeu de données
     */
    @Test
    public final void testGetEntity() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.label", Matchers.is("mydataset")));
        performDefaultGet(PATH + ENTITY_GET_MAPPING, expectations,
                          "Error retrieving a dataset", DATASET.getIpId());
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
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet(PATH + DATASETS_SEARCH, expectations, "Error searching datasets", builder);
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
        performDefaultGet(PATH + DATAOBJECTS_URN, expectations, "Error retrieving a dataobject",
                          DATAOBJECT.getIpId());
    }

    /**
     * Test method for
     * {@link SearchController#searchDataobjects}.
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
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.hasItem(
                Matchers.hasEntry("attributeName", "properties." + CatalogControllerTestUtils.EXTRA_ATTRIBUTE_NAME))));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets",
                                                        Matchers.hasItem(Matchers.hasEntry("type", "STRING"))));
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DATAOBJECT)
                .param("facets", CatalogControllerTestUtils.FACETS_AS_ARRAY);
        performDefaultGet(PATH + DATAOBJECTS_SEARCH_WITH_FACETS, expectations,
                          "Error searching dataobjects", builder);
    }

    /**
     * Test method for
     * {@link SearchController#searchDataobjects}.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDataobjects_shouldFindOneWithoutFacets() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydataobject")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.empty()));
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DATAOBJECT);
        performDefaultGet(PATH + DATAOBJECTS_SEARCH_WITH_FACETS, expectations,
                          "Error searching dataobjects", builder);
    }

    /**
     * Test method for
     * {@link SearchController#searchDataobjects}.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    @Requirement("REGARDS_DSL_DAM_ARC_820")
    public final void testSearchDataobjects_shouldFindAll() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].content.label", Matchers.is("mydataobject")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".facets", Matchers.empty()));
        final RequestParamBuilder builder = RequestParamBuilder.build();
        performDefaultGet(PATH + DATAOBJECTS_SEARCH_WITH_FACETS, expectations,
                          "Error searching dataobjects", builder);
    }

    /**
     * Test method for
     * {@link SearchController#searchDataobjectsReturnDatasets}.
     */
    @Test
    public final void testSearchDataobjectsReturnDatasets() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.customizeRequestParam().param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DATAOBJECT);
        String projectAdminJwt = manageSecurity(PATH + DATAOBJECTS_DATASETS_SEARCH,
                                                RequestMethod.GET, DEFAULT_USER_EMAIL,
                                                DefaultRole.PROJECT_ADMIN.name());

        performGet(PATH + DATAOBJECTS_DATASETS_SEARCH, projectAdminJwt,
                   requestBuilderCustomizer, "Error searching datasets via dataobjects");
    }

    @Test
    public final void testSearchDataobjectsReturnDatasetsNoCriterion() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        final RequestParamBuilder builder = RequestParamBuilder.build();
        performDefaultGet(PATH + DATAOBJECTS_DATASETS_SEARCH, expectations,
                          "Error searching datasets via dataobjects", builder);
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
        performDefaultGet(PATH + DOCUMENTS_URN, expectations, "Error retrieving a document",
                          DOCUMENT.getIpId());
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
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_ONE_DOCUMENT);
        performDefaultGet(PATH + DOCUMENTS_SEARCH, expectations, "Error searching documents", builder);
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

        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS)
                .param("sort", CatalogControllerTestUtils.SORT);
        performDefaultGet(PATH + DATASETS_SEARCH, expectations, "Error searching documents", builder);
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
        setAuthorities(PATH + DATASETS_URN, RequestMethod.GET, DEFAULT_ROLE);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers
                                 .jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].rel", Matchers.is("self")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].href",
                                                        Matchers.startsWith(
                                                                "http://localhost:8080" + PATH
                                                                        + DATASETS_SEARCH
                                                                        + "/URN:AIP:DATASET:")));
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet(PATH + DATASETS_SEARCH, expectations,
                          "Error searching datasets", builder);
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
        setAuthorities(PATH + DATAOBJECTS_SEARCH_WITH_FACETS, RequestMethod.GET,
                       DEFAULT_ROLE);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content", Matchers.notNullValue()));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].rel", Matchers.either(
                Matchers.is(DatasetLinkAdder.LINK_TO_DATAOBJECTS)).or(Matchers.is("self"))));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content.[0].links.[0].href", Matchers.either(
                Matchers.startsWith("http://localhost:8080" + PATH
                                            + DATAOBJECTS_SEARCH_WITH_FACETS + "?q"))
                .or(Matchers.startsWith(
                        "http://localhost:8080" + PATH + DATASETS_SEARCH))));
        final RequestParamBuilder builder = RequestParamBuilder.build()
                .param("q", CatalogControllerTestUtils.Q_FINDS_TWO_DATASETS);
        performDefaultGet(PATH + DATASETS_SEARCH, expectations,
                          "Error searching datasets", builder);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
