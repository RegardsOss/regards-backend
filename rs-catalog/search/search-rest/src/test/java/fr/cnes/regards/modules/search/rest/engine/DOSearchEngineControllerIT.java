/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;

/**
 * Search engine tests
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=dosearch", "spring.jpa.properties.hibernate.default_schema=dosearch" })
@MultitenantTransactional
public class DOSearchEngineControllerIT extends AbstractEngineIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(DOSearchEngineControllerIT.class);

    private static final String ENGINE_TYPE = "legacy";

    private static final String ACCESS_GROUP = "GRANTED";

    @Autowired
    protected IUserClient userClient;

    @Override
    protected void manageAccessRights() {
        Mockito.reset(projectUserClientMock);
        // Do not bypass access rights
        Mockito.when(projectUserClientMock.isAdmin(Mockito.anyString())).thenReturn(ResponseEntity.ok(Boolean.FALSE));

        // Mock user groups
        AccessGroup ag = new AccessGroup(ACCESS_GROUP);
        Collection<EntityModel<AccessGroup>> ags = new ArrayList<>();
        ags.add(new EntityModel<AccessGroup>(ag));

        PagedModel.PageMetadata md = new PagedModel.PageMetadata(0, 0, 0);
        PagedModel<EntityModel<AccessGroup>> pagedResources = new PagedModel<>(ags, md, new ArrayList<>());
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(ResponseEntity.ok(pagedResources));
    }

    @Override
    protected Set<String> getAccessGroups() {
        Set<String> groups = new HashSet<>();
        groups.add(ACCESS_GROUP);
        return groups;
    }

    private ResultActions searchDataobjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        // 9 data from planets & 2 datas from test datas
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(14)));
        return performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                                 customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void dataAccessDenied() {

        String json = payload(searchDataobjects());

        List<Object> thumbnails = JsonPath.read(json, "$..THUMBNAIL");
        Assert.assertEquals(1, thumbnails.size());

        List<Object> rawdata = JsonPath.read(json, "$..RAWDATA");
        Assert.assertTrue(rawdata.isEmpty());
    }

    @Test
    public void dataAccessGranted() throws InterruptedException {

        // Add access to mercury
        DataObject mercury = getAstroObject(MERCURY);
        mercury.getMetadata().addGroup(ACCESS_GROUP, "datasetid", true);
        indexerService.saveEntity(getDefaultTenant(), mercury);

        // Refresh index to be sure data is available for requesting
        indexerService.refresh(getDefaultTenant());

        String json = payload(searchDataobjects());

        List<Object> thumbnails = JsonPath.read(json, "$..THUMBNAIL");
        Assert.assertEquals(1, thumbnails.size());

        List<Object> rawdata = JsonPath.read(json, "$..RAWDATA");
        Assert.assertEquals(1, rawdata.size());
    }

    @Test
    public void searchFullTextDataobjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        // Add full text search
        customizer.addParameter("q", MERCURY);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchFullTextDataobjects2() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(2)));
        // Add full text search
        String value = MERCURY + " OR " + PLANET + ":" + JUPITER;
        customizer.addParameter("q", value);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test(expected = AssertionError.class)
    public void searchFullTextDataobjects3() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(2)));
        // Add full text search
        String value = MERCURY + " " + JUPITER;
        //        String value = MERCURY + " OR " + JUPITER;
        customizer.addParameter("q", value);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

}
