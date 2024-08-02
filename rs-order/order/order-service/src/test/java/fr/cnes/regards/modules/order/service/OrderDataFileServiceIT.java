/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.service.commons.AbstractOrderServiceIT;
import fr.cnes.regards.modules.order.test.OrderTestUtils;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Thomas GUILLOU
 **/

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD,
                hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class OrderDataFileServiceIT extends AbstractOrderServiceIT {

    private static final String URL = "http://frontend.com";

    @Autowired
    private OrderDataFileService orderDataFileService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Create an order and test that the files in the order can be accessed through the OrderDataFileService
     * once they are available.
     */
    @Test
    public void testFindAvailableFilesByOrder() throws EntityInvalidException, InterruptedException {
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleOrderPause");
        basketRepository.save(basket);

        // Run order.
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);
        waitForStatus(order.getId(), OrderStatus.DONE);

        Pageable page = Pageable.ofSize(10);
        Page<OrderDataFileDTO> availableFilesByOrder = orderDataFileService.findAvailableDataFiles(order.getId(),
                                                                                                   null,
                                                                                                   page);
        Assert.assertEquals(10, availableFilesByOrder.getSize());
        Assert.assertEquals(12, availableFilesByOrder.getTotalElements());
        // features creation is mocked : the providerId equals the dir name (@see SearchClientMock.registerFilesIn)
        File testDir = new File("src/test/resources/files");
        List<String> dirNames = Arrays.stream(Objects.requireNonNull(testDir.listFiles())).map(File::getName).toList();
        // check if all productIds of features are stored in orderDataFile
        Assertions.assertTrue(availableFilesByOrder.stream()
                                                   .allMatch(orderDataFileDTO -> dirNames.contains(orderDataFileDTO.getProductId())));
        // check if download url has been computed
        Assertions.assertTrue(availableFilesByOrder.stream()
                                                   .allMatch(orderDataFileDTO -> orderDataFileDTO.getDownloadUrl()
                                                                                                 .contains(
                                                                                                     OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID.replace(
                                                                                                         "{dataFileId}",
                                                                                                         orderDataFileDTO.getId()
                                                                                                                         .toString()))));
    }

    /**
     * Create an order and test that the files in the suborders can be accessed through the OrderDataFileService
     * once they are available.
     */
    @Test
    public void testFindAvailableFilesBySubOrder() throws EntityInvalidException, InterruptedException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        Basket basket = OrderTestUtils.getBasketSingleSelection("simpleSubOrders");
        basketRepository.save(basket);

        // WHEN
        // Run order
        Order order = orderService.createOrder(basket, basket.getOwner(), URL, 240);

        // THEN
        Long orderId = order.getId();
        waitForStatus(orderId, OrderStatus.DONE);
        // get all files tasks (ie suborders) related to this order
        // 2 files task are expected (refer to "${regards.order.files.bucket.size.Mb:100}" to see how suborders are
        // created)
        List<FilesTask> filesTasks = filesTasksRepository.findAll();
        Assert.assertEquals("Unexpected number of suborders created", 2, filesTasks.size());
        Pageable pageFilesTask = PageRequest.of(0, 100, Sort.Direction.ASC, "filesTaskId");
        List<OrderDataFileDTO> availableFilesByOrder = new ArrayList<>(12);
        for (FilesTask filesTask : filesTasks) {
            availableFilesByOrder.addAll(orderDataFileService.findAvailableDataFiles(orderId,
                                                                                     filesTask.getId(),
                                                                                     pageFilesTask).getContent());
        }
        // Check all suborders
        Assert.assertEquals("Unexpected number of available files", 12, availableFilesByOrder.size());
        // features creation is mocked : the providerId equals the dir name (@see SearchClientMock.registerFilesIn)
        File testDir = new File("src/test/resources/files");
        List<String> dirNames = Arrays.stream(Objects.requireNonNull(testDir.listFiles())).map(File::getName).toList();
        // check if all productIds of features are stored in orderDataFile
        Assert.assertTrue(availableFilesByOrder.stream()
                                               .allMatch(orderDataFileDTO -> dirNames.contains(orderDataFileDTO.getProductId())
                                                                             && orderDataFileDTO.getVersion() == 1));
    }

    protected void waitForStatus(Long orderId, OrderStatus status) {
        int loop = 0;
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return orderService.loadComplete(orderId).getStatus().equals(status);
        });
        Assert.assertEquals(status, orderService.loadSimple(orderId).getStatus());
    }
}
