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

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.dto.FileSelectionDescriptionDTO;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRestartOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.domain.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import fr.cnes.regards.modules.order.service.commons.AbstractOrderServiceIT;
import fr.cnes.regards.modules.order.test.OrderTestUtils;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.order.test.SearchClientMock.DS1_IP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=order_test_it",
                                   "regards.amqp.enabled=true",
                                   "regards.order.max.storage.files.jobs.per.user=2" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class OrderServiceTestIT extends AbstractOrderServiceIT {

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

    @Test
    public void test_dataFilesFiltersRAWDATA() throws InterruptedException, ModuleException, CatalogSearchException {
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrderPause");
        basketRepository.save(basket);
        Mockito.when(authenticationResolver.getUser()).thenReturn(basket.getOwner());

        // configure basket selection
        Long id = basket.getDatasetSelections().first().getId();
        basketService.addSelection(basket.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));
        basket = basketService.load(basket.getId());
        basketService.attachFileFilters(basket,
                                        id,
                                        new FileSelectionDescriptionDTO(Set.of(DataTypeLight.RAWDATA), null));

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);

        // check dataFiles
        List<OrderDataFile> fileAvailables = dataFileService.findAllAvailables(order.getId());
        Assertions.assertTrue(fileAvailables.stream()
                                            .map(OrderDataFile::getDataType)
                                            .allMatch(DataType.RAWDATA::equals));
    }

    @Test
    public void test_dataFilesNoFilter() throws InterruptedException, ModuleException, CatalogSearchException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());

        // create basket with empty file filters
        Basket basketEmptyFilter = OrderTestUtils.getBasketSingleSelection("basket_empty_filter");
        basketRepository.save(basketEmptyFilter);
        Mockito.when(authenticationResolver.getUser()).thenReturn(basketEmptyFilter.getOwner());
        basketService.addSelection(basketEmptyFilter.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));
        basketEmptyFilter = basketService.load(basketEmptyFilter.getId());
        Long datasetId = basketEmptyFilter.getDatasetSelections().first().getId();
        basketService.attachFileFilters(basketEmptyFilter, datasetId, new FileSelectionDescriptionDTO(null, null));

        // create basket without indicate file filters
        Basket basketDefault = OrderTestUtils.getBasketSingleSelection("basket_without_filter");
        basketRepository.save(basketDefault);
        Mockito.when(authenticationResolver.getUser()).thenReturn(basketDefault.getOwner());
        basketService.addSelection(basketDefault.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));
        basketDefault = basketService.load(basketDefault.getId());

        // WHEN
        // Run order with file selection description empty.
        Order orderWithEmptyFilters = orderService.createOrder(basketEmptyFilter,
                                                               basketEmptyFilter.getOwner(),
                                                               URL,
                                                               240);
        Order orderDefault = orderService.createOrder(basketDefault, basketDefault.getOwner(), URL, 240);
        waitForStatus(orderWithEmptyFilters.getId(), OrderStatus.DONE);
        waitForStatus(orderDefault.getId(), OrderStatus.DONE);

        // THEN
        // results are the same
        List<OrderDataFile> fileAvailablesEmptyFilters = dataFileService.findAllAvailables(orderWithEmptyFilters.getId());
        List<OrderDataFile> fileAvailablesDefault = dataFileService.findAllAvailables(orderDefault.getId());
        Assertions.assertEquals(fileAvailablesDefault.size(), fileAvailablesEmptyFilters.size());
        Assertions.assertTrue(fileAvailablesDefault.containsAll(fileAvailablesEmptyFilters));
        List<String> strings = fileAvailablesEmptyFilters.stream().map(OrderDataFile::getFilename).toList();
        Assertions.assertEquals(12, fileAvailablesDefault.size());
    }

    @Test
    public void test_dataFilesNameFilter() throws InterruptedException, ModuleException, CatalogSearchException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrderPause");
        basketRepository.save(basket);
        Mockito.when(authenticationResolver.getUser()).thenReturn(basket.getOwner());
        basketService.addSelection(basket.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));
        basket = basketService.load(basket.getId());
        Long datasetId = basket.getDatasetSelections().first().getId();
        // get only file that ends with .bin
        basketService.attachFileFilters(basket, datasetId, new FileSelectionDescriptionDTO(null, ".*\\.bin$"));

        // WHEN
        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);

        // THEN
        // check dataFiles
        List<OrderDataFile> fileAvailables = dataFileService.findAllAvailables(order.getId());
        Assertions.assertTrue(fileAvailables.stream()
                                            .map(OrderDataFile::getFilename)
                                            .allMatch(filename -> filename.endsWith(".bin")));
    }

    @Test
    public void test_dataFilesFiltersWithNoResults()
        throws InterruptedException, ModuleException, CatalogSearchException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrderPause");
        basketRepository.save(basket);
        Mockito.when(authenticationResolver.getUser()).thenReturn(basket.getOwner());
        basketService.addSelection(basket.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));
        basket = basketService.load(basket.getId());
        Long datasetId = basket.getDatasetSelections().first().getId();
        // get only file that ends with .bin
        basketService.attachFileFilters(basket, datasetId, new FileSelectionDescriptionDTO(null, "RegexWithNoResult"));

        // WHEN
        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE_WITH_WARNING);

        // THEN
        // check dataFiles
        List<OrderDataFile> fileAvailables = dataFileService.findAllAvailables(order.getId());
        Assertions.assertEquals(0, fileAvailables.size());
        order = orderService.getOrder(order.getId());
        Assertions.assertNotNull(order.getMessage());
    }

    private BasketSelectionRequest createBasketSelectionRequest(String datasetUrn, String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        request.setDatasetUrn(datasetUrn);
        return request;
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

    private void checkCompletion(long orderId, int expectedCompletion) {
        assertEquals(expectedCompletion, orderRepository.findCompleteById(orderId).getPercentCompleted());
    }

}
