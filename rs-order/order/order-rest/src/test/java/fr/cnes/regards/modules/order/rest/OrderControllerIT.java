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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
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
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
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
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.dto.OrderDto;
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
    public void testCreationWithEmptyBasket() {
        // Create an empty basket
        Basket basket = new Basket();
        basket.setOwner(DEFAULT_USER_EMAIL);
        basketRepos.save(basket);

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        // Test POST with empty order => 201, an order creation cannot fail
        ResultActions results = performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest(),
                                                   customizer, "error");
    }

    @Test
    public void testPause() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer, "error", order.getId());
    }

    @Test
    public void testPauseFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer, "error", order.getId());

        // Re-pause Order => fail
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnauthorized());
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer, "error", order.getId());
    }

    @Test
    public void testResume() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer, "error", order.getId());

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH, null, customizer, "error", order.getId());
    }

    @Test
    public void testResumeFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnauthorized());
        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH, null, customizer, "error", order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testDelete() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Delete Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer, "error", order.getId());
    }

    @Test
    public void testRemoveFailed() throws URISyntaxException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnauthorized());
        // Pause Order
        performDefaultDelete(OrderController.REMOVE_ORDER_PATH, customizer, "error", order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testRemove() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Delete Order
        performDefaultDelete(OrderController.REMOVE_ORDER_PATH, customizer, "error", order.getId());
    }

    @Test
    public void testDeleteFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnauthorized());
        // Pause Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer, "error", order.getId());
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
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        // Add doc
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderController.OrderRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("onSuccessUrl",
                                              "url used by frontend to display a page if order creation has succeeded")
                           .optional().type(JSON_STRING_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedRequestFields(fields));

        // All baskets have been deleted so order creation must fail
        // Test POST without argument
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest("http://perdu.com"),
                           customizer, "error");
    }

    @Test
    public void testGetOrder() throws URISyntaxException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Add Doc

        ConstrainedFields constrainedFields = new ConstrainedFields(BasketSelectionRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "order object").optional().type(JSON_OBJECT_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultGet(OrderController.GET_ORDER_PATH, customizer, "error", order.getId());

    }

    @Test
    public void testGetNotFoundOrder() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNotFound());

        performDefaultGet(OrderController.GET_ORDER_PATH, customizer, "error", -12);
    }

    @Test
    public void testDownloadMetalinkFile() throws IOException, URISyntaxException {
        Order order = createOrderAsRunning();
        DatasetTask ds1Task;
        ds1Task = order.getDatasetTasks().first();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH, customizer,
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

    }

    @Test
    public void testPublicDownloadMetalinkFile() throws IOException, URISyntaxException, JAXBException {
        // Create order
        Order order = createOrderAsRunning();

        // Download metalink file
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH, customizer,
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

        // Read Metalink to retrieve public token
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        FileType fileType = metalink.getFiles().getFile().get(0);
        ResourcesType.Url urlO = fileType.getResources().getUrl().iterator().next();
        String completeUrl = urlO.getValue();
        // Only /orders/... part is interesting us
        String url = completeUrl.substring(completeUrl.indexOf("/orders/"));
        // extract aipId and checksum
        String[] urlParts = url.split("/");

        // Stop at "scope=PROJECT"
        String token = urlParts[5].substring(urlParts[5].indexOf('=') + 1, urlParts[5].indexOf('&'));

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("orderToken", token);
        // request parameters
        customizer.addDocumentationSnippet(RequestDocumentation.relaxedRequestParameters(
                RequestDocumentation.parameterWithName("orderToken").optional()
                        .description("token generated at order creation and sent by email to user.")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"))));

        // Try downloading file as if, with token given into public file url
        performDefaultGet(OrderController.PUBLIC_METALINK_DOWNLOAD_PATH, customizer, "Should return result");
    }

    @Requirement("REGARDS_DSL_STO_CMD_010")
    @Requirement("REGARDS_DSL_STO_CMD_040")
    @Requirement("REGARDS_DSL_STO_CMD_120")
    @Requirement("REGARDS_DSL_STO_CMD_400")
    @Requirement("REGARDS_DSL_STO_CMD_410")
    @Requirement("REGARDS_DSL_STO_ARC_420")
    @Test
    public void testDownloadZipFile()
            throws URISyntaxException, IOException, InterruptedException, JAXBException, SAXException,
            ParserConfigurationException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.ZIP_DOWNLOAD_PATH, customizer,
                                                        "Should return result", order.getId());
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
    }

    @Requirement("REGARDS_DSL_STO_CMD_010")
    @Requirement("REGARDS_DSL_STO_CMD_040")
    @Requirement("REGARDS_DSL_STO_CMD_120")
    @Requirement("REGARDS_DSL_STO_CMD_400")
    @Requirement("REGARDS_DSL_STO_CMD_410")
    @Requirement("REGARDS_DSL_STO_ARC_420")
    @Test
    public void testDownloadFile()
            throws URISyntaxException, IOException, InterruptedException, JAXBException, SAXException,
            ParserConfigurationException {
        Order order = createOrderAsRunning();
        DatasetTask ds1Task;
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
        // 1 file at state PENDING
        dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.PENDING);
        Assert.assertEquals(1, dataFiles.size());
        long fileText3TxtId = dataFiles.get(0).getId();

        //////////////////////////////////////////////////////
        // Check that URL from metalink file are correct    //
        // ie download all files with public url and token  //
        //////////////////////////////////////////////////////
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        int fileCount = 0;
        // Some variable to make data file not yet available download as last action (for Doc with a 202 expectation)
        long lastDataFileId = -1l;
        String lastDataFileAipId = null;
        String lastDataFileToken = null;
        int i = 0;
        for (FileType fileType : metalink.getFiles().getFile()) {
            ResourcesType.Url urlO = fileType.getResources().getUrl().iterator().next();
            String completeUrl = urlO.getValue();
            // Only /orders/... part is interesting us
            String url = completeUrl.substring(completeUrl.indexOf("/orders/"));
            // extract aipId and checksum
            String[] urlParts = url.split("/");
            String aipId = urlParts[3];
            long dataFileId = Long.parseLong(urlParts[5].substring(0, urlParts[5].indexOf('?')));
            // Stop at "scope=PROJECT"
            String token = urlParts[5].substring(urlParts[5].indexOf('=') + 1, urlParts[5].indexOf('&'));

            List<ResultMatcher> expects;
            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                lastDataFileId = dataFileId;
                lastDataFileAipId = aipId;
                lastDataFileToken = token;
                continue;
            }
            i++;
            // Download only 5 files because something is weird about asynchronous streaming body, transactions
            // and connection pool
            if (i > 5) {
                continue;
            }
            RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
            customizer.addExpectation(MockMvcResultMatchers.status().isOk());
            customizer.customizeRequestParam().param("orderToken", token);
            System.out.println("DOWNLOADING " + url);
            // Try downloading file as if, with token given into public file url
            ResultActions results = performDefaultGet(OrderDataFileController.ORDERS_AIPS_AIP_ID_FILES_ID, customizer,
                                                      "Should return result", aipId, dataFileId);

            File tmpFile = File.createTempFile("ORDER", "tmp", new File("/tmp"));
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                Object o = resultActions.andReturn().getAsyncResult();
                System.out.println(o);
                InputStream is = new ByteArrayInputStream(
                        resultActions.andReturn().getResponse().getContentAsByteArray());
                ByteStreams.copy(is, fos);
                is.close();
            }
            Thread.sleep(2000);

            assertMediaType(results, MediaType.TEXT_PLAIN);
            fileCount++;
        }
        // Attempt to download not yet available data file
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        // 202
        customizer.addExpectation(MockMvcResultMatchers.status().isAccepted());
        customizer.customizeRequestParam().param("orderToken", lastDataFileToken);
        ResultActions results = performDefaultGet(OrderDataFileController.ORDERS_AIPS_AIP_ID_FILES_ID, customizer,
                                                  "Should return result", lastDataFileAipId, lastDataFileId);

        Assert.assertEquals(5, fileCount);

    }

    private Order createOrderAsRunning() throws URISyntaxException {
        Order order = createOrderAsPending();

        order.setStatus(OrderStatus.RUNNING);
        order.setPercentCompleted(23);
        order.setAvailableFilesCount(2);

        order = orderRepository.save(order);
        return order;
    }

    private Order createOrderAsPending() throws URISyntaxException {
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
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1.txt", FileState.AVAILABLE, true));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_hd.txt", FileState.AVAILABLE, true));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_md.txt", FileState.AVAILABLE, true));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_sd.txt", FileState.AVAILABLE, true));
        files1Task.setOrderId(order.getId());
        ds1Task.addReliantTask(files1Task);
        ds1Task.setFilesCount(4);
        ds1Task.setObjectsCount(4);
        ds1Task.setFilesSize(52221122);

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
        files20Task.setOrderId(order.getId());
        ds2Task.addReliantTask(files20Task);

        FilesTask files21Task = new FilesTask();
        files21Task.setOwner(DEFAULT_USER_EMAIL);
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_hd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_md_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_sd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO4_IP_ID, "file3.txt", FileState.PENDING));
        files21Task.addFile(createOrderDataFile(order, DO5_IP_ID, "file4.txt", FileState.DOWNLOADED));
        files21Task.setOrderId(order.getId());
        ds2Task.addReliantTask(files21Task);
        ds2Task.setFilesCount(10);
        ds2Task.setObjectsCount(10);
        ds2Task.setFilesSize(52221122);
        return order;
    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAll() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All orders
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(3)));
        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH, customizer, "errors");
    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAllSpecificUser() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All specific user orders
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        customizer.customizeRequestParam().param("user", "other.user2@regards.fr");
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "20");
        // request parameters
        customizer.addDocumentationSnippet(RequestDocumentation.relaxedRequestParameters(
                RequestDocumentation.parameterWithName("user").optional().description(
                        "Optional - user email whom orders are requested, if not provided all users orders are retrieved")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")),
                RequestDocumentation.parameterWithName("page").optional().description("page number (from 0)")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")),
                RequestDocumentation.parameterWithName("size").optional().description("page size")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
        // response body
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDto.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "orders").optional().type(JSON_ARRAY_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(fields));
        performDefaultGet(OrderController.ADMIN_ROOT_PATH, customizer, "errors");
    }

    @Test
    public void testFindAllOwner() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All specific user orders
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "20");
        // request parameters
        customizer.addDocumentationSnippet(RequestDocumentation.relaxedRequestParameters(
                RequestDocumentation.parameterWithName("page").optional().description("page number (from 0)")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")),
                RequestDocumentation.parameterWithName("size").optional().description("page size")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
        // response body
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDto.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "orders").optional().type(JSON_ARRAY_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(fields));
        performDefaultGet(OrderController.USER_ROOT_PATH, customizer, "errors");
    }

    private void createSeveralOrdersWithDifferentOwners() {
        Order order1 = new Order();
        order1.setOwner("other.user1@regards.fr");
        order1.setCreationDate(OffsetDateTime.now());
        order1.setExpirationDate(order1.getCreationDate().plus(3, ChronoUnit.DAYS));
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOwner(DEFAULT_USER_EMAIL);
        order2.setCreationDate(OffsetDateTime.now());
        order2.setExpirationDate(order2.getCreationDate().plus(3, ChronoUnit.DAYS));

        orderRepository.save(order2);

        Order order3 = new Order();
        order3.setOwner("other.user2@regards.fr");
        order3.setCreationDate(OffsetDateTime.now());
        order3.setExpirationDate(order3.getCreationDate().plus(3, ChronoUnit.DAYS));
        orderRepository.save(order3);
    }

    @Test
    public void testCsv() throws URISyntaxException, UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpConstants.CONTENT_TYPE, "application/json");
        headers.add(HttpConstants.ACCEPT, "text/csv");
        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV,
                                                  okExpectations(), "error", headers);
        // Just test headers are present and CSV format is ok
        Assert.assertTrue(results.andReturn().getResponse().getContentAsString()
                                  .startsWith("ORDER_ID;CREATION_DATE;EXPIRATION_DATE"));
    }

    private OrderDataFile createOrderDataFile(Order order, UniformResourceName aipId, String filename, FileState state)
            throws URISyntaxException {
        return createOrderDataFile(order, aipId, filename, state, false);
    }

    private OrderDataFile createOrderDataFile(Order order, UniformResourceName aipId, String filename, FileState state,
            boolean online) throws URISyntaxException {
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/" + filename);
        dataFile1.setName(filename);
        File file = new File("src/test/resources/files/" + filename);
        dataFile1.setSize(file.length());
        dataFile1.setIpId(aipId);
        dataFile1.setOnline(online);
        dataFile1.setState(state);
        dataFile1.setChecksum(filename);
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        dataFileRepository.save(dataFile1);
        return dataFile1;
    }
}
