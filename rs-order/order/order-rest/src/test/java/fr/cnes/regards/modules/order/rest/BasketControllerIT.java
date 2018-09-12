/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.rest;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.BadBasketSelectionRequestException;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = OrderConfiguration.class)
public class BasketControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS3_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO2_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO3_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO4_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO5_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    private BasketSelectionRequest createBasketSelectionRequest(String datasetUrn, String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        request.setDatasetUrn(datasetUrn);
        return request;
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        basketRepos.deleteAll();

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(Matchers.anyString()))
                .thenReturn(ResponseEntity.ok(new Resource<>(project)));
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
    }

    @Test
    public void testAddBadSelection() {
        BasketSelectionRequest request = new BasketSelectionRequest();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());

        try {
            performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer,
                               "error");
        } catch (AssertionError e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testAddNullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer, "error");
    }

    @Test
    public void testAddEmptyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer, "error");
    }

    @Test
    public void testAddFullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));
        request.setDatasetUrn("URN%3AAIP%3ADATASET%3AOlivier%3A4af7fa7f-110e-42c8-b434-7c863c280548%3AV1");

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());
        // Add doc
        ConstrainedFields constrainedFields = new ConstrainedFields(BasketSelectionRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "basket object").optional().type(JSON_OBJECT_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer, "error");
    }

    @Test
    public void testAddOnlyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", "MACHIN: BIDULE AND PATATIPATAT: POUET");
        request.setSearchParameters(parameters);

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer, "error");
    }

    @Test
    public void testGetEmptyBasket() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        performDefaultGet(BasketController.ORDER_BASKET, customizer, "error");
    }

    @Test
    public void testGetBasket() {
        createBasket();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(BasketController.ORDER_BASKET, customizer, "error");
    }

    private Basket createBasket() {
        OffsetDateTime date = OffsetDateTime.now();

        Basket basket = new Basket(getDefaultUserEmail());
        BasketDatasetSelection dsSel = new BasketDatasetSelection();
        dsSel.setDatasetIpid("URN:AIP:DATASET:Olivier:4af7fa7f-110e-42c8-b434-7c863c280548:V1");
        dsSel.setFilesCount(10);
        dsSel.setFilesSize(124452);
        dsSel.setDatasetLabel("DATASET1");
        dsSel.setObjectsCount(5);

        BasketDatedItemsSelection itemSel = new BasketDatedItemsSelection();
        itemSel.setDate(date);
        itemSel.setFilesCount(10);
        itemSel.setFilesSize(124452);
        itemSel.setObjectsCount(5);
        itemSel.setSelectionRequest(createBasketSelectionRequest(null, ""));

        dsSel.addItemsSelection(itemSel);
        basket.addDatasetSelection(dsSel);
        basketRepos.save(basket);

        return basket;
    }

    @Test
    public void testRemoveDatasetSelection() throws UnsupportedEncodingException {
        Basket basket = createBasket();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultDelete(BasketController.ORDER_BASKET + BasketController.DATASET_DATASET_SELECTION_ID, customizer,
                             "error", basket.getDatasetSelections().first().getId());
    }

    @Test
    public void testRemoveDatedItemSelection() throws UnsupportedEncodingException {
        Basket basket = createBasket();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        OffsetDateTime date = basket.getDatasetSelections().first().getItemsSelections().first().getSelectionRequest()
                .getSelectionDate();

        performDefaultDelete(BasketController.ORDER_BASKET
                + BasketController.DATASET_DATASET_SELECTION_ID_ITEMS_SELECTION_DATE, customizer, "error",
                             basket.getDatasetSelections().first().getId(), OffsetDateTimeAdapter.format(date));
    }

    @Test
    public void testEmptyBasket() {
        createBasket();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        performDefaultDelete(BasketController.ORDER_BASKET, customizer, "error");
    }
}
