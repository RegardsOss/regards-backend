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
package fr.cnes.regards.modules.search.rest.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.service.IAttributeModelService;
import fr.cnes.regards.modules.models.service.ModelService;
import fr.cnes.regards.modules.search.rest.SearchEngineController;

/**
 * Search engine tests
 *
 * @author Marc Sordi
 *
 */
// @TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=engines",
// "regards.elasticsearch.address=@regards.IT.elasticsearch.host@", "regards.elasticsearch.cluster.name=regards",
// "regards.elasticsearch.tcp.port=@regards.IT.elasticsearch.port@" })
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class SearchEngineControllerIT extends AbstractRegardsTransactionalIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineControllerIT.class);

    private static final String DATASET_ID = "datatest";

    private static final String ENGINE_TYPE = "opensearch";

    /**
     * Model properties
     */

    // Common properties
    private static final String ABSTRACT = "abstract";

    // Galaxy properties
    private static final String GALAXY = "galaxy";

    // Star properties
    private static final String STAR = "star";

    // Star system properties
    private static final String STAR_SYSTEM = "startSystem";

    // Planet properties
    private static final String PLANET = "planet";

    private static final String PLANET_TYPE = "planet_type";

    private static final String PLANET_TYPE_GAS_GIANT = "Gas giant";

    private static final String PLANET_TYPE_ICE_GIANT = "Ice giant";

    private static final String PLANET_TYPE_TELLURIC = "Telluric";

    @Autowired
    private ModelService modelService;

    @Autowired
    private IEsRepository esRepository;

    @Autowired
    private IAttributeModelService attributeModelService;

    // TODO mock as ADMIN
    @Autowired
    private IProjectUsersClient projectUserClient;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    private void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
    }

    @Before
    public void prepareData() throws ModuleException {

        // Bypass access rights
        Mockito.when(projectUserClient.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(Boolean.TRUE));

        initIndex(DEFAULT_TENANT);

        // - Import models
        // COLLECTION : Galaxy
        Model galaxyModel = modelService.importModel(this.getClass().getResourceAsStream("collection_galaxy.xml"));
        // COLLECTION : Star
        Model starModel = modelService.importModel(this.getClass().getResourceAsStream("collection_star.xml"));
        // DATASET : Star system
        Model starSystemModel = modelService
                .importModel(this.getClass().getResourceAsStream("dataset_star_system.xml"));
        // DATA : Planet
        Model planetModel = modelService.importModel(this.getClass().getResourceAsStream("data_planet.xml"));

        // - Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(DEFAULT_TENANT, atts);

        // Create data
        esRepository.saveBulk(DEFAULT_TENANT, createGalaxies(galaxyModel));
        esRepository.saveBulk(DEFAULT_TENANT, createStars(starModel));
        esRepository.saveBulk(DEFAULT_TENANT, createStarSystems(starSystemModel));
        esRepository.saveBulk(DEFAULT_TENANT, createPlanets(planetModel));
    }

    private List<Collection> createGalaxies(Model galaxyModel) {
        Collection milkyWay = new Collection(galaxyModel, DEFAULT_TENANT, "Milky way");
        milkyWay.addProperty(AttributeBuilder.buildString(GALAXY, "Milky way"));
        milkyWay.addProperty(AttributeBuilder
                .buildString(ABSTRACT, "The Milky Way is the galaxy that contains our Solar System."));
        return Arrays.asList(milkyWay);
    }

    private List<Collection> createStars(Model starModel) {
        Collection sun = new Collection(starModel, DEFAULT_TENANT, "Sun");
        sun.addProperty(AttributeBuilder.buildString(STAR, "Sun"));
        sun.addProperty(AttributeBuilder.buildString(ABSTRACT,
                                                     "The Sun is the star at the center of the Solar System."));
        return Arrays.asList(sun);
    }

    private List<Dataset> createStarSystems(Model starSystemModel) {
        Dataset solarSystem = new Dataset(starSystemModel, DEFAULT_TENANT, "Solar system");
        solarSystem.addProperty(AttributeBuilder.buildString(STAR_SYSTEM, "Solar system"));
        return Arrays.asList(solarSystem);
    }

    private List<DataObject> createPlanets(Model planetModel) {

        DataObject mercury = new DataObject(planetModel, DEFAULT_TENANT, "Mercury");
        mercury.addProperty(AttributeBuilder.buildString(PLANET, "Mercury"));
        mercury.addProperty(AttributeBuilder.buildString(PLANET_TYPE, PLANET_TYPE_TELLURIC));

        return Arrays.asList(mercury);
    }

    @Test
    public void test() throws ModuleException {
        // Nothing to do : simulate @Before
    }

    @Test
    public void searchAll() {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        // customizer.customizeHeaders().setContentType(MediaType.APPLICATION_ATOM_XML);
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));

        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        // customizer.customizeRequestParam().param("facets", "toto", "titi");
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "2");
        performDefaultGet(SearchEngineController.TYPE_MAPPING, customizer, "Search all error", ENGINE_TYPE);

        // RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        // performDefaultGet(SearchEngineController.TYPE_MAPPING, expectations, "Error searching", DATASET_ID,
        // ENGINE_TYPE);
    }

    @Test
    public void basicExtraSearch() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        // RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        // performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.EXTRA_MAPPING, expectations,
        // "Error searching", DATASET_ID, ENGINE_TYPE, "opensearchdescription.xml");
    }
}
