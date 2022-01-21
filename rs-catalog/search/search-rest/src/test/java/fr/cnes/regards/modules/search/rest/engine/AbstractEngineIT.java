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
package fr.cnes.regards.modules.search.rest.engine;

import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.jsoniter.property.JsoniterAttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.service.IIndexerService;
import fr.cnes.regards.modules.model.client.IAttributeModelClient;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.ModelService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.OpenSearchEngine;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.regards.RegardsExtension;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.map.HashedMap;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Engine common methods
 * @author Marc Sordi
 */
@DirtiesContext
public abstract class AbstractEngineIT extends AbstractRegardsTransactionalIT {

    /**
     * Model properties
     */

    // Common properties
    protected static final String ABSTRACT = "abstract";

    // Galaxy properties
    protected static final String GALAXY = "galaxy";

    protected static final String MILKY_WAY = "Milky way";

    // Star properties
    protected static final String STAR = "star";

    protected static final String SUN = "Sun";

    // Star system properties
    protected static final String STAR_SYSTEM = "starSystem";

    protected static final String NUMBER_OF_PLANETS = "numberOfPlanets";

    protected static final String SOLAR_SYSTEM = "Solar planetary system";

    protected static final String KEPLER_90 = "Kepler 90 planetary system";

    protected static final String KEPLER_16 = "Kepler 16 planetary system";

    protected static final String PEGASI_51 = "Pegasi 51 planetary system";

    protected static final String STUDY_DATE = "studyDate";

    protected static final String RESEARCH_LAB = "researchLab";

    protected static final String DISTANCE_TO_SOLAR_SYSTEM = "distanceToSolarSystem";

    // Planet properties
    protected static final String PLANET = "planet";

    protected static final String PLANET_TYPE = "planet_type";

    protected static final String PLANET_TYPE_GAS_GIANT = "Gas giant";

    protected static final String PLANET_TYPE_ICE_GIANT = "Ice giant";

    protected static final String PLANET_TYPE_TELLURIC = "Telluric";

    protected static final String PLANET_DIAMETER = "diameter";

    protected static final String PLANET_SUN_DISTANCE = "sun_distance";

    protected static final String ORIGINE = "origine";

    protected static final String START_DATE = "startDate";

    protected static final String STOP_DATE = "stopDate";

    protected static final String PLANET_PARAMS = "params";

    protected static final String MERCURY = "Mercury";

    protected static final String JUPITER = "Jupiter";

    protected static final String ALPHA_PARAM = "alpha";

    protected static final OffsetDateTime startDateValue = OffsetDateTime.now();

    protected static final OffsetDateTime stopDateValue = OffsetDateTime.now().plusMonths(36);

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected IEsRepository esRepository;

    @Autowired
    protected IAttributeModelService attributeModelService;

    @Autowired
    protected IProjectUsersClient projectUserClientMock;

    @Autowired
    protected IAttributeModelClient attributeModelClientMock;

    @Autowired
    protected IAttributeFinder finder;

    @Autowired
    protected IProjectsClient projectsClientMock;

    @Autowired
    protected IModelAttrAssocClient modelAttrAssocClientMock;

    @Autowired
    protected MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    @Autowired
    protected JsoniterAttributeModelPropertyTypeFinder jsoniterAttributeFactory;

    @Autowired
    protected IIndexerService indexerService;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected ISearchEngineConfigurationService searchEngineService;

    @Autowired
    protected IDatasetClient datasetClientMock;

    // Keep reference to astronomical object

    protected Map<String, AbstractEntity<?>> astroObjects = new HashedMap<>();

    protected SearchEngineConfiguration openSearchEngineConf;

    protected PluginConfiguration openSearchPluginConf;

    protected Dataset solarSystem;

    GsonBuilder builder = new GsonBuilder();

    protected void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
    }

    protected void prepareProject() {

        // Needed for test on date in opensearch descriptors. Date are generated in test and compare with date generated
        // by elasticsearch on test server.
        // Test server is in UTC timezone, so to do comparasion we have to be in the same timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Manage project
        Project project = new Project(1L, "Solar system project", "http://plop/icon.png", true, "SolarSystem");
        project.setHost("http://regards/solarsystem");
        ResponseEntity<EntityModel<Project>> response = ResponseEntity.ok(new EntityModel<>(project));
        Mockito.when(projectsClientMock.retrieveProject(Mockito.anyString())).thenReturn(response);

        // Bypass method access rights
        List<String> relativeUrlPaths = new ArrayList<>();

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_ALL_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_ALL_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_ENTITY_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_COLLECTION_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATASETS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATASETS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_DATASET_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.GET_DATASET_DESCRIPTION_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_DATAOBJECT_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING);

        for (String relativeUrlPath : relativeUrlPaths) {
            setAuthorities(SearchEngineMappings.TYPE_MAPPING + relativeUrlPath, RequestMethod.GET, getDefaultRole());
        }

        initIndex(getDefaultTenant());

        manageAccessRights();
    }

    @Before
    public void prepareData() throws ModuleException, InterruptedException {

        prepareProject();

        astroObjects.clear();

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

        Model testModel = modelService.importModel(this.getClass().getResourceAsStream("data_model_test.xml"));

        // - Manage attribute model retrieval
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocsFor(Mockito.any())).thenAnswer(invocation -> {
            EntityType type = invocation.getArgument(0);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(type));
        });
        Mockito.when(datasetClientMock.getModelAttrAssocsForDataInDataset(Mockito.any())).thenAnswer(invocation -> {
            // UniformResourceName datasetUrn = invocation.getArgumentAt(0, UniformResourceName.class);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(EntityType.DATA));
        });
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocs(Mockito.any())).thenAnswer(invocation -> {
            String modelName = invocation.getArgument(0);
            return ResponseEntity.ok(modelService.getModelAttrAssocs(modelName).stream()
                    .map(a -> new EntityModel<ModelAttrAssoc>(a)).collect(Collectors.toList()));
        });

        // - Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);
        jsoniterAttributeFactory.refresh(getDefaultTenant(), atts);

        // - Manage attribute cache
        List<EntityModel<AttributeModel>> resAtts = new ArrayList<>();
        atts.forEach(att -> resAtts.add(new EntityModel<AttributeModel>(att)));
        Mockito.when(attributeModelClientMock.getAttributes(null, null)).thenReturn(ResponseEntity.ok(resAtts));
        finder.refresh(getDefaultTenant());

        // Create data
        createData(galaxyModel, starModel, starSystemModel, planetModel);

        // Add test datas
        indexerService.saveBulkEntities(getDefaultTenant(), createTestData(testModel));

        // Refresh index to be sure data is available for requesting
        indexerService.refresh(getDefaultTenant());

        initPlugins();
    }

    private void createData(Model galaxyModel, Model starModel, Model starSystemModel, Model planetModel) {
        indexerService.saveBulkEntities(getDefaultTenant(), createGalaxies(galaxyModel));
        indexerService.saveBulkEntities(getDefaultTenant(), createStars(starModel));

        // SOLAR SYSTEM
        solarSystem = createStelarSystem(starSystemModel, SOLAR_SYSTEM);
        List<DataObject> solarPlanets = createSolarSystemPlanets(planetModel, solarSystem.getIpId());
        solarSystem.addProperty(IProperty.buildDate(STUDY_DATE, OffsetDateTime
                .of(LocalDate.of(2020, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC)));
        solarSystem.addProperty(IProperty.buildInteger(NUMBER_OF_PLANETS, solarPlanets.size()));
        solarSystem.addProperty(IProperty.buildUrl(RESEARCH_LAB, "https://esa.int"));
        solarSystem.addProperty(IProperty.buildDouble(DISTANCE_TO_SOLAR_SYSTEM, 0.0));
        indexerService.saveEntity(getDefaultTenant(), solarSystem);
        indexerService.saveBulkEntities(getDefaultTenant(), solarPlanets);

        // KEPLER 90 SYSTEM
        Dataset kepler90System = createStelarSystem(starSystemModel, KEPLER_90);
        List<DataObject> kepler90Planets = createKepler90SystemPlanets(planetModel, kepler90System.getIpId());
        kepler90System.addProperty(IProperty.buildDate(STUDY_DATE, OffsetDateTime
                .of(LocalDate.of(2019, 1, 1), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC)));
        kepler90System.addProperty(IProperty.buildInteger(NUMBER_OF_PLANETS, kepler90Planets.size()));
        kepler90System.addProperty(IProperty.buildUrl(RESEARCH_LAB, "https://roscosmos.ru"));
        kepler90System.addProperty(IProperty.buildDouble(DISTANCE_TO_SOLAR_SYSTEM, 2.544));
        indexerService.saveEntity(getDefaultTenant(), kepler90System);
        indexerService.saveBulkEntities(getDefaultTenant(), kepler90Planets);

        // KEPLER 16 SYSTEM
        Dataset kepler16System = createStelarSystem(starSystemModel, KEPLER_16);
        List<DataObject> kepler16Planets = createKepler16SystemPlanets(planetModel, kepler16System.getIpId());
        kepler16System.addProperty(IProperty.buildInteger(NUMBER_OF_PLANETS, kepler16Planets.size()));
        kepler16System.addProperty(IProperty.buildUrl(RESEARCH_LAB, "https://cnes.fr"));
        kepler16System.addProperty(IProperty.buildDouble(DISTANCE_TO_SOLAR_SYSTEM, 200.0));
        indexerService.saveEntity(getDefaultTenant(), kepler16System);
        indexerService.saveBulkEntities(getDefaultTenant(), kepler16Planets);

        // PEGASI 51 SYSTEM
        Dataset pegasi51System = createStelarSystem(starSystemModel, PEGASI_51);
        List<DataObject> pegasi51Planets = createPegasi51SystemPlanets(planetModel, pegasi51System.getIpId());
        pegasi51System.addProperty(IProperty.buildInteger(NUMBER_OF_PLANETS, kepler16Planets.size()));
        pegasi51System.addProperty(IProperty.buildUrl(RESEARCH_LAB, "https://cnes.fr"));
        pegasi51System.addProperty(IProperty.buildDouble(DISTANCE_TO_SOLAR_SYSTEM, 50.91));
        indexerService.saveEntity(getDefaultTenant(), pegasi51System);
        indexerService.saveBulkEntities(getDefaultTenant(), pegasi51Planets);
    }

    protected void initPlugins() throws ModuleException {

        GeoTimeExtension geoTime = new GeoTimeExtension();
        geoTime.setActivated(true);
        RegardsExtension regardsExt = new RegardsExtension();
        regardsExt.setActivated(true);
        MediaExtension mediaExt = new MediaExtension();
        mediaExt.setActivated(true);

        List<ParameterConfiguration> paramConfigurations = Lists.newArrayList();
        ParameterConfiguration planetParameter = new ParameterConfiguration();
        planetParameter.setAttributeModelJsonPath("properties.planet");
        planetParameter.setName("planet");
        planetParameter.setOptionsEnabled(true);
        planetParameter.setOptionsCardinality(12);
        paramConfigurations.add(planetParameter);

        ParameterConfiguration startTimeParameter = new ParameterConfiguration();
        startTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.startDate");
        startTimeParameter.setAllias("debut");
        startTimeParameter.setName("start");
        startTimeParameter.setNamespace("time");
        paramConfigurations.add(startTimeParameter);
        ParameterConfiguration endTimeParameter = new ParameterConfiguration();
        endTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.stopDate");
        endTimeParameter.setAllias("fin");
        endTimeParameter.setName("end");
        endTimeParameter.setNamespace("time");
        paramConfigurations.add(endTimeParameter);

        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setAttribution("Plop");
        engineConfiguration.setSearchDescription("desc");
        engineConfiguration.setSearchTitle("search");
        engineConfiguration.setContact("regards@c-s.fr");
        engineConfiguration.setImage("http://plop/image.png");
        engineConfiguration.setEntityLastUpdateDatePropertyPath("TimePeriod.startDate");

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(OpenSearchEngine.TIME_EXTENSION_PARAMETER,
                                        PluginParameterTransformer.toJson(geoTime)),
                     IPluginParam.build(OpenSearchEngine.REGARDS_EXTENSION_PARAMETER,
                                        PluginParameterTransformer.toJson(regardsExt)),
                     IPluginParam.build(OpenSearchEngine.MEDIA_EXTENSION_PARAMETER,
                                        PluginParameterTransformer.toJson(mediaExt)),
                     IPluginParam.build(OpenSearchEngine.PARAMETERS_CONFIGURATION,
                                        PluginParameterTransformer.toJson(paramConfigurations)),
                     IPluginParam.build(OpenSearchEngine.ENGINE_PARAMETERS,
                                        PluginParameterTransformer.toJson(engineConfiguration)));

        PluginConfiguration opensearchConf = PluginConfiguration.build(OpenSearchEngine.class, null, parameters);
        openSearchPluginConf = pluginService.savePluginConfiguration(opensearchConf);
        SearchEngineConfiguration seConfOS = new SearchEngineConfiguration();
        seConfOS.setConfiguration(openSearchPluginConf);
        seConfOS.setLabel("Opensearch conf for all datasets");
        openSearchEngineConf = searchEngineService.createConf(seConfOS);

        SearchEngineConfiguration seConfOSdataset = new SearchEngineConfiguration();
        seConfOSdataset.setConfiguration(openSearchPluginConf);
        seConfOSdataset.setLabel("Opensearch conf for one dataset");
        seConfOSdataset
                .setDatasetUrn("URN:AIP:" + EntityType.DATASET.toString() + ":PROJECT:" + UUID.randomUUID() + ":V1");
        searchEngineService.createConf(seConfOSdataset);
    }

    /**
     * Default implementation
     */
    protected void manageAccessRights() {
        // Bypass access rights
        Mockito.when(projectUserClientMock.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(Boolean.TRUE));
    }

    protected List<Collection> createGalaxies(Model galaxyModel) {
        Collection milkyWay = createEntity(galaxyModel, MILKY_WAY);
        milkyWay.addProperty(IProperty.buildString(GALAXY, MILKY_WAY));
        milkyWay.addProperty(IProperty.buildString(ABSTRACT,
                                                   "The Milky Way is the galaxy that contains our Solar System."));
        return Arrays.asList(milkyWay);
    }

    protected List<Collection> createStars(Model starModel) {
        Collection sun = createEntity(starModel, SUN);
        sun.addProperty(IProperty.buildString(STAR, SUN));
        sun.addProperty(IProperty.buildString(ABSTRACT, "The Sun is the star at the center of the Solar System."));
        return Arrays.asList(sun);
    }

    protected Dataset createStelarSystem(Model starSystemModel, String label) {
        Dataset solarSystem = createEntity(starSystemModel, label);
        solarSystem.addProperty(IProperty.buildString(STAR_SYSTEM, label));
        solarSystem.addTags("REGARDS");
        solarSystem.addTags("CNES");
        solarSystem.addTags("CS-SI");
        solarSystem.addTags(label);
        return solarSystem;
    }

    protected List<DataObject> createSolarSystemPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createMercury(planetModel));
        planets.add(createPlanet(planetModel, "Venus", PLANET_TYPE_TELLURIC, 12104, 108_000_000L,
                                 createParams("near", "sun")));
        planets.add(createPlanet(planetModel, "Earth", PLANET_TYPE_TELLURIC, 12756, 150_000_000L));
        planets.add(createPlanet(planetModel, "Mars", PLANET_TYPE_TELLURIC, 6800, 228_000_000L));
        planets.add(createPlanet(planetModel, JUPITER, PLANET_TYPE_GAS_GIANT, 143_000, 778_000_000L,
                                 createParams(ALPHA_PARAM, "beta", "gamma")));
        planets.add(createPlanet(planetModel, "Saturn", PLANET_TYPE_GAS_GIANT, 120_536, 1_427_000_000L));
        planets.add(createPlanet(planetModel, "Uranus", PLANET_TYPE_ICE_GIANT, 51_800, 2_800_000_000L));
        planets.add(createPlanet(planetModel, "Neptune", PLANET_TYPE_ICE_GIANT, 49_500, 4_489_435_980L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.addTags(dataset.toString()));
        return planets;
    }

    protected List<DataObject> createKepler90SystemPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createPlanet(planetModel, "Kepler 90b", PLANET_TYPE_TELLURIC, 1000, 50_000_000L));
        planets.add(createPlanet(planetModel, "Kepler 90c", PLANET_TYPE_TELLURIC, 30000, 150_000_000L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.addTags(dataset.toString()));
        return planets;
    }

    protected List<DataObject> createKepler16SystemPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createPlanet(planetModel, "Kepler 16b", PLANET_TYPE_GAS_GIANT, 105000, 150_000_000L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.addTags(dataset.toString()));
        return planets;
    }

    protected List<DataObject> createPegasi51SystemPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createPlanet(planetModel, "Pegasi b", PLANET_TYPE_GAS_GIANT, 8000000, 7_000_000L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.addTags(dataset.toString()));
        return planets;
    }

    protected List<DataObject> createTestData(Model model) {
        List<DataObject> datas = new ArrayList<>();

        DataObject data = createEntity(model, "data_one");
        data.addProperty(IProperty.buildString("name_test", "data_one_test"));
        data.setGroups(getAccessGroups());
        data.setCreationDate(OffsetDateTime.now());
        datas.add(data);

        data = createEntity(model, "data_two");
        data.addProperty(IProperty.buildString("name_test", "data_two_test"));
        data.setGroups(getAccessGroups());
        data.setCreationDate(OffsetDateTime.now());
        datas.add(data);

        return datas;
    }

    protected DataObject createMercury(Model planetModel) {
        DataObject planet = createPlanet(planetModel, MERCURY, PLANET_TYPE_TELLURIC, 4878, 58_000_000L);

        planet.setDatasetModelNames(Sets.newHashSet(planetModel.getName()));

        DataFile quicklooksd = new DataFile();
        quicklooksd.setMimeType(MimeType.valueOf("application/jpg"));
        quicklooksd.setUri(URI.create("http://regards/le_quicklook_sd.jpg").toString());
        quicklooksd.setReference(false);
        quicklooksd.setImageWidth(100d);
        quicklooksd.setImageHeight(100d);
        planet.getFiles().put(DataType.QUICKLOOK_SD, quicklooksd);
        DataFile quicklookmd = new DataFile();
        quicklookmd.setMimeType(MimeType.valueOf("application/jpg"));
        quicklookmd.setUri(URI.create("http://regards/le_quicklook_md.jpg").toString());
        quicklookmd.setReference(false);
        quicklookmd.setImageWidth(100d);
        quicklookmd.setImageHeight(100d);
        planet.getFiles().put(DataType.QUICKLOOK_MD, quicklookmd);
        DataFile quicklookhd = new DataFile();
        quicklookhd.setMimeType(MimeType.valueOf("application/jpg"));
        quicklookhd.setUri(URI.create("http://regards/le_quicklook_hd.jpg").toString());
        quicklookhd.setReference(false);
        quicklookhd.setImageWidth(100d);
        quicklookhd.setImageHeight(100d);
        planet.getFiles().put(DataType.QUICKLOOK_HD, quicklookhd);

        DataFile thumbnail = new DataFile();
        thumbnail.setMimeType(MimeType.valueOf("application/png"));
        thumbnail.setUri(URI.create("http://regards/thumbnail.png").toString());
        thumbnail.setImageWidth(250d);
        thumbnail.setImageHeight(250d);
        thumbnail.setReference(false);
        planet.getFiles().put(DataType.THUMBNAIL, thumbnail);

        DataFile rawdata = DataFile.build(DataType.RAWDATA, "test.nc", "http://regards/test.nc",
                                          MediaType.APPLICATION_OCTET_STREAM, Boolean.TRUE, Boolean.FALSE);
        rawdata.setFilesize(10L);
        planet.getFiles().put(rawdata.getDataType(), rawdata);

        Polygon geo = IGeometry.polygon(IGeometry.toPolygonCoordinates(IGeometry
                .toLinearRingCoordinates(IGeometry.position(10.0, 10.0), IGeometry.position(10.0, 30.0),
                                         IGeometry.position(30.0, 30.0), IGeometry.position(30.0, 10.0),
                                         IGeometry.position(10.0, 10.0))));
        planet.setGeometry(geo);
        planet.setWgs84(geo);
        planet.addProperty(IProperty.buildObject("TimePeriod", IProperty.buildDate(START_DATE, startDateValue),
                                                 IProperty.buildDate(STOP_DATE, stopDateValue)));

        return planet;
    }

    /**
     * Default implementation : no group on data object
     */
    protected Set<String> getAccessGroups() {
        return null;
    }

    protected DataObject createPlanet(Model planetModel, String name, String type, Integer diameter, Long sunDistance,
            Set<String> params) {
        DataObject planet = createEntity(planetModel, name);
        planet.setGroups(getAccessGroups());
        planet.setCreationDate(OffsetDateTime.now());
        planet.addProperty(IProperty.buildString(PLANET, name));
        planet.addProperty(IProperty.buildString(PLANET_TYPE, type));
        planet.addProperty(IProperty.buildInteger(PLANET_DIAMETER, diameter));
        planet.addProperty(IProperty.buildLong(PLANET_SUN_DISTANCE, sunDistance));
        planet.addProperty(IProperty.buildJson(ORIGINE, buildPlanetOrigine()));
        if ((params != null) && !params.isEmpty()) {
            planet.addProperty(IProperty.buildStringArray(PLANET_PARAMS, params.toArray(new String[params.size()])));
        }
        planet.addProperty(IProperty.buildObject("TimePeriod", IProperty.buildDate(START_DATE, startDateValue),
                                                 IProperty.buildDate(STOP_DATE, stopDateValue)));
        return planet;
    }

    protected JsonObject buildPlanetOrigine() {
        return builder.create()
                .fromJson("{\"name\":\"CNES\",\"link\":\"http://cnes.fr\",\"contacts\":[{\"name\":\"JeanPaul\",\"locations\":[{\"institut\":\"CNES-001\",\"code\":1}]},{\"name\":\"Bernadette\",\"locations\":[{\"institut\":\"CNES-156\",\"code\":156}]}]}",
                          JsonObject.class);
    }

    protected Set<String> createParams(String... params) {
        return new HashSet<>(Arrays.asList(params));
    }

    protected DataObject createPlanet(Model planetModel, String name, String type, Integer diameter, Long localSunDistance) {
        return createPlanet(planetModel, name, type, diameter, localSunDistance, null);
    }

    /**
     * Init an entity and add it to the local astronomical object map with label as key
     */
    @SuppressWarnings("unchecked")
    protected <T> T createEntity(Model model, String label) {
        AbstractEntity<?> entity;
        switch (model.getType()) {
            case COLLECTION:
                entity = new Collection(model, getDefaultTenant(), label, label);
                break;
            case DATA:
                entity = new DataObject(model, getDefaultTenant(), label, label);
                break;
            case DATASET:
                entity = new Dataset(model, getDefaultTenant(), label, label);
                break;
            default:
                throw new UnsupportedOperationException("Unknown entity type " + model.getType());
        }
        if (astroObjects.containsKey(label)) {
            throw new UnsupportedOperationException("Label \"" + label
                    + "\" for astronomical object already exists! Please change it and relaunch test!");
        }
        astroObjects.put(label, entity);
        return (T) entity;
    }

    /**
     * Retrieve an astronomical object by its label
     */
    @SuppressWarnings("unchecked")
    protected <T> T getAstroObject(String label) {
        return (T) astroObjects.get(label);
    }

    /**
     * Enclose string in quotes
     */
    protected String protect(String value) {
        String protect = "\"";
        if (value.startsWith(protect)) {
            return value;
        }
        return String.format("%s%s%s", protect, value, protect);
    }
}
