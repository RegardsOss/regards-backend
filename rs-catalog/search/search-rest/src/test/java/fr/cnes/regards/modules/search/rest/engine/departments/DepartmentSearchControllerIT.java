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
package fr.cnes.regards.modules.search.rest.engine.departments;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureCollection;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.adapters.ClassAdapter;
import fr.cnes.regards.framework.gson.strategy.SerializationExclusionStrategy;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.manager.MicroserviceConfiguration;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.module.manager.IModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItemAdapter;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.dam.domain.models.Model;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeModel;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.rest.engine.AbstractEngineIT;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;

/**
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=departments", "spring.jpa.properties.hibernate.default_schema=departments" })
@MultitenantTransactional
public class DepartmentSearchControllerIT extends AbstractEngineIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepartmentSearchControllerIT.class);

    private static final String ENGINE_TYPE = "opensearch";

    @Autowired
    private Gson gson;

    @Autowired(required = false)
    private List<IModuleConfigurationManager> managers;

    @Autowired
    private GsonBuilderFactory gsonBuilderFactory;

    private Gson configGson;

    private Gson configItemGson;

    private Dataset france;

    @Override
    @Before
    public void prepareData() throws ModuleException, InterruptedException {

        prepareProject();

        // - Import models
        Model departmentModel = modelService.importModel(this.getClass().getResourceAsStream("model-Departement.xml"));
        Model countryModel = modelService.importModel(this.getClass().getResourceAsStream("model-Pays.xml"));

        // - Manage attribute model retrieval
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocsFor(Mockito.any())).thenAnswer(invocation -> {
            EntityType type = invocation.getArgumentAt(0, EntityType.class);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(type));
        });
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocsForDataInDataset(Mockito.any()))
                .thenAnswer(invocation -> {
                    // UniformResourceName datasetUrn = invocation.getArgumentAt(0, UniformResourceName.class);
                    return ResponseEntity.ok(modelService.getModelAttrAssocsFor(EntityType.DATA));
                });

        // - Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);

        // - Manage attribute cache
        List<Resource<AttributeModel>> resAtts = new ArrayList<>();
        atts.forEach(att -> resAtts.add(new Resource<AttributeModel>(att)));
        Mockito.when(attributeModelClientMock.getAttributes(null, null)).thenReturn(ResponseEntity.ok(resAtts));
        finder.refresh(getDefaultTenant());

        // Dataset
        france = new Dataset(countryModel, getDefaultTenant(), "France", "France");
        indexerService.saveEntity(getDefaultTenant(), france);

        // Create data
        prepareDepartments(departmentModel, france.getIpId());

        // Refresh index to be sure data is available for requesting
        indexerService.refresh(getDefaultTenant());

        // initPlugins();
        try (JsonReader reader = new JsonReader(
                new InputStreamReader(this.getClass().getResourceAsStream("config-rs-catalog.json"), "UTF-8"))) {
            MicroserviceConfiguration microConfig = getConfigGson().fromJson(reader, MicroserviceConfiguration.class);
            for (ModuleConfiguration module : microConfig.getModules()) {
                for (IModuleConfigurationManager manager : managers) {
                    if (manager.isApplicable(module)) {
                        manager.importConfigurationAndLog(module);
                    }
                }
            }
        } catch (Exception e) {
            Assert.fail("Cannot read plugin configuration");
        }
    }

    private Gson getConfigGson() {
        if (configGson == null) {
            // Create GSON for generic module configuration item adapter without itself! (avoid stackOverflow)
            GsonBuilder customBuilder = gsonBuilderFactory.newBuilder();
            customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
            customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
            configItemGson = customBuilder.create();

            // Create GSON with specific adapter to dynamically analyze parameterized type
            customBuilder = gsonBuilderFactory.newBuilder();
            customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
            customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
            customBuilder.registerTypeHierarchyAdapter(ModuleConfigurationItem.class,
                                                       new ModuleConfigurationItemAdapter(configItemGson));
            configGson = customBuilder.create();
        }
        return configGson;
    }

    private void prepareDepartments(Model departmentModel, UniformResourceName dataset) {

        List<DataObject> departments = new ArrayList<>();
        Random random = new Random();

        try (JsonReader reader = new JsonReader(new InputStreamReader(
                this.getClass().getResourceAsStream("departements-version-simplifiee.geojson")))) {

            FeatureCollection fc = gson.fromJson(reader, FeatureCollection.class);
            for (Feature feature : fc.getFeatures()) {

                String name = (String) feature.getProperties().get("nom");
                DataObject department = new DataObject(departmentModel, getDefaultTenant(), name, name);
                department.setCreationDate(OffsetDateTime.now());
                department.setGeometry(feature.getGeometry());
                department.setWgs84(feature.getGeometry());
                department.addProperty(AttributeBuilder.buildString("Code",
                                                                    (String) feature.getProperties().get("code")));
                department.addProperty(AttributeBuilder.buildLong("FileSize", random.nextLong()));
                department.addProperty(AttributeBuilder.buildString("Name", name));

                // Attach to dataset
                department.addTags(dataset.toString());

                departments.add(department);
            }

            // Create data
            indexerService.saveBulkEntities(getDefaultTenant(), departments);
        } catch (IOException e) {
            Assert.fail("Cannot read geojson file");
        }
    }

    @Test
    public void getOpenSearchDescription() throws XPathExpressionException {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));

        customizer.customizeRequestParam().param("token", "public_token");

        long startTime = System.currentTimeMillis();
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA, customizer,
                          "open search description error", ENGINE_TYPE, france.getIpId().toString(),
                          OpenSearchEngine.EXTRA_DESCRIPTION);
        logDuration(startTime);
    }

    @Test
    public void searchDataobjects() {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        long startTime = System.currentTimeMillis();
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
        logDuration(startTime);
    }

    private void logDuration(long startTime) {
        LOGGER.info(">>>>>>>>>>>>>>>>>>> Request took {} milliseconds <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
                    System.currentTimeMillis() - startTime);
    }
}
