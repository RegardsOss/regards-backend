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
package fr.cnes.regards.modules.backendforfrontend.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.Model;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Declare static variables for tests.
 *
 * @author Xavier-Alexandre Brochard
 */
public class BackendForFrontendTestUtils {

    /**
     * Default tenant configured in application properties
     */
    public static final String DEFAULT_TENANT = "PROJECT";

    public static final String OPENSEARCH_QUERY = "some:opensearchrequest";

    public static final Model DATASET_MODEL;

    public static final Model DATAOBJECT_MODEL;

    public static final Model COLLECTION_MODEL;

    public static final Model DOCUMENT_MODEL;

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET_0;

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET_1;

    /**
     * A dummy dataobject tagging DATASET_0
     */
    public static final DataObject DATAOBJECT;

    /**
     * A dummy collection tagging DATASET_1
     */
    public static final Collection COLLECTION;

    /**
     * A dummy document tagging DATASET_0 and DATASET_1
     */
    public static final DataObject DOCUMENT;

    /**
     * The result a call to IJsonSearchClient#searchAll
     */
    public static final ResponseEntity<JsonObject> SEARCH_ALL_RESULT;

    /**
     * The result a call to IJsonSearchClient#searchCollections
     */
    public static final ResponseEntity<JsonObject> SEARCH_COLLECTIONS_RESULT;

    /**
     * The result a call to IJsonSearchClient#searchDatasets
     */
    public static final ResponseEntity<JsonObject> SEARCH_DATASETS_RESULT;

    /**
     * The result a call to IJsonSearchClient#searchDataobjects
     */
    public static final ResponseEntity<JsonObject> SEARCH_DATAOBJECTS_RESULT;

    /**
     * The result a call to IJsonSearchClient#searchDocuments
     */
    public static final ResponseEntity<JsonObject> SEARCH_DOCUMENTS_RESULT;

    /**
     * A dummy {@link PluginServiceDto} from a {@link PluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_A;

    /**
     * A dummy {@link PluginServiceDto} from a {@link PluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_B;

    /**
     * A dummy {@link PluginServiceDto} from a {@link UIPluginConfiguration}
     */
    public static final PluginServiceDto PLUGIN_SERVICE_DTO_C;

    /**
     * The services applicable to DATASET_0
     */
    public static final ResponseEntity<List<EntityModel<PluginServiceDto>>> SERVICES_FOR_DATASET_0;

    /**
     * The services applicable to DATASET_1
     */
    public static final ResponseEntity<List<EntityModel<PluginServiceDto>>> SERVICES_FOR_DATASET_1;

    // FIXME: should use GsonBuilderFactory with spring context configuration
    private static final Gson gson = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty()).create();

    static {
        DATASET_MODEL = new Model();
        DATASET_MODEL.setType(EntityType.DATASET);
        DATASET_MODEL.setName("datasetModel");
    }

    static {
        DATAOBJECT_MODEL = new Model();
        DATAOBJECT_MODEL.setType(EntityType.DATA);
        DATAOBJECT_MODEL.setName("dataobjectModel");
    }

    static {
        COLLECTION_MODEL = new Model();
        COLLECTION_MODEL.setType(EntityType.COLLECTION);
        COLLECTION_MODEL.setName("colelctiontModel");
    }

    static {
        DOCUMENT_MODEL = new Model();
        DOCUMENT_MODEL.setType(EntityType.DATA);
        DOCUMENT_MODEL.setName("documentModel");
    }

    static {
        DATASET_0 = new Dataset(DATASET_MODEL, DEFAULT_TENANT, "DS0", "dataset0");
    }

    static {
        DATASET_1 = new Dataset(DATASET_MODEL, DEFAULT_TENANT, "DS1", "dataset1");
        DATASET_1.setTags(Sets.newHashSet(DATASET_0.getIpId().toString()));
    }

    static {
        DATAOBJECT = new DataObject(DATAOBJECT_MODEL, DEFAULT_TENANT, "DO1", "dataobject");
        DATAOBJECT.setTags(Sets.newHashSet(DATASET_0.getIpId().toString(), "string_tag"));
    }

    static {
        COLLECTION = new Collection(COLLECTION_MODEL, DEFAULT_TENANT, "COL1", "collection");
        COLLECTION.setTags(Sets.newHashSet(DATASET_1.getIpId().toString()));
    }

    static {
        DOCUMENT = new DataObject(DOCUMENT_MODEL, DEFAULT_TENANT, "DOC1", "document");
        DOCUMENT.setTags(Sets.newHashSet(DATASET_0.getIpId().toString(), DATASET_1.getIpId().toString()));
    }

    static {
        List<AbstractEntity<?>> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATAOBJECT,
                                                              BackendForFrontendTestUtils.COLLECTION);
        PagedModel<EntityModel<AbstractEntity<?>>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_ALL_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    static {
        List<AbstractEntity<?>> entities = Lists.newArrayList(BackendForFrontendTestUtils.COLLECTION);
        PagedModel<EntityModel<AbstractEntity<?>>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_COLLECTIONS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    static {
        List<AbstractEntity<?>> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATASET_0,
                                                              BackendForFrontendTestUtils.DATASET_1);
        PagedModel<EntityModel<AbstractEntity<?>>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_DATASETS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    static {
        List<AbstractEntity<?>> entities = Lists.newArrayList(BackendForFrontendTestUtils.DATAOBJECT);
        PagedModel<EntityModel<AbstractEntity<?>>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_DATAOBJECTS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    static {
        List<AbstractEntity<?>> entities = Lists.newArrayList(BackendForFrontendTestUtils.DOCUMENT);
        PagedModel<EntityModel<AbstractEntity<?>>> asPagedResources = HateoasUtils.wrapToPagedResources(entities);
        JsonObject asJsonObject = (JsonObject) gson.toJsonTree(asPagedResources);
        SEARCH_DOCUMENTS_RESULT = new ResponseEntity<>(asJsonObject, HttpStatus.OK);
    }

    static {
        PluginMetaData metaData = new PluginMetaData();
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        metaData.setPluginId(SampleServicePlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration pluginConfiguration = new PluginConfiguration("conf0", metaData.getPluginId());
        pluginConfiguration.setMetaDataAndPluginId(metaData);
        pluginConfiguration.setId(1L);
        PluginConfigurationDto pluginConfigurationDto = new PluginConfigurationDto(pluginConfiguration);
        PLUGIN_SERVICE_DTO_A = PluginServiceDto.fromPluginConfigurationDto(pluginConfigurationDto);
    }

    static {
        PluginMetaData metaData = new PluginMetaData();
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(SampleServicePlugin.class.getName());
        metaData.setPluginId(SampleServicePlugin.class.getAnnotation(Plugin.class).id());
        PluginConfiguration pluginConfiguration = new PluginConfiguration("conf1", metaData.getPluginId());
        pluginConfiguration.setMetaDataAndPluginId(metaData);
        pluginConfiguration.setId(2L);
        PluginConfigurationDto pluginConfigurationDto = new PluginConfigurationDto(pluginConfiguration);
        PLUGIN_SERVICE_DTO_B = PluginServiceDto.fromPluginConfigurationDto(pluginConfigurationDto);
    }

    static {
        UIPluginConfiguration uiPluginConfiguration = new UIPluginConfiguration();
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        uiPluginConfiguration.setId(3L);
        uiPluginConfiguration.setLabel("uiPluginConfiguration2");
        uiPluginConfiguration.setPluginDefinition(pluginDefinition);
        pluginDefinition.setApplicationModes(Sets.newHashSet(ServiceScope.MANY));
        pluginDefinition.setEntityTypes(Sets.newHashSet(EntityType.COLLECTION));

        PLUGIN_SERVICE_DTO_C = PluginServiceDto.fromUIPluginConfiguration(uiPluginConfiguration);
    }

    static {
        List<PluginServiceDto> asList = Lists.newArrayList(BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_A,
                                                           BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_C);
        List<EntityModel<PluginServiceDto>> asResources = HateoasUtils.wrapList(asList);
        SERVICES_FOR_DATASET_0 = new ResponseEntity<>(asResources, HttpStatus.OK);
    }

    static {
        List<PluginServiceDto> asList = Lists.newArrayList(BackendForFrontendTestUtils.PLUGIN_SERVICE_DTO_B);
        List<EntityModel<PluginServiceDto>> asResources = HateoasUtils.wrapList(asList);
        SERVICES_FOR_DATASET_1 = new ResponseEntity<>(asResources, HttpStatus.OK);
    }

}
