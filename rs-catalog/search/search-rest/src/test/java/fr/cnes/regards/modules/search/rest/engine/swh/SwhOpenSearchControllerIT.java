/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.swh;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.adapters.ClassAdapter;
import fr.cnes.regards.framework.gson.strategy.SerializationExclusionStrategy;
import fr.cnes.regards.framework.microservice.manager.MicroserviceConfiguration;
import fr.cnes.regards.framework.module.manager.*;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.search.dao.ISearchEngineConfRepository;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.rest.engine.AbstractEngineIT;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.OpenSearchEngine;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.MimeType;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(locations = { "classpath:test.properties" },
    properties = { "regards.tenant=swh", "spring.jpa.properties.hibernate.default_schema=swh" })
public class SwhOpenSearchControllerIT extends AbstractEngineIT {

    public static final OffsetDateTime CREATION_DATE = OffsetDateTime.now();

    private static final Logger LOGGER = LoggerFactory.getLogger(SwhOpenSearchControllerIT.class);

    private static final String ENGINE_TYPE = "opensearch";

    private final OffsetDateTime lastUpdateSpot4 = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    private final OffsetDateTime lastUpdateSpot5 = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    @Autowired
    private Gson gson;

    @Autowired(required = false)
    private List<IModuleManager<?>> managers;

    @Autowired
    private GsonBuilderFactory gsonBuilderFactory;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private ISearchEngineConfRepository seRepo;

    @Autowired
    private IModelRepository modelRepo;

    @Autowired
    private IDatasetRepository dataSetRepo;

    @Autowired
    private IEsRepository esRepo;

    @Autowired
    private IAttributeModelRepository attrModelRepo;

    @Autowired
    private IRuntimeTenantResolver runTimeTenantResoler;

    private Gson configGson;

    private Gson configItemGson;

    private Dataset swhDataset;

    @Override
    @Before
    public void prepareData() throws ModuleException, InterruptedException {

        runTimeTenantResoler.forceTenant(getDefaultTenant());
        seRepo.deleteAll();
        pluginRepo.deleteAll();
        dataSetRepo.deleteAll();
        modelRepo.deleteAll();
        attrModelRepo.deleteAll();
        esRepo.deleteAll(getDefaultTenant());

        prepareProject();

        // - Import models
        Model dataModel = modelService.importModel(this.getClass().getResourceAsStream("model-SWH_Data.xml"));
        Model datasetModel = modelService.importModel(this.getClass().getResourceAsStream("model-SWH_Dataset.xml"));

        // - Manage attribute model retrieval
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocsFor(Mockito.any())).thenAnswer(invocation -> {
            EntityType type = invocation.getArgument(0);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(type));
        });
        Mockito.when(datasetClientMock.getModelAttrAssocsForDataInDataset(Mockito.any())).thenAnswer(invocation -> {
            // UniformResourceName datasetUrn = invocation.getArgumentAt(0, UniformResourceName.class);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(EntityType.DATA));
        });

        // - Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);
        jsoniterAttributeFactory.refresh(getDefaultTenant(), atts);

        // - Manage attribute cache
        List<EntityModel<AttributeModel>> resAtts = new ArrayList<>();
        atts.forEach(att -> resAtts.add(EntityModel.of(att)));
        Mockito.when(attributeModelClientMock.getAttributes(null, null)).thenReturn(ResponseEntity.ok(resAtts));
        finder.refresh(getDefaultTenant());

        // Dataset
        swhDataset = new Dataset(datasetModel, getDefaultTenant(), "Mapping_DS_MDD", "Mapping_DS_MDD");
        indexerService.saveEntity(getDefaultTenant(), swhDataset);

        // Create data
        prepareData(dataModel, swhDataset.getIpId());

        // Refresh index to be sure data is available for requesting
        indexerService.refresh(getDefaultTenant());

        // initPlugins();
        try (JsonReader reader = new JsonReader(new InputStreamReader(this.getClass()
                                                                          .getResourceAsStream(
                                                                              "config-rs-catalog-swh.json"),
                                                                      StandardCharsets.UTF_8))) {
            MicroserviceConfiguration microConfig = getConfigGson().fromJson(reader, MicroserviceConfiguration.class);
            for (ModuleConfiguration module : microConfig.getModules()) {
                for (IModuleManager<?> manager : managers) {
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

    private void prepareData(Model dataModel, UniformResourceName dataset) {
        List<DataObject> dataObjects = new ArrayList<>();

        try (JsonReader reader = new JsonReader(new InputStreamReader(this.getClass()
                                                                          .getResourceAsStream("products.json")))) {

            JsonArray fp = gson.fromJson(reader, JsonArray.class);
            for (JsonElement entity : fp) {
                JsonObject feature = entity.getAsJsonObject().getAsJsonObject("content");
                JsonObject properties = feature.getAsJsonObject("properties");
                String label = feature.getAsJsonPrimitive("label").getAsString();
                String providerId = feature.getAsJsonPrimitive("providerId").getAsString();

                DataObject dataObject = new DataObject(dataModel, getDefaultTenant(), providerId, label);
                UniformResourceName ipId = UniformResourceName.fromString(feature.getAsJsonPrimitive("id")
                                                                                 .getAsString());
                dataObject.setIpId(ipId);
                dataObject.setCreationDate(CREATION_DATE);
                // Not all products share the same platform name; so we use it to have two different date
                if (getStringProperty(properties, "PlatformName").get().getValue().equals("SPOT4")) {
                    dataObject.setLastUpdate(lastUpdateSpot4);
                } else {
                    dataObject.setLastUpdate(lastUpdateSpot5);
                }

                addFiles(dataObject, feature.getAsJsonObject("files"));
                dataObject.setNormalizedGeometry(IGeometry.polygon(IGeometry.toPolygonCoordinates(IGeometry.toLinearRingCoordinates(
                    IGeometry.position(10.0, 10.0),
                    IGeometry.position(10.0, 30.0),
                    IGeometry.position(30.0, 30.0),
                    IGeometry.position(30.0, 10.0),
                    IGeometry.position(10.0, 10.0)))));

                dataObject.setGeometry(dataObject.getNormalizedGeometry());

                addProperty(dataObject, getStringProperty(properties, "CoupledMode"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesMI"));
                addProperty(dataObject, getStringProperty(properties, "GridReference"));
                addProperty(dataObject, getStringProperty(properties, "PlatformName"));
                addProperty(dataObject, getStringProperty(properties, "ProcessingLevel"));
                addProperty(dataObject, getStringProperty(properties, "PolygonCenterWKT"));
                addProperty(dataObject, getStringProperty(properties, "DataDate"));
                addProperty(dataObject, getIntProperty(properties, "CloudCover"));
                addProperty(dataObject, getArrayProperty(properties, "CouplingModes"));
                addProperty(dataObject, getIntProperty(properties, "FreeCloudPercentage"));
                addProperty(dataObject, getStringProperty(properties, "ImageQualityIndicator"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesS"));
                addProperty(dataObject, getDoubleProperty(properties, "ViewingAngle"));
                addProperty(dataObject, getStringProperty(properties, "A23Code"));
                addProperty(dataObject, getStringProperty(properties, "ProductionDate"));
                addProperty(dataObject, getIntProperty(properties, "ShiftValue"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesTHX"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesHM_S"));
                addProperty(dataObject, getStringProperty(properties, "PolygonWKT"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesTHR_S"));
                addProperty(dataObject, getStringProperty(properties, "GeometricProcessing"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesHMB2"));
                addProperty(dataObject, getIntProperty(properties, "NbRows"));
                addProperty(dataObject, getIntProperty(properties, "SceneCount"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesI2"));
                addProperty(dataObject, getStringProperty(properties, "SceneID"));
                addProperty(dataObject, getStringProperty(properties, "SensorCode"));
                addProperty(dataObject, getStringProperty(properties, "InstrumentName"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesTHX_S"));
                addProperty(dataObject, getStringProperty(properties, "CloudCoverquotes"));
                addProperty(dataObject, getStringProperty(properties, "A21Code"));
                addProperty(dataObject, getIntProperty(properties, "NbBands"));
                addProperty(dataObject, getIntProperty(properties, "SceneRank"));
                addProperty(dataObject, getIntProperty(properties, "gainNumber"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesTHR"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesM2"));
                addProperty(dataObject, getIntProperty(properties, "DataFileSize"));
                addProperty(dataObject, getStringProperty(properties, "Station"));
                addProperty(dataObject, getStringProperty(properties, "NbCols"));
                addProperty(dataObject, getDoubleProperty(properties, "SunElevation"));
                addProperty(dataObject, getStringProperty(properties, "SwathMode"));
                addProperty(dataObject, getDoubleProperty(properties, "SunAzimuth"));
                addProperty(dataObject, getStringProperty(properties, "DataID"));
                addProperty(dataObject, getArrayProperty(properties, "CoupledScenesPX"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesX2"));
                addProperty(dataObject, getStringProperty(properties, "GeraldName"));
                addProperty(dataObject, getStringProperty(properties, "SceneName"));
                addProperty(dataObject, getStringProperty(properties, "CoupledScenesHMX"));
                addProperty(dataObject, getDoubleProperty(properties, "IncidenceAngle"));

                // Attach to dataset
                dataObject.addTags(dataset.toString());

                dataObjects.add(dataObject);
            }

            // Create data
            indexerService.saveBulkEntities(getDefaultTenant(), dataObjects);
        } catch (IOException e) {
            Assert.fail("Cannot read geojson file");
        }
    }

    private void addFiles(DataObject dataObject, JsonObject files) {
        JsonArray rawdata = files.getAsJsonArray("RAWDATA");
        if (rawdata != null) {
            addFiles(dataObject, rawdata, DataType.RAWDATA);
        }
        JsonArray thumbnail = files.getAsJsonArray("THUMBNAIL");
        if (thumbnail != null) {
            addFiles(dataObject, thumbnail, DataType.THUMBNAIL);
        }
    }

    private void addFiles(DataObject dataObject, JsonArray fileArray, DataType dataType) {
        dataObject.getFiles().putAll(dataType, getDatafile(fileArray, dataType));
    }

    private List<DataFile> getDatafile(JsonArray fileArray, DataType dataType) {
        List<DataFile> dataFiles = new ArrayList<>();
        for (JsonElement file : fileArray) {
            DataFile dataFile = buildDataFile(file, dataType);
            dataFiles.add(dataFile);
        }
        return dataFiles;
    }

    private DataFile buildDataFile(JsonElement file, DataType dataType) {
        JsonObject fileAsObj = file.getAsJsonObject();
        String filename = fileAsObj.get("filename").getAsString();
        String uri = fileAsObj.get("uri").getAsString();
        MimeType mimeType = MimeType.valueOf(fileAsObj.get("mimeType").getAsString());
        Boolean online = fileAsObj.get("online").getAsBoolean();
        Boolean reference = fileAsObj.get("reference").getAsBoolean();
        DataFile dataFile = DataFile.build(dataType, filename, uri, mimeType, online, reference);

        dataFile.setChecksum(fileAsObj.get("checksum").getAsString());
        JsonElement imageHeight = fileAsObj.get("imageHeight");
        if (imageHeight != null) {
            dataFile.setImageHeight(imageHeight.getAsDouble());
        }
        JsonElement imageWidth = fileAsObj.get("imageWidth");
        if (imageWidth != null) {
            dataFile.setImageWidth(imageWidth.getAsDouble());
        }
        dataFile.setFilesize(fileAsObj.get("filesize").getAsLong());
        return dataFile;
    }

    private void addProperty(DataObject dataObject, Optional<IProperty> propertyOpt) {
        propertyOpt.ifPresent(dataObject::addProperty);
    }

    private Optional<IProperty> getDoubleProperty(JsonObject properties, String attrName) {
        JsonPrimitive value = properties.getAsJsonPrimitive(attrName);
        if (value != null) {
            return Optional.of(IProperty.buildDouble(attrName, value.getAsDouble()));
        }
        return Optional.empty();
    }

    private Optional<IProperty> getStringProperty(JsonObject properties, String attrName) {
        JsonPrimitive value = properties.getAsJsonPrimitive(attrName);
        if (value != null) {
            return Optional.of(IProperty.buildString(attrName, value.getAsString()));
        }
        return Optional.empty();
    }

    private Optional<IProperty> getIntProperty(JsonObject properties, String attrName) {
        JsonPrimitive value = properties.getAsJsonPrimitive(attrName);
        if (value != null) {
            return Optional.of(IProperty.buildInteger(attrName, value.getAsInt()));
        }
        return Optional.empty();
    }

    private Optional<IProperty> getArrayProperty(JsonObject properties, String attrName) {
        JsonArray value = properties.getAsJsonArray(attrName);
        List<String> collection = new ArrayList<>();
        if (value != null) {
            for (JsonElement el : value) {
                collection.add(el.getAsString());
            }
            return Optional.of(IProperty.buildStringCollection(attrName, collection));
        }
        return Optional.empty();
    }

    @Test
    public void getOpenSearchDescription() throws XPathExpressionException {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        customizer.addParameter("token", "public_token");
        String atomUrl = "OpenSearchDescription/Url[@type='" + MediaType.APPLICATION_ATOM_XML_VALUE + "']";
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "[count(Parameter)=65]").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
                                                   atomUrl + "/Parameter[@name='illuminationAzimuthAngle' and @value='{eo:illuminationAzimuthAngle}']")
                                               .exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='Platform' and @value='{eo:platform}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='Instrument' and @value='{eo:instrument}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='CloudCover' and @value='{eo:cloudCover}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
                                                   atomUrl + "/Parameter[@name='illuminationElevationAngle' and @value='{eo:illuminationElevationAngle}']")
                                               .exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='ProcessingDate' and @value='{eo:processingDate}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='acquisitionStation' and @value='{eo:acquisitionStation}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='SensorCode' and @value='{eo:sensorType}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='SwathIdentifier' and @value='{eo:swathIdentifier}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='minimumIncidenceAngle' and @value='{eo:minimumIncidenceAngle}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(
            atomUrl + "/Parameter[@name='ProcessingLevel' and @value='{eo:processingLevel}']").exists());
        customizer.expect(MockMvcResultMatchers.xpath(atomUrl + "/Parameter[@name='updated' and @value='{updated}']")
                                               .exists());
        long startTime = System.currentTimeMillis();
        performDefaultGet(
            SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA,
            customizer,
            "open search description error",
            ENGINE_TYPE,
            swhDataset.getIpId().toString(),
            OpenSearchEngine.EXTRA_DESCRIPTION);
        logDuration(startTime);
    }

    @Test
    public void searchDataobject() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        long startTime = System.currentTimeMillis();
        customizer.headers().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "0");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("SceneName", "011-012_S4_234-211-0_2011-04-17-04-06-46_HRVIR-2_M_DT_BM");
        customizer.addParameter("Platform", "SPOT4");
        customizer.expectValue("$.properties.totalResults", 1);
        customizer.expectValue("$.features.length()", 1);
        customizer.expectValue("$.features[0].properties.productInformation.processingLevel", "1A");
        customizer.expectValue("$.features[0].properties.productInformation.processingDate", "2019-09-17T18:49:33Z");
        customizer.expectValue("$.features[0].properties.productInformation.cloudCover", 87);
        customizer.expectValue("$.features[0].properties.acquisitionAngles.minimumIncidenceAngle", -0.57049401836);
        customizer.expectValue("$.features[0].properties.acquisitionAngles.illuminationAzimuthAngle", 170.1602678);
        customizer.expectValue("$.features[0].properties.acquisitionParameters.acquisitionStation", "BM");
        customizer.expectValue(
            "$.features[0].properties.acquisitionInformation[0].acquisitionParameters.operationalMode",
            "M");
        customizer.expectValue(
            "$.features[0].properties.acquisitionInformation[0].acquisitionParameters.swathIdentifier",
            "FULL");
        customizer.expectValue("$.features[0].properties.acquisitionInformation[0].instrument.instrumentShortName",
                               "HRVIR2");
        customizer.expectValue("$.features[0].properties.PlatformName", "SPOT4");
        customizer.expectValue("$.features[0].properties.creationDate",
                               CREATION_DATE.withOffsetSameInstant(ZoneOffset.UTC)
                                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        customizer.expectValue("$.features[0].properties.updated", lastUpdateSpot4.toString());
        String thumbnailURL = "https://regards.cnes.fr/api/v1/rs-catalog/downloads/URN:AIP:DATA:swh:a4456e48-3e24-3c32-baea-093b1f63e85d:V1/files/19273a25e605bf83658c27890576b041?scope=swh";
        customizer.expectValue("$.features[0].properties.thumbnail", thumbnailURL);
        // There is no quicklook inside this product, so quicklook value is also thumbnailURL
        customizer.expectValue("$.features[0].properties.quicklook", thumbnailURL);
        customizer.expectValue("$.features[0].properties.temporal.beginningDateTime", "2011-04-17T04:06:46Z");
        customizer.expectValue("$.features[0].properties.temporal.endingDateTime", "2011-04-17T04:06:46Z");

        customizer.expectValueMatchesPattern("$.features[0].properties.services.download.url",
                                             "https://regards.cnes.fr/api/v1/rs-catalog/downloads/URN:AIP:DATA:swh:a4456e48-3e24-3c32-baea-093b1f63e85d:V1/files/.*");

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
        logDuration(startTime);
    }

    @Test
    public void searchDataobjectExcludingAllResults() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        long startTime = System.currentTimeMillis();
        customizer.headers().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        customizer.addParameter("Platform", "SPOT1515");

        customizer.expectValue("$.properties.totalResults", 0);
        customizer.expectValue("$.features.length()", 0);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
        logDuration(startTime);
    }

    @Test
    public void searchDataobjectsWithUpdatedFilter() {
        searchDataobjectWithUpdatedFilter(lastUpdateSpot5.toString(), 148);
        searchDataobjectWithUpdatedFilter(lastUpdateSpot4.toString(), 400);
    }

    private void searchDataobjectWithUpdatedFilter(String updated, int totalResults) {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        long startTime = System.currentTimeMillis();
        customizer.headers().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        customizer.addParameter("page", "0");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("updated", updated);
        customizer.expectValue("$.properties.totalResults", totalResults);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
        logDuration(startTime);
    }

    @Test
    public void searchDataAtomReturningEOAttrs() throws XPathExpressionException {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.headers().setAccept(Collections.singletonList(MediaType.APPLICATION_ATOM_XML));
        customizer.addParameter("page", "0");
        customizer.addParameter("maxRecords", "100");
        customizer.addParameter("SceneName", "001-003_S4_053-236-8_1999-06-13-10-34-54_HRVIR-1_I_DT_KK");
        customizer.expect(MockMvcResultMatchers.xpath("feed/itemsPerPage").string(Matchers.is("100")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/totalResults").string(Matchers.is("1")));
        customizer.expect(MockMvcResultMatchers.xpath("feed/startIndex").string(Matchers.is("1")));
        customizer.expect(MockMvcResultMatchers.xpath(
                                                   "feed/entry[1]/metaDataProperty/EarthObservationMetaData/processing/ProcessingInformation/processingDate")
                                               .string(Matchers.is("\"2019-08-21T00:03:09Z\"")));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    private void logDuration(long startTime) {
        LOGGER.info(">>>>>>>>>>>>>>>>>>> Request took {} milliseconds <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
                    System.currentTimeMillis() - startTime);
    }
}
