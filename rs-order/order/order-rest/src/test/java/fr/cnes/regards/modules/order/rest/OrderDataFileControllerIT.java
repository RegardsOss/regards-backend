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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * @author oroussel
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@DirtiesContext
public class OrderDataFileControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    private static final String USER = "raphael@mechoui.fr";

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
    }

    @Test
    public void testDownloadFileFailed() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNotFound());

        performDefaultGet(OrderDataFileController.ORDERS_FILES_DATA_FILE_ID, customizer, "Should return result",
                          6465465);
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_CMD_020"), @Requirement("REGARDS_DSL_STO_CMD_030"), })
    public void testDownloadFile() throws URISyntaxException, IOException {
        Order order = new Order();
        order.setOwner(USER);
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
        files1Task.setOwner(USER);
        files1Task.setOrderId(order.getId());
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/file1.txt");
        dataFile1.setFilename(testFile.getName());
        dataFile1.setIpId(DO1_IP_ID);
        dataFile1.setOnline(true);
        dataFile1.setReference(false);
        // Use filename as checksum (same as OrderControllerIT)
        dataFile1.setChecksum(dataFile1.getFilename());
        dataFile1.setOrderId(order.getId());
        dataFile1.setFilesize(testFile.length());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        dataFile1.setDataType(DataType.RAWDATA);
        dataFileRepository.save(dataFile1);
        files1Task.addFile(dataFile1);
        ds1Task.addReliantTask(files1Task);

        order = orderRepository.save(order);
        ds1Task = order.getDatasetTasks().first();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        File resultFile;
        int count = 0;
        do {
            count++;
            ResultActions resultActions = performDefaultGet(OrderDataFileController.ORDERS_FILES_DATA_FILE_ID,
                                                            customizer, "Should return result", dataFile1.getId());

            assertMediaType(resultActions, MediaType.TEXT_PLAIN);
            resultFile = File.createTempFile("ORDER", "");
            resultFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(resultFile)) {
                // Wait for availability
                resultActions.andReturn().getAsyncResult();
                InputStream is = new ByteArrayInputStream(
                        resultActions.andReturn().getResponse().getContentAsByteArray());
                ByteStreams.copy(is, fos);
                is.close();
            }
        } while ((resultFile.length() == 0) && (count < 4));
        Assert.assertTrue(Files.equal(testFile, resultFile));

        tenantResolver.forceTenant(getDefaultTenant()); // ?

        Optional<OrderDataFile> dataFileOpt = dataFileRepository
                .findFirstByChecksumAndIpIdAndOrderId(dataFile1.getChecksum(), DO1_IP_ID, order.getId());
        Assert.assertTrue(dataFileOpt.isPresent());
        Assert.assertEquals(FileState.DOWNLOADED, dataFileOpt.get().getState());
    }
}