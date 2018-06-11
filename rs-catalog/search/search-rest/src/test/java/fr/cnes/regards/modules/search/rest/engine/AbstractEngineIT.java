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
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
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

    // Star properties
    protected static final String STAR = "star";

    protected static final String SUN = "Sun";

    // Star system properties
    protected static final String STAR_SYSTEM = "startSystem";

    protected static final String SOLAR_SYSTEM = "Solar system";

    // Planet properties
    protected static final String PLANET = "planet";

    protected static final String PLANET_TYPE = "planet_type";

    protected static final String PLANET_TYPE_GAS_GIANT = "Gas giant";

    protected static final String PLANET_TYPE_ICE_GIANT = "Ice giant";

    protected static final String PLANET_TYPE_TELLURIC = "Telluric";

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
        esRepository.saveBulk(getDefaultTenant(), createStarSystems(starSystemModel));
        esRepository.saveBulk(getDefaultTenant(), createPlanets(planetModel));
    }

    protected List<Collection> createGalaxies(Model galaxyModel) {
        Collection milkyWay = new Collection(galaxyModel, getDefaultTenant(), "Milky way");
        milkyWay.addProperty(AttributeBuilder.buildString(GALAXY, "Milky way"));
        milkyWay.addProperty(AttributeBuilder
                .buildString(ABSTRACT, "The Milky Way is the galaxy that contains our Solar System."));
        return Arrays.asList(milkyWay);
    }

    protected List<Collection> createStars(Model starModel) {
        Collection sun = new Collection(starModel, getDefaultTenant(), SUN);
        sun.addProperty(AttributeBuilder.buildString(STAR, SUN));
        sun.addProperty(AttributeBuilder.buildString(ABSTRACT,
                                                     "The Sun is the star at the center of the Solar System."));
        sun.setGeometry(IGeometry.point(IGeometry.position(50.0, 30.0)));
        return Arrays.asList(sun);
    }

    protected List<Dataset> createStarSystems(Model starSystemModel) {
        Dataset solarSystem = new Dataset(starSystemModel, getDefaultTenant(), SOLAR_SYSTEM);
        solarSystem.addProperty(AttributeBuilder.buildString(STAR_SYSTEM, SOLAR_SYSTEM));
        return Arrays.asList(solarSystem);
    }

    protected List<DataObject> createPlanets(Model planetModel) {

        DataObject mercury = new DataObject(planetModel, getDefaultTenant(), "Mercury");
        mercury.addProperty(AttributeBuilder.buildString(PLANET, "Mercury"));
        mercury.addProperty(AttributeBuilder.buildString(PLANET_TYPE, PLANET_TYPE_TELLURIC));
        mercury.addProperty(AttributeBuilder.buildDate(START_DATE, OffsetDateTime.now()));
        mercury.addProperty(AttributeBuilder.buildDate(STOP_DATE, OffsetDateTime.now().plusMonths(36)));
        DataFile quicklook = new DataFile();
        quicklook.setMimeType(MimeType.valueOf("application/jpg"));
        quicklook.setUri(URI.create("http://regards/le_quicklook.jpg"));
        quicklook.setImageWidth(100);
        quicklook.setImageHeight(100);
        mercury.getFiles().put(DataType.QUICKLOOK_SD, quicklook);

        DataFile thumbnail = new DataFile();
        thumbnail.setMimeType(MimeType.valueOf("application/png"));
        thumbnail.setUri(URI.create("http://regards/thumbnail.png"));
        thumbnail.setImageWidth(250);
        thumbnail.setImageHeight(250);
        mercury.getFiles().put(DataType.THUMBNAIL, thumbnail);

        return Arrays.asList(mercury);
    }
}
