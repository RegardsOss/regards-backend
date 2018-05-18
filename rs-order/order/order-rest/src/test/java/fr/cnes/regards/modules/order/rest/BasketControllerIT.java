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

import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.BadBasketSelectionRequestException;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author oroussel
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@Ignore
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

    @Before
    public void init() {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        basketRepos.deleteAll();

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(Matchers.anyString()))
                .thenReturn(ResponseEntity.ok(new Resource<>(project)));
        Mockito.when(authResolver.getUser()).thenReturn(DEFAULT_USER_EMAIL);
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
    }

    @Test
    public void testAddBadSelection() {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isExpectationFailed());

        ResultActions results = performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                                                   customizer, "error");
    }

    @Test
    public void testAddNullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setIpIds(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));

        Assert.assertEquals("ipId:\"URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1\"",
                            request.computeOpenSearchRequest());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        ResultActions results = performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                                                   customizer, "error");
    }

    @Test
    public void testAddEmptyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setIpIds(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));
        request.setSelectAllOpenSearchRequest("");

        Assert.assertEquals("NOT(ipId:\"URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1\")",
                            request.computeOpenSearchRequest());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        ResultActions results = performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                                                   customizer, "error");
    }

    @Test
    public void testAddFullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setIpIds(Collections.singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));
        request.setSelectAllOpenSearchRequest("MACHIN: BIDULE AND PATATIPATAT: POUET");

        Assert.assertEquals("(MACHIN: BIDULE AND PATATIPATAT: POUET) AND NOT(ipId:\"URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1\")",
                            request.computeOpenSearchRequest());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        ResultActions results = performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                                                   customizer, "error");
    }

    @Test
    public void testAddOnlyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setSelectAllOpenSearchRequest("MACHIN: BIDULE AND PATATIPATAT: POUET");

        Assert.assertEquals("MACHIN: BIDULE AND PATATIPATAT: POUET", request.computeOpenSearchRequest());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        ResultActions results = performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                                                   customizer, "error");
    }
}
