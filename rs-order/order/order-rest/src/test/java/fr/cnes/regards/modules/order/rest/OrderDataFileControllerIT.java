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
package fr.cnes.regards.modules.order.rest;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.rest.mock.StorageClientMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@DirtiesContext
@TestPropertySource(properties = { "regards.tenant=orderdata",
                                   "spring.jpa.properties.hibernate.default_schema=orderdata" })
public class OrderDataFileControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private OrderDataFileController orderDataFileController;

    @MockBean
    private IProjectUsersClient projectUsersClient;

    @MockBean
    private IAuthenticationResolver authResolver;

    public static final UniformResourceName DS1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
    }

    @Test
    public void test_downloadFileFailed() {
        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusNotFound(),
                          "Should return result",
                          6465465);
    }

    @Test
    public void test_accessRights() {
        Order order = new Order();
        order.setOwner(getDefaultUserEmail());
        order.setLabel(DateTime.now().toString());
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        order = orderRepository.save(order);

        // One dataset task
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");

        order.addDatasetOrderTask(ds1Task);

        File testFile = new File("src/test/resources/files/file1.txt");

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(getDefaultUserEmail());
        files1Task.setOrderId(order.getId());
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/file1.txt");
        dataFile1.setFilename(testFile.getName());
        dataFile1.setIpId(DO1_IP_ID);
        dataFile1.setOnline(true);
        dataFile1.setReference(false);
        // Use filename as checksum (same as OrderControllerIT)
        dataFile1.setChecksum(StorageClientMock.TEST_FILE_CHECKSUM);
        dataFile1.setOrderId(order.getId());
        dataFile1.setFilesize(testFile.length());
        dataFile1.setMimeType(StorageClientMock.TEST_MEDIA_TYPE);
        dataFile1.setDataType(DataType.RAWDATA);
        dataFile1.setState(FileState.AVAILABLE);
        dataFile1 = dataFileRepository.save(dataFile1);
        files1Task.addFile(dataFile1);
        ds1Task.addReliantTask(files1Task);

        orderRepository.save(order);

        // test wrong owner and not admin -> failed
        Mockito.when(authResolver.getRole()).thenReturn("NOT_ADMIN");
        Mockito.when(authResolver.getUser()).thenReturn("WRONG_OWNER");

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusForbidden(),
                          "Should return result",
                          dataFile1.getId());

        // test admin and wrong owner -> success
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PROJECT_ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn("NOT_OWNER_OF_DATAFILE");

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusOk(),
                          "Should return result",
                          dataFile1.getId());

        // test instance admin and wrong owner -> success
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.INSTANCE_ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn("NOT_OWNER_OF_DATAFILE");

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusOk(),
                          "Should return result",
                          dataFile1.getId());

        // test not admin user and correct owner -> success

        Mockito.when(authResolver.getRole()).thenReturn("NOT_IMPORTANT");
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusOk(),
                          "Should return result",
                          dataFile1.getId());
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_CMD_020"), @Requirement("REGARDS_DSL_STO_CMD_030"), })
    public void test_downloadFile() throws IOException {
        Order order = new Order();
        order.setOwner(getDefaultUserEmail());
        order.setLabel(DateTime.now().toString());
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        order = orderRepository.save(order);

        // One dataset task
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");

        order.addDatasetOrderTask(ds1Task);

        File testFile = new File("src/test/resources/files/file1.txt");

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(getDefaultUserEmail());
        files1Task.setOrderId(order.getId());
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/file1.txt");
        dataFile1.setFilename(testFile.getName());
        dataFile1.setIpId(DO1_IP_ID);
        dataFile1.setOnline(true);
        dataFile1.setReference(false);
        // Use filename as checksum (same as OrderControllerIT)
        dataFile1.setChecksum(StorageClientMock.TEST_FILE_CHECKSUM);
        dataFile1.setOrderId(order.getId());
        dataFile1.setFilesize(testFile.length());
        dataFile1.setMimeType(StorageClientMock.TEST_MEDIA_TYPE);
        dataFile1.setDataType(DataType.RAWDATA);
        dataFile1.setState(FileState.AVAILABLE);
        dataFile1 = dataFileRepository.save(dataFile1);
        files1Task.addFile(dataFile1);
        ds1Task.addReliantTask(files1Task);

        order = orderRepository.save(order);
        ds1Task = order.getDatasetTasks().first();

        // user admin have access to all orders
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PROJECT_ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn("owner not null");

        ResultActions resultActions = performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        dataFile1.getId());

        assertMediaType(resultActions, StorageClientMock.TEST_MEDIA_TYPE);
        File resultFile = File.createTempFile("ORDER", "");
        resultFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertTrue(Files.equal(testFile, resultFile));

        Optional<OrderDataFile> dataFileOpt = dataFileRepository.findFirstByChecksumAndIpIdAndOrderId(dataFile1.getChecksum(),
                                                                                                      DO1_IP_ID,
                                                                                                      order.getId());
        Assert.assertTrue(dataFileOpt.isPresent());
        Assert.assertEquals(FileState.DOWNLOADED, dataFileOpt.get().getState());
    }

}