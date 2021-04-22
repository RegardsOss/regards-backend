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
package fr.cnes.regards.modules.order.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.rest.mock.StorageClientMock;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@DirtiesContext
@TestPropertySource(
        properties = { "regards.tenant=orderdata", "spring.jpa.properties.hibernate.default_schema=orderdata" })
public class OrderDataFileControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    public static final UniformResourceName DS1_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
    }

    @Test
    public void testDownloadFileFailed() {
        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                          customizer().expectStatusNotFound(), "Should return result", 6465465);
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_CMD_020"), @Requirement("REGARDS_DSL_STO_CMD_030"), })
    public void testDownloadFile() throws URISyntaxException, IOException {
        Order order = new Order();
        order.setOwner(getDefaultUserEmail());
        order.setLabel(DateTime.now().toString());
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
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

        ResultActions resultActions = performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
                                                        customizer().expectStatusOk(), "Should return result",
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

        Optional<OrderDataFile> dataFileOpt = dataFileRepository
                .findFirstByChecksumAndIpIdAndOrderId(dataFile1.getChecksum(), DO1_IP_ID, order.getId());
        Assert.assertTrue(dataFileOpt.isPresent());
        Assert.assertEquals(FileState.DOWNLOADED, dataFileOpt.get().getState());
    }

}