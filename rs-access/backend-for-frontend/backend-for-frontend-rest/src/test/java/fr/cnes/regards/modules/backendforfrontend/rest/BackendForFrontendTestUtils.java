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
package fr.cnes.regards.modules.backendforfrontend.rest;

import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.client.ISearchAllClient;
import fr.cnes.regards.modules.search.client.ISearchCollectionsClient;
import fr.cnes.regards.modules.search.client.ISearchDataobjectsClient;
import fr.cnes.regards.modules.search.client.ISearchDatasetsClient;

/**
 * Declare static variables for tests.
 *
 * @author Xavier-Alexandre Brochard
 */
public class BackendForFrontendTestUtils {

    private static Gson gson = new Gson();

    /**
     * Default tenant configured in application properties
     */
    public static final String DEFAULT_TENANT = "PROJECT";

    public static final String OPENSEARCH_QUERY = "some:opensearchrequest";

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET_0 = new Dataset(null, DEFAULT_TENANT, "dataset0");

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET_1;
    static {
        DATASET_1 = new Dataset(null, DEFAULT_TENANT, "dataset1");
        DATASET_1.setTags(Sets.newHashSet(DATASET_0.getIpId().toString()));
    }

    /**
     * A dummy dataobject tagging DATASET_0
     */
    public static final DataObject DATAOBJECT;
    static {
        DATAOBJECT = new DataObject(null, DEFAULT_TENANT, "dataobject");
        DATAOBJECT.setTags(Sets.newHashSet(DATASET_0.getIpId().toString()));
    }

    /**
     * A dummy collection tagging DATASET_1
     */
    public static final Collection COLLECTION;
    static {
        COLLECTION = new Collection(null, DEFAULT_TENANT, "collection");
        COLLECTION.setTags(Sets.newHashSet(DATASET_1.getIpId().toString()));
    }

    /**
     * The result a call to {@link ISearchAllClient#searchAll(java.util.Map)}
     */
    public static final ResponseEntity<JsonObject> SEARCH_ALL_RESULT;
    static {
        List<AbstractEntity> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATAOBJECT,
                                                           BackendForFrontendTestUtils.COLLECTION);
        PagedResources<Resource<AbstractEntity>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_ALL_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    /**
     * The result a call to {@link ISearchCollectionsClient#searchCollections(java.util.Map)}
     */
    public static final ResponseEntity<JsonObject> SEARCH_COLLECTIONS_RESULT;
    static {
        List<AbstractEntity> entities = Lists.newArrayList(BackendForFrontendTestUtils.COLLECTION);
        PagedResources<Resource<AbstractEntity>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_COLLECTIONS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    /**
     * The result a call to {@link ISearchDatasetsClient#searchDatasets(java.util.Map)}
     */
    public static final ResponseEntity<JsonObject> SEARCH_DATASETS_RESULT;
    static {
        List<AbstractEntity> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATASET_0,
                                                           BackendForFrontendTestUtils.DATASET_1);
        PagedResources<Resource<AbstractEntity>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_DATASETS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    /**
     * The result a call to {@link ISearchDataobjectsClient#searchDataobjects(java.util.Map, String[])}
     */
    public static final ResponseEntity<JsonObject> SEARCH_DATAOBJECTS_RESULT;
    static {
        List<AbstractEntity> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATAOBJECT);
        PagedResources<Resource<AbstractEntity>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_DATAOBJECTS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    /**
     * A dummy {@link PluginServiceDto} from a {@link PluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_A;
    static {
        PluginMetaData metaData = new PluginMetaData();
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        PluginConfiguration pluginConfiguration = new PluginConfiguration(metaData, "conf0");

        PLUGIN_SERVICE_DTO_A = PluginServiceDto.fromPluginConfiguration(pluginConfiguration);
    }

    /**
     * A dummy {@link PluginServiceDto} from a {@link PluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_B;
    static {
        PluginMetaData metaData = new PluginMetaData();
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        PluginConfiguration pluginConfiguration = new PluginConfiguration(metaData, "conf1");

        PLUGIN_SERVICE_DTO_B = PluginServiceDto.fromPluginConfiguration(pluginConfiguration);
    }

    /**
     * A dummy {@link PluginServiceDto} from a {@link UIPluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_C;
    static {
        UIPluginConfiguration uiPluginConfiguration = new UIPluginConfiguration();
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        uiPluginConfiguration.setId(2L);
        uiPluginConfiguration.setLabel("uiPluginConfiguration2");
        uiPluginConfiguration.setPluginDefinition(pluginDefinition);
        pluginDefinition.setApplicationModes(Sets.newHashSet(ServiceScope.MANY));
        pluginDefinition.setEntityTypes(Sets.newHashSet(EntityType.COLLECTION));

        PLUGIN_SERVICE_DTO_C = PluginServiceDto.fromUIPluginConfiguration(uiPluginConfiguration);
    }

    /**
     * The services applicable to DATASET_0
     */
    public static final ResponseEntity<List<Resource<PluginServiceDto>>> SERVICES_FOR_DATASET_0;
    static {
        List<PluginServiceDto> asList = Lists.newArrayList(BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_A,
                                                           BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_C);
        List<Resource<PluginServiceDto>> asResources = HateoasUtils.wrapList(asList);
        SERVICES_FOR_DATASET_0 = new ResponseEntity<>(asResources, HttpStatus.OK);
    }

    /**
     * The services applicable to DATASET_1
     */
    public static final ResponseEntity<List<Resource<PluginServiceDto>>> SERVICES_FOR_DATASET_1;
    static {
        List<PluginServiceDto> asList = Lists.newArrayList(BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_B);
        List<Resource<PluginServiceDto>> asResources = HateoasUtils.wrapList(asList);
        SERVICES_FOR_DATASET_1 = new ResponseEntity<>(asResources, HttpStatus.OK);
    }

}
