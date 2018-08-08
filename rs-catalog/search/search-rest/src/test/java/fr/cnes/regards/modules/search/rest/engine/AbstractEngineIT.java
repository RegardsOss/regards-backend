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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.collections4.map.HashedMap;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.client.models.IAttributeModelClient;
import fr.cnes.regards.modules.dam.client.models.IModelAttrAssocClient;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.Document;
import fr.cnes.regards.modules.dam.domain.entities.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.dam.domain.models.Model;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeModel;
import fr.cnes.regards.modules.dam.gson.entities.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.dam.service.models.IAttributeModelService;
import fr.cnes.regards.modules.dam.service.models.ModelService;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.service.IIndexerService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.rest.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;

/**
 * Engine common methods
 * @author Marc Sordi
 */
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
    protected static final String STAR_SYSTEM = "startSystem";

    protected static final String SOLAR_SYSTEM = "Solar system";

    protected static final String KEPLER_90 = "Kepler 90 planetary system";

    // Planet properties
    protected static final String PLANET = "planet";

    protected static final String PLANET_TYPE = "planet_type";

    protected static final String PLANET_TYPE_GAS_GIANT = "Gas giant";

    protected static final String PLANET_TYPE_ICE_GIANT = "Ice giant";

    protected static final String PLANET_TYPE_TELLURIC = "Telluric";

    protected static final String PLANET_DIAMETER = "diameter";

    protected static final String PLANET_SUN_DISTANCE = "sun_distance";

    protected static final String START_DATE = "startDate";

    protected static final String STOP_DATE = "stopDate";

    protected static final String MERCURY = "Mercury";

    protected static final String JUPITER = "Jupiter";

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
    protected IProjectsClient projectsClientMock;

    @Autowired
    protected IModelAttrAssocClient modelAttrAssocClientMock;

    @Autowired
    protected MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    @Autowired
    protected IIndexerService indexerService;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected ISearchEngineConfigurationService searchEngineService;

    // Keep reference to astronomical object

    protected Map<String, AbstractEntity<?>> astroObjects = new HashedMap<>();

    protected SearchEngineConfiguration openSearchEngineConf;

    protected PluginConfiguration openSearchPluginConf;

    protected void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
        String[] types = Arrays.stream(EntityType.values()).map(EntityType::toString)
                .toArray(length -> new String[length]);
        esRepository.setGeometryMapping(index, types);
    }

    @Before
    public void prepareData() throws ModuleException, InterruptedException {

        // Needed for test on date in opensearch descriptors. Date are generated in test and compare with date generated
        // by elasticsearch on test server.
        // Test server is in UTC timezone, so to do comparasion we have to be in the same timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        astroObjects.clear();

        // Manage project
        Project project = new Project(1L, "Solar system project", "http://plop/icon.png", true, "SolarSystem");
        project.setHost("http://regards/solarsystem");
        ResponseEntity<Resource<Project>> response = ResponseEntity.ok(new Resource<>(project));
        Mockito.when(projectsClientMock.retrieveProject(Mockito.anyString())).thenReturn(response);

        // Bypass method access rights
        List<String> relativeUrlPaths = new ArrayList<>();

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_ALL_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_ALL_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_ENTITY_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_COLLECTION_MAPPING);

        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DOCUMENTS_MAPPING);
        relativeUrlPaths.add(SearchEngineMappings.SEARCH_DOCUMENTS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineMappings.GET_DOCUMENT_MAPPING);

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

        // - Manage attribute model retrieval
        Mockito.when(modelAttrAssocClientMock.getModelAttrAssocsFor(Mockito.any())).thenAnswer(invocation -> {
            EntityType type = invocation.getArgumentAt(0, EntityType.class);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(type));
        });

        // - Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);

        // - Manage attribute cache
        List<Resource<AttributeModel>> resAtts = new ArrayList<>();
        atts.forEach(att -> resAtts.add(new Resource<AttributeModel>(att)));
        Mockito.when(attributeModelClientMock.getAttributes(null, null)).thenReturn(ResponseEntity.ok(resAtts));

        // Create data
        indexerService.saveBulkEntities(getDefaultTenant(), createGalaxies(galaxyModel));
        indexerService.saveBulkEntities(getDefaultTenant(), createStars(starModel));

        Dataset solarSystem = createStelarSystem(starSystemModel, SOLAR_SYSTEM);
        indexerService.saveEntity(getDefaultTenant(), solarSystem);
        indexerService.saveBulkEntities(getDefaultTenant(), createPlanets(planetModel, solarSystem.getIpId()));

        Dataset kepler90System = createStelarSystem(starSystemModel, KEPLER_90);
        indexerService.saveEntity(getDefaultTenant(), kepler90System);
        DataObject kepler90b = createPlanet(planetModel, "Kepler 90b", PLANET_TYPE_TELLURIC, 1000, 50_000_000L);
        indexerService.saveEntity(getDefaultTenant(), kepler90b);

        // Refresh index to be sure data is available for requesting
        indexerService.refresh(getDefaultTenant());

        initPlugins();
    }

    private void initPlugins() throws ModuleException {
        pluginService.addPluginPackage("fr.cnes.regards.modules.search.rest.engine.plugin.legacy");
        PluginConfiguration legacyConf = PluginUtils
                .getPluginConfiguration(PluginParametersFactory.build().getParameters(), LegacySearchEngine.class,
                                        Lists.newArrayList());
        legacyConf = pluginService.savePluginConfiguration(legacyConf);
        SearchEngineConfiguration seConf = new SearchEngineConfiguration();
        seConf.setLabel("Legacy conf for all datasets");
        seConf.setConfiguration(legacyConf);
        searchEngineService.createConf(seConf);

        pluginService.addPluginPackage("fr.cnes.regards.modules.search.rest.engine.plugin.opensearch");
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
        planetParameter.setOptionsCardinality(10);
        paramConfigurations.add(planetParameter);

        ParameterConfiguration startTimeParameter = new ParameterConfiguration();
        startTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.startDate");
        startTimeParameter.setName("start");
        startTimeParameter.setNamespace("time");
        paramConfigurations.add(startTimeParameter);
        ParameterConfiguration endTimeParameter = new ParameterConfiguration();
        endTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.stopDate");
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

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(OpenSearchEngine.TIME_EXTENSION_PARAMETER, geoTime)
                .addParameter(OpenSearchEngine.REGARDS_EXTENSION_PARAMETER, regardsExt)
                .addParameter(OpenSearchEngine.MEDIA_EXTENSION_PARAMETER, mediaExt)
                .addParameter(OpenSearchEngine.PARAMETERS_CONFIGURATION, paramConfigurations)
                .addParameter(OpenSearchEngine.ENGINE_PARAMETERS, engineConfiguration).getParameters();
        PluginConfiguration opensearchConf = PluginUtils.getPluginConfiguration(parameters, OpenSearchEngine.class,
                                                                                Lists.newArrayList());
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
        milkyWay.addProperty(AttributeBuilder.buildString(GALAXY, MILKY_WAY));
        milkyWay.addProperty(AttributeBuilder
                .buildString(ABSTRACT, "The Milky Way is the galaxy that contains our Solar System."));
        return Arrays.asList(milkyWay);
    }

    protected List<Collection> createStars(Model starModel) {
        Collection sun = createEntity(starModel, SUN);
        sun.addProperty(AttributeBuilder.buildString(STAR, SUN));
        sun.addProperty(AttributeBuilder.buildString(ABSTRACT,
                                                     "The Sun is the star at the center of the Solar System."));
        return Arrays.asList(sun);
    }

    protected Dataset createStelarSystem(Model starSystemModel, String label) {
        Dataset solarSystem = createEntity(starSystemModel, label);
        solarSystem.addProperty(AttributeBuilder.buildString(STAR_SYSTEM, label));
        solarSystem.addTags("REGARDS");
        solarSystem.addTags("CNES");
        solarSystem.addTags("CS-SI");
        return solarSystem;
    }

    protected List<DataObject> createPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createMercury(planetModel));
        planets.add(createPlanet(planetModel, "Venus", PLANET_TYPE_TELLURIC, 12104, 108_000_000L));
        planets.add(createPlanet(planetModel, "Earth", PLANET_TYPE_TELLURIC, 12756, 150_000_000L));
        planets.add(createPlanet(planetModel, "Mars", PLANET_TYPE_TELLURIC, 6800, 228_000_000L));
        planets.add(createPlanet(planetModel, JUPITER, PLANET_TYPE_GAS_GIANT, 143_000, 778_000_000L));
        planets.add(createPlanet(planetModel, "Saturn", PLANET_TYPE_GAS_GIANT, 120_536, 1_427_000_000L));
        planets.add(createPlanet(planetModel, "Uranus", PLANET_TYPE_ICE_GIANT, 51_800, 2_800_000_000L));
        planets.add(createPlanet(planetModel, "Neptune", PLANET_TYPE_ICE_GIANT, 49_500, 4_489_435_980L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.addTags(dataset.toString()));
        return planets;
    }

    protected DataObject createMercury(Model planetModel) {
        DataObject planet = createPlanet(planetModel, MERCURY, PLANET_TYPE_TELLURIC, 4878, 58_000_000L);

        planet.setDatasetModelIds(Sets.newHashSet(99L));

        DataFile quicklook = new DataFile();
        quicklook.setMimeType(MimeType.valueOf("application/jpg"));
        quicklook.setUri(URI.create("http://regards/le_quicklook.jpg"));
        quicklook.setImageWidth(100);
        quicklook.setImageHeight(100);
        planet.getFiles().put(DataType.QUICKLOOK_SD, quicklook);

        DataFile thumbnail = new DataFile();
        thumbnail.setMimeType(MimeType.valueOf("application/png"));
        thumbnail.setUri(URI.create("http://regards/thumbnail.png"));
        thumbnail.setImageWidth(250);
        thumbnail.setImageHeight(250);
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
        planet.addProperty(AttributeBuilder
                .buildObject("TimePeriod", AttributeBuilder.buildDate(START_DATE, OffsetDateTime.now()),
                             AttributeBuilder.buildDate(STOP_DATE, OffsetDateTime.now().plusMonths(36))));

        return planet;
    }

    /**
     * Default implementation : no group on data object
     */
    protected Set<String> getAccessGroups() {
        return null;
    }

    protected DataObject createPlanet(Model planetModel, String name, String type, Integer diameter, Long sunDistance) {
        DataObject planet = createEntity(planetModel, name);
        planet.setGroups(getAccessGroups());
        planet.setCreationDate(OffsetDateTime.now());
        planet.addProperty(AttributeBuilder.buildString(PLANET, name));
        planet.addProperty(AttributeBuilder.buildString(PLANET_TYPE, type));
        planet.addProperty(AttributeBuilder.buildInteger(PLANET_DIAMETER, diameter));
        planet.addProperty(AttributeBuilder.buildLong(PLANET_SUN_DISTANCE, sunDistance));
        return planet;
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
            case DOCUMENT:
                entity = new Document(model, getDefaultTenant(), label, label);
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
}
