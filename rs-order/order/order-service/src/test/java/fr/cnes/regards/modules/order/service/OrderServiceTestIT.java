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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.order.dao.*;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRestartOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import fr.cnes.regards.modules.order.test.OrderTestUtils;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.order.test.StorageClientMock;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=order_test_it", "regards.amqp.enabled=true" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class OrderServiceTestIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);

    private static final String URL = "http://frontend.com";

    private static final UniformResourceName OBJECT_IP_ID = UniformResourceName.fromString(
        "URN:AIP:DATA:ORDER:00000000-0000-0002-0000-000000000002:V1");

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderDownloadService orderDownloadService;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository orderDataFileRepository;

    @Autowired
    private IFilesTasksRepository filesTasksRepository;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IDatasetTaskRepository datasetTaskRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private StorageClientMock storageClientMock;

    @MockBean
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private OrderSettingsService orderSettingsService;

    @Autowired
    private OrderHelperService orderHelperService;

    @Autowired
    private ITenantResolver tenantsResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    public void clean() {
        filesTasksRepository.deleteAll();
        jobInfoRepository.deleteAll();
        orderDataFileRepository.deleteAll();
        datasetTaskRepository.deleteAll();
        orderRepository.deleteAll();
        basketRepository.deleteAll();
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setWaitMode(false);
        clean();
        Mockito.when(authenticationResolver.getRole()).thenAnswer(i -> {
            LOGGER.info("Asking for role");
            return DefaultRole.REGISTERED_USER.toString();
        });

        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(project), HttpStatus.OK));

        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.isAdmin(any())).thenReturn(ResponseEntity.ok(false));
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(projectUser), HttpStatus.OK));
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    @Test
    public void simpleOrder() throws InterruptedException, EntityInvalidException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrder");
        basketRepository.save(basket);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        LOGGER.info("Order has been created !!");

        // Wait order ends.
        waitForStatus(order.getId(), OrderStatus.DONE);
        LOGGER.info("Order is done !!");
    }

    @Test
    public void multipleDsOrder() throws InterruptedException, EntityInvalidException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketDoubleSelection("multipleDsOrder");
        basketRepository.save(basket);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        LOGGER.info("Order has been created !!");

        //Wait order in waiting user status
        waitForWaitingForUser(order.getId());
        LOGGER.info("Order waits uer download !!");

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderDownloadService.downloadOrderCurrentZip(order.getOwner(), availableFiles, new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                // Nothing to do
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        // Wait order ends.
        waitForStatus(order.getId(), OrderStatus.DONE);
        LOGGER.info("Order is done !!");
    }

    @Test
    public void simpleOrderPause() throws InterruptedException, ModuleException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrderPause");
        basketRepository.save(basket);
        storageClientMock.setWaitMode(true);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        LOGGER.info("Order has been created !!");

        // Wait order ends.
        waitForStatus(order.getId(), OrderStatus.RUNNING);

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());
        LOGGER.info("Order has been paused !!");

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        waitForStatus(order.getId(), OrderStatus.DONE);
        LOGGER.info("Order is done !!");
    }

    @Test
    public void multipleDsOrderPause() throws InterruptedException, ModuleException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketDoubleSelection("multipleDsOrderPause");
        basketRepository.save(basket);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        LOGGER.info("Order has been created !!");

        // Wait order in waiting user status
        waitForWaitingForUser(order.getId());
        LOGGER.info("Order waits user download !!");

        // Simulate latency from storage for second suborder
        storageClientMock.setWaitMode(true);

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderDownloadService.downloadOrderCurrentZip(order.getOwner(), availableFiles, new OutputStream() {

            @Override
            public void write(int b) {
                // Nothing to do
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());
        LOGGER.info("Order has been paused !!");

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        waitForStatus(order.getId(), OrderStatus.DONE);
        LOGGER.info("Order is done !!");
    }

    @Test
    public void multipleDsOrderPauseAndDelete() throws InterruptedException, ModuleException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketDoubleSelection("multipleDsOrderPauseAndDelete");
        basketRepository.save(basket);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        LOGGER.info("Order has been created !!");

        // Wait order in waiting user status
        waitForWaitingForUser(order.getId());
        LOGGER.info("Order waits for user download !!");

        // Simulate latency from storage for second suborder
        storageClientMock.setWaitMode(true);

        // Download files to allow next suborder to be run
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(order.getId()));
        orderDownloadService.downloadOrderCurrentZip(order.getOwner(), availableFiles, new OutputStream() {

            @Override
            public void write(int b) {
                // Nothing to do
            }

        });
        Assert.assertFalse(orderService.loadComplete(order.getId()).isWaitingForUser());

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());
        LOGGER.info("Order has been paused !!");

        orderService.delete(order.getId());
        Assert.assertEquals(OrderStatus.DELETED, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order has been deleted !!");

        Mockito.when(projectUsersClient.isAdmin(any())).thenReturn(ResponseEntity.ok(true));
        orderService.remove(order.getId());
        Assert.assertNull(orderService.loadComplete(order.getId()));
        LOGGER.info("Order has been removed !!");
    }

    @Test(expected = CannotPauseOrderException.class)
    public void pauseOrderWhenNotOwner() throws ModuleException, InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("pauseOrderWhenNotOwner");
        basketRepository.save(basket);
        Mockito.when(authenticationResolver.getUser()).thenReturn("notOwner");
        storageClientMock.setWaitMode(true);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.RUNNING);
        Thread.sleep(1_500);

        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());
    }

    @Test
    public void resumeOrderWhenNotOwner() throws ModuleException, InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("resumeOrderWhenNotOwner");
        basketRepository.save(basket);
        storageClientMock.setWaitMode(true);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        waitForStatus(order.getId(), OrderStatus.RUNNING);
        Thread.sleep(1_500);

        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());

        storageClientMock.setWaitMode(false);
        Mockito.when(authenticationResolver.getUser()).thenReturn("notOwner");
        Assertions.assertThrows(CannotResumeOrderException.class, () -> orderService.resume(order.getId()));
    }

    @Test
    public void pauseAndResumeOrderWhenAdminAndNotOwner() throws InterruptedException, ModuleException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("pauseResumeWhenAdmin");
        basketRepository.save(basket);
        Mockito.when(authenticationResolver.getUser()).thenReturn("notOwner");
        Mockito.when(projectUsersClient.isAdmin(any())).thenReturn(ResponseEntity.ok(true));
        storageClientMock.setWaitMode(true);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.RUNNING);
        Thread.sleep(1_500);

        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        waitForStatus(order.getId(), OrderStatus.DONE);
    }

    @Test
    public void basketStoredWithProperOwnerAfterCreation() throws EntityInvalidException, InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("basketOwner");
        basketRepository.save(basket);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);

        String expectedBasketOwner = IOrderService.BASKET_OWNER_PREFIX + order.getId();
        Assert.assertNotNull(basketRepository.findByOwner(expectedBasketOwner));
    }

    @Test
    public void basketDeletedAfterOrderRemoved() throws InterruptedException, ModuleException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("basketOwner");
        basketRepository.save(basket);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);

        String basketOwner = IOrderService.BASKET_OWNER_PREFIX + order.getId();
        Assert.assertNotNull(basketRepository.findByOwner(basketOwner));

        Mockito.when(projectUsersClient.isAdmin(any())).thenReturn(ResponseEntity.ok(true));
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        orderService.delete(order.getId());
        orderService.remove(order.getId());
        Assert.assertNull(orderService.loadComplete(order.getId()));
        LOGGER.info("Order has been removed !!");
        Assert.assertNull(basketRepository.findByOwner(basketOwner));
    }

    @Test
    public void orderRestart() throws ModuleException, InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("orderRestart");
        basketRepository.save(basket);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());

        String basketOwner = IOrderService.BASKET_OWNER_PREFIX + order.getId();
        Assert.assertNotNull(basketRepository.findByOwner(basketOwner));

        Order restartedOrder = orderService.restart(order.getId(), "orderRestart", URL);
        waitForStatus(restartedOrder.getId(), OrderStatus.RUNNING);

        String newBasketOwner = IOrderService.BASKET_OWNER_PREFIX + restartedOrder.getId();
        Basket newBasket = basketRepository.findByOwner(newBasketOwner);
        Assert.assertNotNull(newBasket);
        Assert.assertEquals(order.getOwner(), restartedOrder.getOwner());
    }

    @Test
    public void orderRestartWhenNotDone() throws ModuleException, InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("orderRestart");
        basketRepository.save(basket);
        storageClientMock.setWaitMode(true);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.RUNNING);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());

        Assertions.assertThrows(CannotRestartOrderException.class,
                                () -> orderService.restart(order.getId(), "restartWhenNotDone", URL));

        Thread.sleep(1_500);
        orderService.pause(order.getId());
        waitForPausedStatus(order.getId());

        Assertions.assertThrows(CannotRestartOrderException.class,
                                () -> orderService.restart(order.getId(), "restartWhenNotDone", URL));

        storageClientMock.setWaitMode(false);
        orderService.resume(order.getId());
        waitForStatus(order.getId(), OrderStatus.DONE);
    }

    @Test
    public void test_expirationDate() throws EntityException {
        // expiration is calculated from current date, and we cannot mock current date.
        // that's why we add few seconds of delta to the expiration date
        // expiration is calculated in hour so few second will not affect tests.
        int deltaInSeconds = 5;

        // expiration date 1 hour reached
        orderSettingsService.setExpirationMaxDurationInHours(1);
        OffsetDateTime dateComputed = orderHelperService.computeOrderExpirationDate(5, 5);
        Assertions.assertEquals(dateComputed.toEpochSecond(),
                                OffsetDateTime.now().plusHours(1).toEpochSecond(),
                                deltaInSeconds);

        // expiration date 100 hour reached
        orderSettingsService.setExpirationMaxDurationInHours(100);
        dateComputed = orderHelperService.computeOrderExpirationDate(500, 500);
        Assertions.assertEquals(dateComputed.toEpochSecond(),
                                OffsetDateTime.now().plusHours(100).toEpochSecond(),
                                deltaInSeconds);

        // expiration date 100 hour not reached
        orderSettingsService.setExpirationMaxDurationInHours(100);
        dateComputed = orderHelperService.computeOrderExpirationDate(0, 10);
        Assertions.assertEquals(dateComputed.toEpochSecond(),
                                OffsetDateTime.now().plusHours(10).toEpochSecond(),
                                deltaInSeconds);
    }

    /**
     * Check if two dates are equals with +- 5 sec inaccuracy
     */
    private void assertDateIsApproximatelyEqualsTo(OffsetDateTime date, OffsetDateTime approximatelyEqualsTo) {
        Assertions.assertEquals(date.toEpochSecond(), approximatelyEqualsTo.toEpochSecond(), 5);
    }

    @Test
    public void retry() throws ModuleException, InterruptedException {

        int creationTasksCount = 2;
        int retryTasksCount = 3;
        int expectedPostRetryCompletion = 83;

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleRetry");
        basketRepository.save(basket);

        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        Mockito.when(authenticationResolver.getUser()).thenReturn(order.getOwner());
        Long orderId = order.getId();
        LOGGER.info("Order created");
        waitForStatus(orderId, OrderStatus.DONE);
        assertEquals(creationTasksCount,
                     filesTasksRepository.findAll()
                                         .stream()
                                         .filter(filesTask -> filesTask.getOrderId().equals(orderId))
                                         .count());
        checkCompletion(orderId, 100);

        // Set current FilesTask as downloaded to ensure new job will be processed after retry
        filesTasksRepository.findDistinctByWaitingForUser(true).forEach(filesTask -> {
            filesTask.getFiles().forEach(orderDataFile -> {
                orderDataFile.setState(FileState.DOWNLOADED);
                orderDataFileRepository.save(orderDataFile);
            });
            filesTask.computeWaitingForUser();
            filesTasksRepository.save(filesTask);
        });
        // Set one file in error and order as done with warning
        OrderDataFile ds1OrderDataFile = orderDataFileRepository.findAllByOrderId(orderId)
                                                                .stream()
                                                                .filter(orderDataFile -> orderDataFile.getIpId()
                                                                                                      .equals(
                                                                                                          OBJECT_IP_ID))
                                                                .findFirst()
                                                                .get();
        ds1OrderDataFile.setState(FileState.ERROR);
        orderDataFileRepository.save(ds1OrderDataFile);
        FilesTask oldFilesTask = filesTasksRepository.findDistinctByFilesContaining(ds1OrderDataFile);
        order = orderRepository.findSimpleById(orderId);
        order.setStatus(OrderStatus.DONE_WITH_WARNING);
        orderRepository.save(order);

        // Mock storage delay to check on completion after retry
        storageClientMock.setWaitMode(true);

        // Actually retry order
        orderService.retryErrors(order.getId());
        LOGGER.info("Order retried");
        waitForStatus(orderId, OrderStatus.RUNNING);

        // Wait for completion to be computed through scheduled task - see OrderMaintenanceService
        TimeUnit.SECONDS.sleep(2);
        // Check completion is set properly
        checkCompletion(orderId, expectedPostRetryCompletion);

        // Should get a new FilesTask for retried files
        assertEquals(retryTasksCount,
                     filesTasksRepository.findAll()
                                         .stream()
                                         .filter(filesTask -> filesTask.getOrderId().equals(orderId))
                                         .count());
        FilesTask newFilesTask = filesTasksRepository.findDistinctByFilesContaining(ds1OrderDataFile);
        assertNotEquals(oldFilesTask, newFilesTask);

        // Pause and resume order to force job to actually complete
        orderService.pause(orderId);
        waitForPausedStatus(orderId);
        storageClientMock.setWaitMode(false);
        orderService.resume(orderId);
        waitForStatus(orderId, OrderStatus.DONE);
        checkCompletion(orderId, 100);
    }

    private void waitForStatus(Long orderId, OrderStatus status) throws InterruptedException {
        int loop = 0;
        while (!orderService.loadComplete(orderId).getStatus().equals(status) && (loop < 20)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(status, orderService.loadSimple(orderId).getStatus());
    }

    private void waitForPausedStatus(Long orderId) throws InterruptedException {
        int loop = 0;
        while (!orderService.isPaused(orderId) && (loop < 20)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertTrue(orderService.isPaused(orderId));
    }

    private void waitForWaitingForUser(Long orderId) throws InterruptedException {
        int loop = 0;
        while (!orderService.loadComplete(orderId).isWaitingForUser() && (loop < 30)) {
            Thread.sleep(1_000);
            loop++;
        }
        Assert.assertTrue(orderService.loadComplete(orderId).isWaitingForUser());
    }

    private void checkCompletion(long orderId, int expectedCompletion) {
        assertEquals(expectedCompletion, orderRepository.findCompleteById(orderId).getPercentCompleted());
    }

}
