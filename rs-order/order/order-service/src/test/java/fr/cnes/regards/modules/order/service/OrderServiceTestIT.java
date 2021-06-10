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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.test.SearchClientMock;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.order.test.StorageClientMock;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles(value = {"default", "test", "testAmqp"}, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(
        properties = {"spring.jpa.properties.hibernate.default_schema=order_test_it", "regards.amqp.enabled=true"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class OrderServiceTestIT extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderRepository orderRepos;
    @Autowired
    private IOrderDataFileRepository dataFileRepos;
    @Autowired
    private IOrderDataFileService dataFileService;
    @Autowired
    private IBasketRepository basketRepos;
    @Autowired
    private IJobInfoRepository jobInfoRepos;
    @Autowired
    private IAuthenticationResolver authResolver;
    @Autowired
    private IProjectsClient projectsClient;
    @Autowired
    private IRuntimeTenantResolver tenantResolver;
    @Autowired
    private StorageClientMock storageClientMock;

    public void clean() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();
        jobInfoRepos.deleteAll();
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setWaitMode(false);

        clean();
        Mockito.when(authResolver.getRole()).thenAnswer(i -> {
            LOGGER.info("Asking for role");
            return DefaultRole.REGISTERED_USER.toString();
        });
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(project), HttpStatus.OK));
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    @Test
    public void simpleOrder() throws InterruptedException, EntityInvalidException {
        tenantResolver.forceTenant(getDefaultTenant());
        String orderOwner = randomLabel("simpleOrder");
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);
        // Run order.
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com", 240);

        LOGGER.info("Order has been created !!");
        // Wait order ends.
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.DONE) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.DONE, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order is done !!");
    }

    private String randomLabel(String prefix) {
        return prefix + "_" + Long.toHexString(new Random().nextLong());
    }

    @Test
    public void multipleDsOrder() throws InterruptedException, EntityInvalidException {
        tenantResolver.forceTenant(getDefaultTenant());
        String orderOwner = randomLabel("multipleDsOrder");
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);

        BasketDatasetSelection dsSelection2 = new BasketDatasetSelection();
        dsSelection2.setDatasetIpid(SearchClientMock.DS2_IP_ID.toString());
        dsSelection2.setDatasetLabel("DS-2");
        dsSelection2.setObjectsCount(3);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection2.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection2);
        basketRepos.save(basket);
        // Run order.
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com", 240);
        LOGGER.info("Order has been created !!");

        //Wait order in waiting user status
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).isWaitingForUser() && (loop < 30)) {
            Thread.sleep(1_000);
            loop++;
        }
        Assert.assertTrue(orderService.loadComplete(order.getId()).isWaitingForUser());

        LOGGER.info("Order waits uer donwload !!");

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderService.downloadOrderCurrentZip(orderOwner, availableFiles, new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                // Nothing todo
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        // Wait order ends.
        loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.DONE) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.DONE, orderService.loadComplete(order.getId()).getStatus());

        LOGGER.info("Order is done !!");
    }

    @Test
    public void simpleOrderPause() throws InterruptedException, CannotPauseOrderException, CannotResumeOrderException, EntityInvalidException {
        tenantResolver.forceTenant(getDefaultTenant());
        String orderOwner = randomLabel("simpleOrderPause");
        storageClientMock.setWaitMode(true);
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);
        // Run order.
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com", 240);
        LOGGER.info("Order has been created !!");

        // Wait order ends.
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.RUNNING) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Thread.sleep(1_500);
        orderService.pause(order.getId());
        loop = 0;
        while (!orderService.isPaused(order.getId()) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.PAUSED, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order has been paused !!");

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.DONE) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.DONE, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order is done !!");
    }

    @Test
    public void multipleDsOrderPause()
            throws InterruptedException, CannotPauseOrderException, CannotResumeOrderException, EntityInvalidException {
        tenantResolver.forceTenant(getDefaultTenant());
        String orderOwner = randomLabel("multipleDsOrderPause");
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);

        BasketDatasetSelection dsSelection2 = new BasketDatasetSelection();
        dsSelection2.setDatasetIpid(SearchClientMock.DS2_IP_ID.toString());
        dsSelection2.setDatasetLabel("DS-2");
        dsSelection2.setObjectsCount(3);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection2.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection2);
        basketRepos.save(basket);
        // Run order.
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com", 240);
        LOGGER.info("Order has been created !!");

        // Wait order in waiting user status
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).isWaitingForUser() && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertTrue(orderService.loadComplete(order.getId()).isWaitingForUser());

        LOGGER.info("Order waits user download !!");

        // Simulate latence from storage for second suborder
        storageClientMock.setWaitMode(true);

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderService.downloadOrderCurrentZip(orderOwner, availableFiles, new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                // Nothing todo
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        loop = 0;
        while (!orderService.isPaused(order.getId()) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.PAUSED, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order has been paused !!");

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.DONE) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.DONE, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order is done !!");
    }

    @Test
    public void multipleDsOrderPauseAndDelete() throws InterruptedException, CannotPauseOrderException,
            CannotResumeOrderException, CannotDeleteOrderException, CannotRemoveOrderException, EntityInvalidException {
        tenantResolver.forceTenant(getDefaultTenant());
        String orderOwner = randomLabel("multipleDsOrderPauseAndDelete");
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);

        BasketDatasetSelection dsSelection2 = new BasketDatasetSelection();
        dsSelection2.setDatasetIpid(SearchClientMock.DS2_IP_ID.toString());
        dsSelection2.setDatasetLabel("DS-2");
        dsSelection2.setObjectsCount(3);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection2.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection2.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection2.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection2);
        basketRepos.save(basket);
        // Run order.
        Order order = orderService.createOrder(basket, orderOwner,"http://frontend.com", 240);
        LOGGER.info("Order has been created !!");

        // Wait order in waiting user status
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).isWaitingForUser() && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertTrue(orderService.loadComplete(order.getId()).isWaitingForUser());

        LOGGER.info("Order waits for user download !!");

        // Simulate latence from storage for second suborder
        storageClientMock.setWaitMode(true);

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderService.downloadOrderCurrentZip(orderOwner, availableFiles, new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                // Nothing todo
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        loop = 0;
        while (!orderService.isPaused(order.getId()) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.PAUSED, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order has been paused !!");

        orderService.delete(order.getId());

        Assert.assertEquals(OrderStatus.DELETED, orderService.loadComplete(order.getId()).getStatus());

        LOGGER.info("Order has been deleted !!");

        orderService.remove(order.getId());

        Assert.assertNull(orderService.loadComplete(order.getId()));

        LOGGER.info("Order has been removed !!");
    }

    private BasketDatedItemsSelection createDatasetItemSelection(long filesSize, long filesCount, int objectsCount,
                                                                 String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

}
