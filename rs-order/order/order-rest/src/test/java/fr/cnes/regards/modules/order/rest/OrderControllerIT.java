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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.xml.sax.SAXException;

import com.google.common.io.ByteStreams;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
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
 * @author Sébastien Binda
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

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        basketRepos.deleteAll();

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(Matchers.anyString()))
                .thenReturn(ResponseEntity.ok(new Resource<>(project)));
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testCreationWithEmptyBasket() {
        // Create an empty basket
        Basket basket = new Basket();
        basket.setOwner(getDefaultUserEmail());
        basketRepos.save(basket);

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        // Test POST with empty order => 201, an order creation cannot fail
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest(), customizer, "error");
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
    public void testDelete() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer, "error", order.getId());

        Thread.sleep(1000);

        // Delete Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer, "error", order.getId());
    }

    @Test
    public void testDeleteFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isUnauthorized());
        // Pause Order
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
    public void testCreateNOK() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        // Add doc
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderController.OrderRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields
                .withPath("onSuccessUrl", "url used by frontend to display a page if order creation has succeeded")
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
        DatasetTask ds1Task = order.getDatasetTasks().first();

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

        @SuppressWarnings("unchecked")
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
        customizer.addDocumentationSnippet(RequestDocumentation
                .relaxedRequestParameters(RequestDocumentation.parameterWithName("orderToken").optional()
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
    public void testDownloadZipFile() throws URISyntaxException, IOException, InterruptedException, JAXBException,
            SAXException, ParserConfigurationException {
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

    @Test
    public void testFindAllOrderFiles() throws URISyntaxException {
        Order order = createOrderAsPending();
        orderRepository.save(order);

        order = orderRepository.findCompleteById(order.getId());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("page", "0");
        customizer.customizeRequestParam().param("size", "20");
        // request parameters
        customizer.addDocumentationSnippet(RequestDocumentation
                .relaxedRequestParameters(RequestDocumentation.parameterWithName("page").optional()
                        .description("page number (from 0)")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")),
                                          RequestDocumentation.parameterWithName("size").optional()
                                                  .description("page size").attributes(Attributes
                                                          .key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
        customizer.addDocumentationSnippet(RequestDocumentation
                .pathParameters(RequestDocumentation.parameterWithName("datasetId")
                        .description("dataset task id (from order)")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Long")),
                                RequestDocumentation.parameterWithName("orderId").description("order id")
                                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                .value("Long"))));

        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDataFile.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "files").optional().type(JSON_ARRAY_TYPE));
        customizer.addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultGet(OrderDataFileController.ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES, customizer, "error",
                          order.getId(), order.getDatasetTasks().first().getId());

    }

    @Requirement("REGARDS_DSL_STO_CMD_010")
    @Requirement("REGARDS_DSL_STO_CMD_040")
    @Requirement("REGARDS_DSL_STO_CMD_120")
    @Requirement("REGARDS_DSL_STO_CMD_400")
    @Requirement("REGARDS_DSL_STO_CMD_410")
    @Requirement("REGARDS_DSL_STO_ARC_420")
    @Test
    public void testDownloadFile() throws URISyntaxException, IOException, InterruptedException, JAXBException,
            SAXException, ParserConfigurationException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
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

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        resultActions = performDefaultGet("/user/orders/{orderId}/download", customizer, "Should return result",
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

        tenantResolver.forceTenant(getDefaultTenant()); // ?

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

        @SuppressWarnings("unchecked")
        JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        int fileCount = 0;
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

            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                continue;
            }
            i++;
            // Download only 5 files
            if (i > 5) {
                continue;
            }
            customizer = getNewRequestBuilderCustomizer();
            customizer.addExpectation(MockMvcResultMatchers.status().isOk());
            customizer.customizeRequestParam().param("orderToken", token);

            customizer.addDocumentationSnippet(RequestDocumentation
                    .relaxedRequestParameters(RequestDocumentation.parameterWithName("orderToken").optional()
                            .description("token generated at order creation and sent by email to user.")
                            .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"))));

            customizer.addDocumentationSnippet(RequestDocumentation
                    .pathParameters(RequestDocumentation.parameterWithName("aipId")
                            .description("IP_ID of data object of which file belongs to")
                            .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")),
                                    RequestDocumentation.parameterWithName("dataFileId").description("file id ")
                                            .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                    .value("Long"))));

            // Try downloading file as if, with token given into public file url
            ResultActions results = performDefaultGet(OrderDataFileController.ORDERS_AIPS_AIP_ID_FILES_ID, customizer,
                                                      "Should return result", aipId, dataFileId);

            File tmpFile = File.createTempFile("ORDER", "tmp", new File("/tmp"));
            tmpFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                resultActions.andReturn().getAsyncResult();
                InputStream is = new ByteArrayInputStream(
                        resultActions.andReturn().getResponse().getContentAsByteArray());
                ByteStreams.copy(is, fos);
                is.close();
            }

            assertMediaType(results, MediaType.TEXT_PLAIN);
            fileCount++;
        }
        Assert.assertEquals(5, fileCount);

    }

    @Test
    public void testDownloadNotYetAvailableFile() throws URISyntaxException, IOException, JAXBException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
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

        tenantResolver.forceTenant(getDefaultTenant()); // ?

        // 1 file at state PENDING
        List<OrderDataFile> dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.PENDING);
        Assert.assertEquals(1, dataFiles.size());
        long fileText3TxtId = dataFiles.get(0).getId();

        //////////////////////////////////////////////////////
        // Check that URL from metalink file are correct    //
        //////////////////////////////////////////////////////
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        @SuppressWarnings("unchecked")
        JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        // Some variable to make data file not yet available download as last action (for Doc with a 202 expectation)
        long lastDataFileId = -1l;
        String lastDataFileAipId = null;
        String lastDataFileToken = null;
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

            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                lastDataFileId = dataFileId;
                lastDataFileAipId = aipId;
                lastDataFileToken = token;
                break;
            }
        }
        // Attempt to download not yet available data file
        customizer = getNewRequestBuilderCustomizer();
        // 202
        customizer.addExpectation(MockMvcResultMatchers.status().isAccepted());
        customizer.customizeRequestParam().param("orderToken", lastDataFileToken);
        performDefaultGet(OrderDataFileController.ORDERS_AIPS_AIP_ID_FILES_ID, customizer, "Should return result",
                          lastDataFileAipId, lastDataFileId);
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
        order.setOwner(getDefaultUserEmail());
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order = orderRepository.save(order);

        // dataset task 1
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");
        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(getDefaultUserEmail());
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
        files20Task.setOwner(getDefaultUserEmail());
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_hd.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_md.txt", FileState.AVAILABLE));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_sd.txt", FileState.AVAILABLE));
        files20Task.setOrderId(order.getId());
        ds2Task.addReliantTask(files20Task);

        FilesTask files21Task = new FilesTask();
        files21Task.setOwner(getDefaultUserEmail());
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
        performDefaultGet(OrderController.ADMIN_ROOT_PATH, customizer, "errors");
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
        customizer.addDocumentationSnippet(RequestDocumentation
                .relaxedRequestParameters(RequestDocumentation.parameterWithName("user").optional()
                        .description("Optional - user email whom orders are requested, if not provided all users orders are retrieved")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")),
                                          RequestDocumentation.parameterWithName("page").optional()
                                                  .description("page number (from 0)")
                                                  .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value("Integer")),
                                          RequestDocumentation.parameterWithName("size").optional()
                                                  .description("page size").attributes(Attributes
                                                          .key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
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
        customizer.addDocumentationSnippet(RequestDocumentation
                .relaxedRequestParameters(RequestDocumentation.parameterWithName("page").optional()
                        .description("page number (from 0)")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")),
                                          RequestDocumentation.parameterWithName("size").optional()
                                                  .description("page size").attributes(Attributes
                                                          .key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
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
        order2.setOwner(getDefaultUserEmail());
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

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeHeaders().add(HttpConstants.CONTENT_TYPE, "application/json");
        customizer.customizeHeaders().add(HttpConstants.ACCEPT, "text/csv");

        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV, customizer,
                                                  "error");
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
        dataFile1.setFilename(filename);
        File file = new File("src/test/resources/files/" + filename);
        dataFile1.setFilesize(file.length());
        dataFile1.setReference(false);
        dataFile1.setIpId(aipId);
        dataFile1.setOnline(online);
        dataFile1.setState(state);
        dataFile1.setChecksum(filename);
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        dataFile1.setDataType(DataType.RAWDATA);
        dataFileRepository.save(dataFile1);
        return dataFile1;
    }
}
