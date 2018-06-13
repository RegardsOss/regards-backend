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

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.service.IAttributeModelService;
import fr.cnes.regards.modules.models.service.ModelService;
import fr.cnes.regards.modules.search.rest.SearchEngineController;

/**
 * Engine common methods
 * @author Marc Sordi
 */
public class AbstractEngineIT extends AbstractRegardsTransactionalIT {

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

    private static final String START_DATE = "startDate";

    private static final String STOP_DATE = "stopDate";

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
    protected MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    // Keep reference to astronomical object

    protected Map<String, AbstractEntity> astroObjects = new HashedMap<>();

    protected void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
    }

    @Before
    public void prepareData() throws ModuleException {

        // Bypass method access rights
        List<String> relativeUrlPaths = new ArrayList<>();

        relativeUrlPaths.add(SearchEngineController.SEARCH_ALL_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_ALL_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineController.GET_ENTITY_MAPPING);

        relativeUrlPaths.add(SearchEngineController.SEARCH_COLLECTIONS_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_COLLECTIONS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineController.GET_COLLECTION_MAPPING);

        relativeUrlPaths.add(SearchEngineController.SEARCH_DOCUMENTS_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_DOCUMENTS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineController.GET_DOCUMENT_MAPPING);

        relativeUrlPaths.add(SearchEngineController.SEARCH_DATASETS_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_DATASETS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineController.GET_DATASET_MAPPING);
        relativeUrlPaths.add(SearchEngineController.GET_DATASET_DESCRIPTION_MAPPING);

        relativeUrlPaths.add(SearchEngineController.SEARCH_DATAOBJECTS_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_DATAOBJECTS_MAPPING_EXTRA);
        relativeUrlPaths.add(SearchEngineController.GET_DATAOBJECT_MAPPING);

        relativeUrlPaths.add(SearchEngineController.SEARCH_DATASET_DATAOBJECTS_MAPPING);
        relativeUrlPaths.add(SearchEngineController.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA);

        relativeUrlPaths.add(SearchEngineController.SEARCH_DATAOBJECTS_DATASETS_MAPPING);

        for (String relativeUrlPath : relativeUrlPaths) {
            setAuthorities(SearchEngineController.TYPE_MAPPING + relativeUrlPath, RequestMethod.GET, getDefaultRole());
        }

        // Bypass access rights
        Mockito.when(projectUserClientMock.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(Boolean.TRUE));

        initIndex(getDefaultTenant());

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
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);

        // - Manage attribute cache
        List<Resource<AttributeModel>> resAtts = new ArrayList<>();
        atts.forEach(att -> resAtts.add(new Resource<AttributeModel>(att)));
        Mockito.when(attributeModelClientMock.getAttributes(null, null)).thenReturn(ResponseEntity.ok(resAtts));

        // Create data
        esRepository.saveBulk(getDefaultTenant(), createGalaxies(galaxyModel));
        esRepository.saveBulk(getDefaultTenant(), createStars(starModel));

        Dataset solarSystem = createStelarSystem(starSystemModel, SOLAR_SYSTEM);
        esRepository.save(getDefaultTenant(), solarSystem);
        esRepository.saveBulk(getDefaultTenant(), createPlanets(planetModel, solarSystem.getIpId()));

        Dataset kepler90System = createStelarSystem(starSystemModel, KEPLER_90);
        esRepository.save(getDefaultTenant(), kepler90System);
        DataObject kepler90b = createPlanet(planetModel, "Kepler 90b", PLANET_TYPE_TELLURIC, 1000, 50_000_000L);
        esRepository.save(getDefaultTenant(), kepler90b);
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
        return solarSystem;
    }

    protected List<DataObject> createPlanets(Model planetModel, UniformResourceName dataset) {
        // Create planets
        List<DataObject> planets = new ArrayList<>();
        planets.add(createMercury(planetModel));
        planets.add(createPlanet(planetModel, "Venus", PLANET_TYPE_TELLURIC, 12104, 108_000_000L));
        planets.add(createPlanet(planetModel, "Earth", PLANET_TYPE_TELLURIC, 12756, 150_000_000L));
        planets.add(createPlanet(planetModel, "Mars", PLANET_TYPE_TELLURIC, 6800, 228_000_000L));
        planets.add(createPlanet(planetModel, "Jupiter", PLANET_TYPE_GAS_GIANT, 143_000, 778_000_000L));
        planets.add(createPlanet(planetModel, "Saturn", PLANET_TYPE_GAS_GIANT, 120_536, 1_427_000_000L));
        planets.add(createPlanet(planetModel, "Uranus", PLANET_TYPE_ICE_GIANT, 51_800, 2_800_000_000L));
        planets.add(createPlanet(planetModel, "Neptune", PLANET_TYPE_ICE_GIANT, 49_500, 4_489_435_980L));
        // Attach planets to dataset
        planets.forEach(planet -> planet.getTags().add(dataset.toString()));
        return planets;
    }

    protected DataObject createMercury(Model planetModel) {
        DataObject planet = createPlanet(planetModel, "Mercury", PLANET_TYPE_TELLURIC, 4878, 58_000_000L);

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
        planet.setGeometry(IGeometry.point(IGeometry.position(50.0, 30.0)));
        planet.addProperty(AttributeBuilder
                .buildObject("TimePeriod", AttributeBuilder.buildDate(START_DATE, OffsetDateTime.now()),
                             AttributeBuilder.buildDate(STOP_DATE, OffsetDateTime.now().plusMonths(36))));

        return planet;
    }

    protected DataObject createPlanet(Model planetModel, String name, String type, Integer diameter, Long sunDistance) {
        DataObject planet = createEntity(planetModel, name);
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
        AbstractEntity entity;
        switch (model.getType()) {
            case COLLECTION:
                entity = new Collection(model, getDefaultTenant(), label);
                break;
            case DATA:
                entity = new DataObject(model, getDefaultTenant(), label);
                break;
            case DATASET:
                entity = new Dataset(model, getDefaultTenant(), label);
                break;
            case DOCUMENT:
                entity = new Document(model, getDefaultTenant(), label);
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
