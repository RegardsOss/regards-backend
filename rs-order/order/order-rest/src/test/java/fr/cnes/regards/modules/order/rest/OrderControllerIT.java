package fr.cnes.regards.modules.order.rest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.xml.sax.SAXException;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.metalink.schema.FileType;
import fr.cnes.regards.modules.order.metalink.schema.MetalinkType;
import fr.cnes.regards.modules.order.metalink.schema.ObjectFactory;
import fr.cnes.regards.modules.order.metalink.schema.ResourcesType;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author oroussel
 */
@ContextConfiguration(classes = OrderConfiguration.class)
public class OrderControllerIT extends AbstractRegardsIT {

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

    @Autowired
    private Gson gson;

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

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testActions() throws UnsupportedEncodingException {
        // Create an empty basket
        Basket basket = new Basket();
        basket.setOwner(DEFAULT_USER_EMAIL);
        basketRepos.save(basket);

        // Test POST without argument : order should be created with RUNNING status
        ResultActions results = performDefaultPost(OrderController.USER_ROOT_PATH, null,
                                                   Lists.newArrayList(MockMvcResultMatchers.status().isCreated()),
                                                   "error");
        String jsonResult = results.andReturn().getResponse().getContentAsString();
        Long orderId = Long.valueOf(jsonResult.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Retrieve order and test its status
        assertStatus(orderId, OrderStatus.RUNNING);

        // Pause Order
        results = performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, okExpectations(), "error", orderId);
        // Retrieve order and test its status
        assertStatus(orderId, OrderStatus.PAUSED);

        // Resume Order (immediate, order hasn't any files nor jobInfos)
        results = performDefaultPut(OrderController.RESUME_ORDER_PATH, null, okExpectations(), "error", orderId);
        // Retrieve order and test its status
        assertStatus(orderId, OrderStatus.RUNNING);

        // Try to delete
        results = performDefaultDelete(OrderController.DELETE_ORDER_PATH,
                                       expectations(MockMvcResultMatchers.status().isUnauthorized()), "error", orderId);

        // Re-pause
        results = performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, okExpectations(), "error", orderId);

        // Try to remove
        results = performDefaultDelete(OrderController.REMOVE_ORDER_PATH,
                                       expectations(MockMvcResultMatchers.status().isUnauthorized()), "error", orderId);

        // Re-delete
        results = performDefaultDelete(OrderController.DELETE_ORDER_PATH, okExpectations(), "error", orderId);
        // This time : ok
        assertStatus(orderId, OrderStatus.DELETED);

        // Re-remove
        results = performDefaultDelete(OrderController.REMOVE_ORDER_PATH, okExpectations(), "error", orderId);
        // This time : ok, so no more  order exists in database
        performDefaultGet(OrderController.GET_ORDER_PATH, expectations(MockMvcResultMatchers.status().isNotFound()),
                          "error", orderId);

    }

    private void assertStatus(Long orderId, OrderStatus expectedStatus) throws UnsupportedEncodingException {
        List<ResultMatcher> expectations = okExpectations();
        ResultActions results = performDefaultGet(OrderController.GET_ORDER_PATH, expectations, "error", orderId);
        String jsonResult = results.andReturn().getResponse().getContentAsString();
        Assert.assertTrue(jsonResult.contains("\"status\":\"" + expectedStatus.toString() + "\""));
    }

    private List<ResultMatcher> okExpectations() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        return expectations;
    }

    private List<ResultMatcher> expectations(ResultMatcher resultMatcher) {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(resultMatcher);
        return expectations;
    }

    @Test
    public void testCreateNOK() {
        // All baskets have been deleted so order creation must fail
        // Test POST without argument
        performDefaultPost(OrderController.USER_ROOT_PATH, null,
                           Lists.newArrayList(MockMvcResultMatchers.status().isNoContent()), "error");
    }

    @Requirement("REGARDS_DSL_STO_CMD_010")
    @Requirement("REGARDS_DSL_STO_CMD_040")
    @Requirement("REGARDS_DSL_STO_CMD_120")
    @Requirement("REGARDS_DSL_STO_CMD_400")
    @Requirement("REGARDS_DSL_STO_CMD_410")
    @Test
    public void testDownloadFile()
            throws URISyntaxException, IOException, InterruptedException, JAXBException, SAXException,
            ParserConfigurationException {
        Order order = new Order();
        order.setOwner(DEFAULT_USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order = orderRepository.save(order);

        // dataset task 1
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");
        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(DEFAULT_USER_EMAIL);
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_hd.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_md.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_sd.txt", FileState.ONLINE));
        ds1Task.addReliantTask(files1Task);

        // dataset task 2
        DatasetTask ds2Task = new DatasetTask();
        ds2Task.setDatasetIpid(DS2_IP_ID.toString());
        ds2Task.setDatasetLabel("DS2");
        order.addDatasetOrderTask(ds2Task);

        FilesTask files20Task = new FilesTask();
        files20Task.setOwner(DEFAULT_USER_EMAIL);
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_hd.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_md.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_sd.txt", FileState.AVAILABLE));
        ds2Task.addReliantTask(files20Task);

        FilesTask files21Task = new FilesTask();
        files21Task.setOwner(DEFAULT_USER_EMAIL);
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_hd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_md_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_sd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO4_IP_ID, "file3.txt", FileState.PENDING));
        files21Task.addFile(createOrderDataFile(order, DO5_IP_ID, "file4.txt", FileState.DOWNLOADED));
        ds2Task.addReliantTask(files21Task);

        order = orderRepository.save(order);
        ds1Task = order.getDatasetTasks().first();

        List<ResultMatcher> expectations = okExpectations();

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH, expectations,
                                                        "Should return result", order.getId());
        Assert.assertEquals("application/metalink+xml", resultActions.andReturn().getResponse().getContentType());
        File resultFileMl = File.createTempFile("ORDER_", ".metalink");
        resultFileMl.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFileMl)) {
            // WAit for availability
            resultActions.andReturn().getAsyncResult();
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertTrue(resultFileMl.length() > 8000l); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

        List<ResultMatcher> expectations2 = okExpectations();
        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        resultActions = performDefaultGet("/user/orders/{orderId}/download", expectations2, "Should return result",
                                          order.getId());
        assertMediaType(resultActions, MediaType.APPLICATION_OCTET_STREAM);
        File resultFile = File.createTempFile("ZIP_ORDER_", ".zip");
        resultFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            Object o = resultActions.andReturn().getAsyncResult();
            System.out.println(o);
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertEquals(1816l, resultFile.length());

        tenantResolver.forceTenant(DEFAULT_TENANT); // ?

        // 12 files from AVAILABLE to DOWNLOADED + the one already at this state
        List<OrderDataFile> dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.DOWNLOADED);
        Assert.assertEquals(13, dataFiles.size());

        //////////////////////////////////////////////////////
        // Check that URL from metalink file are correct    //
        // ie download all files with public url and token  //
        //////////////////////////////////////////////////////
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        int fileCount = 0;
        for (FileType fileType : metalink.getFiles().getFile()) {
            ResourcesType.Url urlO = fileType.getResources().getUrl().iterator().next();
            String completeUrl = urlO.getValue();
            // Only /orders/... part is interesting us
            String url = completeUrl.substring(completeUrl.indexOf("/orders/"));
            // extract aipId and checksum
            String[] urlParts = url.split("/");
            String aipId = urlParts[3];
            String checksum = urlParts[5].substring(0, urlParts[5].indexOf('?'));
            String token = urlParts[5].substring(urlParts[5].indexOf('=') + 1);

            List<ResultMatcher> expects;
            // File file3.txt has a status PENDING...
            if (!checksum.contains("file3.txt")) {
                expects = okExpectations();
            } else { // ...so its HttpStatus is 202 (ACCEPTED)
                expects = expectations(MockMvcResultMatchers.status().isAccepted());
            }

            // Try downloading file as if, with token given into public file url
            ResultActions results = performDefaultGet(OrderDataFileController.ORDERS_AIPS_AIP_ID_FILES_CHECKSUM,
                                                      expects, "Should return result",
                                                      RequestParamBuilder.build().param("orderToken", token), aipId,
                                                      checksum, token);

            assertMediaType(results, MediaType.TEXT_PLAIN);
            fileCount++;
        }
        Assert.assertEquals(14, fileCount);

    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAll() throws UnsupportedEncodingException {
        Order order1 = new Order();
        order1.setOwner("RIRI");
        order1.setCreationDate(OffsetDateTime.now());
        order1.setExpirationDate(order1.getCreationDate().plus(3, ChronoUnit.DAYS));
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOwner(DEFAULT_USER_EMAIL);
        order2.setCreationDate(OffsetDateTime.now());
        order2.setExpirationDate(order2.getCreationDate().plus(3, ChronoUnit.DAYS));

        orderRepository.save(order2);

        Order order3 = new Order();
        order3.setOwner("FIFI");
        order3.setCreationDate(OffsetDateTime.now());
        order3.setExpirationDate(order3.getCreationDate().plus(3, ChronoUnit.DAYS));
        orderRepository.save(order3);

        // All orders
        List<ResultMatcher> expects = okExpectations();
        expects.add(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(3)));
        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH, expects, "errors");

        // All specific user orders
        expects = okExpectations();
        expects.add(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        results = performDefaultGet(OrderController.ADMIN_ROOT_PATH, expects, "errors", RequestParamBuilder.build().param("user", "FIFI"));

        // Only owner orders
        expects = okExpectations();
        expects.add(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        results = performDefaultGet(OrderController.USER_ROOT_PATH, expects, "errors");
        System.out.println(results.andReturn().getResponse().getContentAsString());
    }


    @Test
    public void testCsv() throws URISyntaxException, UnsupportedEncodingException {
        Order order = new Order();
        order.setOwner(DEFAULT_USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order = orderRepository.save(order);

        // dataset task 1
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");
        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(DEFAULT_USER_EMAIL);
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_hd.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_md.txt", FileState.ONLINE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_sd.txt", FileState.ONLINE));
        ds1Task.addReliantTask(files1Task);

        // dataset task 2
        DatasetTask ds2Task = new DatasetTask();
        ds2Task.setDatasetIpid(DS2_IP_ID.toString());
        ds2Task.setDatasetLabel("DS2");
        order.addDatasetOrderTask(ds2Task);

        FilesTask files20Task = new FilesTask();
        files20Task.setOwner(DEFAULT_USER_EMAIL);
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_hd.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_md.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_sd.txt", FileState.AVAILABLE));
        ds2Task.addReliantTask(files20Task);

        FilesTask files21Task = new FilesTask();
        files21Task.setOwner(DEFAULT_USER_EMAIL);
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_hd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_md_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_sd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO4_IP_ID, "file3.txt", FileState.PENDING));
        files21Task.addFile(createOrderDataFile(order, DO5_IP_ID, "file4.txt", FileState.DOWNLOADED));
        ds2Task.addReliantTask(files21Task);

        order = orderRepository.save(order);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpConstants.CONTENT_TYPE, "application/json");
        headers.add(HttpConstants.ACCEPT, "text/csv");
        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH, okExpectations(), "error",
                                                  headers);
        // Just test headers are present and CSV format is ok
        Assert.assertTrue(results.andReturn().getResponse().getContentAsString().startsWith("ORDER_ID;CREATION_DATE;EXPIRATION_DATE"));
    }

    private OrderDataFile createOrderDataFile(Order order, UniformResourceName aipId, String filename, FileState state)
            throws URISyntaxException {
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/" + filename);
        dataFile1.setName(filename);
        File file = new File("src/test/resources/files/" + filename);
        dataFile1.setSize(file.length());
        dataFile1.setIpId(aipId);
        if (state == FileState.ONLINE) {
            dataFile1.setOnline(true);
        } else {
            dataFile1.setOnline(false);
            dataFile1.setState(state);
        }
        dataFile1.setChecksum(filename);
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        dataFileRepository.save(dataFile1);
        return dataFile1;
    }
}
