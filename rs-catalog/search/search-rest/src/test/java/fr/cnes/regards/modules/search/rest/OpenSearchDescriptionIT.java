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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@ContextConfiguration(classes = CatalogITConfiguration.class)
@TestPropertySource(locations = { "classpath:test.properties" })
public class OpenSearchDescriptionIT extends AbstractRegardsIT {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchDescriptionIT.class);

    @Autowired
    private IModelAttrAssocClient modelAttrAssocClient;

    @Autowired
    private IProjectsClient projectClient;

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testSearchDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modCol = Model.build("modCol", "pDescription", EntityType.COLLECTION);
        Model modDs = Model.build("modDs", "pDescription", EntityType.DATASET);
        Model modData = Model.build("modData", "pDescription", EntityType.DATA);
        Model modDoc = Model.build("modDoc", "pDescription", EntityType.DOCUMENT);
        AttributeModel attrCol = AttributeModelBuilder.build("attrCol", AttributeType.BOOLEAN, "attrCol")
                .fragment(Fragment.buildDefault()).get();
        AttributeModel attrDs = AttributeModelBuilder.build("attrDs", AttributeType.BOOLEAN, "attrDs")
                .fragment(Fragment.buildDefault()).get();
        AttributeModel attrData = AttributeModelBuilder.build("attrData", AttributeType.BOOLEAN, "attrData")
                .fragment(Fragment.buildDefault()).get();
        AttributeModel attrDoc = AttributeModelBuilder.build("attrDoc", AttributeType.BOOLEAN, "attrDoc")
                .fragment(Fragment.buildDefault()).get();
        ModelAttrAssoc assocCol = new ModelAttrAssoc(attrCol, modCol);
        ModelAttrAssoc assocData = new ModelAttrAssoc(attrData, modData);
        ModelAttrAssoc assocDs = new ModelAttrAssoc(attrDs, modDs);
        ModelAttrAssoc assocDoc = new ModelAttrAssoc(attrDoc, modDoc);
        assocs.add(assocCol);
        assocs.add(assocData);
        assocs.add(assocDs);
        assocs.add(assocDoc);
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(null))
                .thenReturn(new ResponseEntity<java.util.Collection<ModelAttrAssoc>>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.DESCRIPTOR,
                          requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testCollectionDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modCol = Model.build("modCol", "pDescription", EntityType.COLLECTION);
        int i = 0;
        for (AttributeType type : AttributeType.values()) {
            AttributeModel attr = AttributeModelBuilder.build("attr" + i, type, "attr" + i)
                    .fragment(Fragment.buildDefault()).get();
            ModelAttrAssoc assoc = new ModelAttrAssoc(attr, modCol);
            assocs.add(assoc);
            i++;
        }
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(EntityType.COLLECTION))
                .thenReturn(new ResponseEntity<java.util.Collection<ModelAttrAssoc>>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.COLLECTIONS_SEARCH + SearchController.DESCRIPTOR,
                          requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testDatasetDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modDs = Model.build("modDs", "pDescription", EntityType.DATASET);
        AttributeModel attrDs = AttributeModelBuilder.build("attrDs", AttributeType.BOOLEAN, "attrDs")
                .fragment(Fragment.buildDefault()).get();
        ModelAttrAssoc assocDs = new ModelAttrAssoc(attrDs, modDs);
        assocs.add(assocDs);
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(EntityType.DATASET))
                .thenReturn(new ResponseEntity<java.util.Collection<ModelAttrAssoc>>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.DATASETS_SEARCH + SearchController.DESCRIPTOR,
                          requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testDataDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modData = Model.build("modData", "pDescription", EntityType.DATA);
        AttributeModel attrData = AttributeModelBuilder.build("attrData", AttributeType.BOOLEAN, "attrData")
                .fragment(Fragment.buildDefault()).get();
        ModelAttrAssoc assocData = new ModelAttrAssoc(attrData, modData);
        assocs.add(assocData);
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(EntityType.DATA))
                .thenReturn(new ResponseEntity<>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.DATAOBJECTS_SEARCH_WITH_FACETS + SearchController.DESCRIPTOR,
                          requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testDataDatasetDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modData = Model.build("modData", "pDescription", EntityType.DATA);
        AttributeModel attrData = AttributeModelBuilder.build("attrData", AttributeType.BOOLEAN, "attrData")
                .fragment(Fragment.buildDefault()).get();
        ModelAttrAssoc assocData = new ModelAttrAssoc(attrData, modData);
        assocs.add(assocData);
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(EntityType.DATA))
                .thenReturn(new ResponseEntity<java.util.Collection<ModelAttrAssoc>>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.DATAOBJECTS_DATASETS_SEARCH
                + SearchController.DESCRIPTOR, requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Test
    @Purpose("System should be able to generate OpenSearch Descriptor")
    @Requirement("REGARDS_DSL_DAM_ARC_800")
    public void testDocumentDescriptor() {
        java.util.Collection<ModelAttrAssoc> assocs = new ArrayList<>();
        Model modDoc = Model.build("modDoc", "pDescription", EntityType.DOCUMENT);
        AttributeModel attrDoc = AttributeModelBuilder.build("attrDoc", AttributeType.BOOLEAN, "attrDoc")
                .fragment(Fragment.buildDefault()).get();
        ModelAttrAssoc assocDoc = new ModelAttrAssoc(attrDoc, modDoc);
        assocs.add(assocDoc);
        Mockito.when(modelAttrAssocClient.getModelAttrAssocsFor(EntityType.DOCUMENT))
                .thenReturn(new ResponseEntity<java.util.Collection<ModelAttrAssoc>>(assocs, HttpStatus.OK));

        Project project = new Project("pDesc", "pIcon", true, DEFAULT_TENANT);
        project.setHost("http://test.test:120/");
        Mockito.when(projectClient.retrieveProject(DEFAULT_TENANT))
                .thenReturn(new ResponseEntity<Resource<Project>>(new Resource<Project>(project), HttpStatus.OK));

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeadersToApply());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_XML));

        performDefaultGet(SearchController.PATH + SearchController.DOCUMENTS_SEARCH + SearchController.DESCRIPTOR,
                          requestBuilderCustomizer, "Couldn't build a proper descriptor for global searches");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }


    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT, Lists.newArrayList(MediaType.APPLICATION_XML_VALUE));

        return headers;
    }

}
