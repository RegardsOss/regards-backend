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
package fr.cnes.regards.modules.order.rest;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.SearchRequestParameters;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.OrderLabelErrorEnum;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.dto.dto.BasketSelectionRequest;
import fr.cnes.regards.modules.order.dto.dto.OrderDto;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.metalink.schema.FileType;
import fr.cnes.regards.modules.order.metalink.schema.MetalinkType;
import fr.cnes.regards.modules.order.metalink.schema.ObjectFactory;
import fr.cnes.regards.modules.order.metalink.schema.ResourcesType;
import fr.cnes.regards.modules.order.rest.mock.StorageClientMock;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.hamcrest.text.MatchesPattern;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "regards.tenant=order1", "spring.jpa.properties.hibernate.default_schema=order1" })
public class OrderControllerIT extends AbstractOrderControllerIT {

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testCreationWithEmptyBasket() {
        // Create an empty basket
        Basket basket = new Basket();
        basket.setOwner(getDefaultUserEmail());
        basketRepository.save(basket);
        RequestBuilderCustomizer customizer = customizer().expectStatusCreated();
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Test POST with empty order => 201, an order creation cannot fail
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest(), customizer, "error");
    }

    @Test
    public void testPause() throws URISyntaxException {
        Order order = createOrderAsPending();
        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());
    }

    @Test
    public void testPauseFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());

        // Re-pause Order => fail
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatus(HttpStatus.UNAUTHORIZED),
                          "error",
                          order.getId());
    }

    @Test
    public void testResume() throws URISyntaxException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());

        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());
    }

    @Test
    public void testResumeFailed() throws URISyntaxException {
        Order order = createOrderAsPending();
        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH,
                          null,
                          customizer().expectStatus(HttpStatus.UNAUTHORIZED),
                          "error",
                          order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testDelete() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());

        Thread.sleep(1000);

        // Delete Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer().expectStatusOk(), "error", order.getId());
    }

    @Test
    public void testDeleteFailed() throws URISyntaxException {
        Order order = createOrderAsPending();
        // Pause Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH,
                             customizer().expectStatus(HttpStatus.UNAUTHORIZED),
                             "error",
                             order.getId());
    }

    @Test
    public void testRemoveFailed() throws URISyntaxException {
        Order order = createOrderAsRunning();
        // Pause Order
        performDefaultDelete(OrderController.REMOVE_ORDER_PATH,
                             customizer().expectStatus(HttpStatus.UNAUTHORIZED),
                             "error",
                             order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testRemove() throws URISyntaxException {
        Order order = createOrderAsPending();
        performDefaultPut(OrderController.PAUSE_ORDER_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "error",
                          order.getId());
        performDelete(OrderController.DELETE_ORDER_PATH,
                      projectAdminToken,
                      customizer().expectStatusOk(),
                      "error",
                      order.getId());
        // Remove Order
        performDelete(OrderController.REMOVE_ORDER_PATH,
                      projectAdminToken,
                      customizer().expectStatusOk(),
                      "error",
                      order.getId());
    }

    /**
     * Create order parameters description)
     */
    private Snippet getCreateOrderDocumentation() {
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderController.OrderRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("label",
                                              "command label, 1 to 50 characters, must be unique for current user (generated when not provided)")
                                    .optional()
                                    .type(JSON_STRING_TYPE));
        fields.add(constrainedFields.withPath("onSuccessUrl",
                                              "url used by frontend to display a page if order creation has succeeded")
                                    .optional()
                                    .type(JSON_STRING_TYPE));
        return PayloadDocumentation.relaxedRequestFields(fields);
    }

    @Test
    public void testCreateOKSimpleLabel() throws URISyntaxException, InterruptedException {
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // Expectations
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.CREATED)
                                                          .expectValue("content.owner", getDefaultUserEmail())
                                                          .expectValue("content.label", "myCommand");
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myCommand", "http://perdu.com"),
                           customizer,
                           "error");
        TimeUnit.SECONDS.sleep(5);
        // After: clear
        clearForPreviousOrder();
    }

    @Test
    public void testCreateOKGenLabel() throws URISyntaxException, InterruptedException {
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // Expectations
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.CREATED)
                                                          .expectValue("content.owner", getDefaultUserEmail())
                                                          .expect(MockMvcResultMatchers.jsonPath("content.label",
                                                                                                 MatchesPattern.matchesPattern(
                                                                                                     "Order of \\d{4}/\\d{2}/\\d{2} at \\d{2}:\\d{2}:\\d{2}"))); // value should match generated pattern
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest(null, "http://perdu.com"),
                           customizer,
                           "error");
        TimeUnit.SECONDS.sleep(5);
        // After: clear
        clearForPreviousOrder();
    }

    @Test
    public void testCreateNOKLabelNonUnique() throws URISyntaxException {
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // First request expectations: OK
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.CREATED)
                                                          .expectValue("content.owner", getDefaultUserEmail())
                                                          .expectValue("content.label", "myDoubleCommand");
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myDoubleCommand", "http://perdu.com"),
                           customizer,
                           "error");
        // Second request expectations: NOK (label already used by a command for that user)
        RequestBuilderCustomizer customizer2 = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                                                           .expectValue("messages[0]",
                                                                        OrderLabelErrorEnum.LABEL_NOT_UNIQUE_FOR_OWNER.toString());
        // Send second request
        initForNextOrder();
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myDoubleCommand", "http://perdu2.com"),
                           customizer2,
                           "error");
        // After: clear
        clearForPreviousOrder();
    }

    @Test
    public void testCreateNOKLabelTooLong() throws URISyntaxException {
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // Expectations
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                                                          .expectValue("messages[0]",
                                                                       OrderLabelErrorEnum.TOO_MANY_CHARACTERS_IN_LABEL.toString());
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("this-label-has-too-many-characters-if-we-append(51)",
                                                            "http://perdu.com"),
                           customizer,
                           "error");
        // After: clear
        clearForPreviousOrder();
    }

    @Test
    public void testCreateNOKNoBasket() {
        RequestBuilderCustomizer customizer = customizer().expectStatusNoContent();
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // No basket available
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myCommand", "http://perdu.com"),
                           customizer,
                           "error");
    }

    @Test
    public void testGetOrder() throws URISyntaxException {
        Order order = createOrderAsRunning();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        // Add Doc
        ConstrainedFields constrainedFields = new ConstrainedFields(BasketSelectionRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "order object").optional().type(JSON_OBJECT_TYPE));
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultGet(OrderController.GET_ORDER_PATH, customizer, "error", order.getId());

    }

    @Test
    public void testGetNotFoundOrder() {
        performDefaultGet(OrderController.GET_ORDER_PATH, customizer().expectStatusNotFound(), "error", -12);
    }

    // TODO : Use new storage client
    @Ignore("TODO !!!!")
    @Test
    public void testDownloadMetalinkFile() throws IOException, URISyntaxException {
        Order order = createOrderAsRunning();
        order.getDatasetTasks().first();

        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
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
        Assert.assertTrue(resultFileMl.length() > 8000L); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

    }

    @Test
    public void testPublicDownloadMetalinkFile() throws IOException, URISyntaxException, JAXBException {
        // Create order
        Order order = createOrderAsRunning();

        // Download metalink file
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
        Assert.assertEquals("application/metalink+xml", resultActions.andReturn().getResponse().getContentType());
        File resultFileMl = File.createTempFile("ORDER_", ".metalink");
        resultFileMl.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFileMl)) {
            // WAit for availability
            // resultActions.andReturn().getAsyncResult();
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }

        // Read Metalink to retrieve public token
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        @SuppressWarnings("unchecked") JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(
            resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        FileType fileType = metalink.getFiles().getFile().get(0);
        ResourcesType.Url urlO = fileType.getResources().getUrl().iterator().next();
        String completeUrl = urlO.getValue();
        // Only /orders/... part is interesting us
        String url = completeUrl.substring(completeUrl.indexOf("/orders/"));
        // extract aipId and checksum
        String[] urlParts = url.split("/");

        // Stop at "scope=PROJECT"
        String token = urlParts[4].substring(urlParts[4].indexOf('=') + 1, urlParts[4].indexOf('&'));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addParameter("orderToken", token);
        // request parameters
        customizer.document(RequestDocumentation.relaxedQueryParameters(RequestDocumentation.parameterWithName(
                                                                                                "orderToken")
                                                                                            .optional()
                                                                                            .description(
                                                                                                "token generated at order creation and sent by email to user.")
                                                                                            .attributes(Attributes.key(
                                                                                                                      RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                  .value(
                                                                                                                      "String"))));

        // Try downloading file as if, with token given into public file url
        performDefaultGet(OrderController.PUBLIC_METALINK_DOWNLOAD_PATH, customizer, "Should return result");
    }

    // TODO : Use new storage client
    @Ignore("TODO !!!!")
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

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.ZIP_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
        assertMediaType(resultActions, MediaType.APPLICATION_OCTET_STREAM);
        File resultFile = File.createTempFile("ZIP_ORDER_", ".zip");
        //resultFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            Object o = resultActions.andReturn().getAsyncResult();
            System.out.println(o);
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertEquals(1816L, resultFile.length());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderControllerIT.class);

    @Test
    public void testDownloadZipFile_contains_notice_when_inner_downloads_fail()
        throws URISyntaxException, IOException, InterruptedException, JAXBException, SAXException,
        ParserConfigurationException {
        Order order = createOrderAsRunning();

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.ZIP_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
        assertMediaType(resultActions, MediaType.APPLICATION_OCTET_STREAM);

        Set<String> failures = new HashSet<>();
        // Object o = resultActions.andReturn().getAsyncResult();
        try (InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            byte[] buffer = new byte[2048];

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("NOTICE.txt")) {
                    StringBuilder sb = new StringBuilder();
                    int len = 0;
                    while ((len = zis.read(buffer)) > 0) {
                        String s = new String(buffer, 0, len, StandardCharsets.UTF_8);
                        sb.append(s);
                    }
                    failures.addAll(Arrays.asList(sb.toString().split("\n")));
                    break;
                }
            }
        }
        Assert.assertTrue(failures.size() > 0);
        Assert.assertTrue(failures.stream()
                                  .findFirst()
                                  .get()
                                  .matches(String.format("Failed to download file \\(.*\\): %s.",
                                                         StorageClientMock.NO_QUOTA_MSG_STUB)));
    }

    @Test
    public void testFindAllOrderFiles() throws URISyntaxException {
        Order order = createOrderAsPending();
        orderRepository.save(order);

        order = orderRepository.findCompleteById(order.getId());

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addParameter("page", "0");
        customizer.addParameter("size", "20");
        // request parameters
        customizer.document(RequestDocumentation.relaxedQueryParameters(RequestDocumentation.parameterWithName("page")
                                                                                            .optional()
                                                                                            .description(
                                                                                                "page number (from 0)")
                                                                                            .attributes(Attributes.key(
                                                                                                                      RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                  .value(
                                                                                                                      "Integer")),
                                                                        RequestDocumentation.parameterWithName("size")
                                                                                            .optional()
                                                                                            .description("page size")
                                                                                            .attributes(Attributes.key(
                                                                                                                      RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                  .value(
                                                                                                                      "Integer"))));
        customizer.document(RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("datasetId")
                                                                                    .description(
                                                                                        "dataset task id (from order)")
                                                                                    .attributes(Attributes.key(
                                                                                                              RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                          .value("Long")),
                                                                RequestDocumentation.parameterWithName("orderId")
                                                                                    .description("order id")
                                                                                    .attributes(Attributes.key(
                                                                                                              RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                          .value("Long"))));

        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDataFile.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "files").optional().type(JSON_ARRAY_TYPE));
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES,
                          customizer,
                          "error",
                          order.getId(),
                          order.getDatasetTasks().first().getId());

    }

    // TODO : Use new storage client
    @Ignore("TODO !!!!")
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

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
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
        Assert.assertTrue(resultFileMl.length() > 8000L); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        resultActions = performDefaultGet("/user/orders/{orderId}/download",
                                          customizer().expectStatusOk(),
                                          "Should return result",
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
        Assert.assertEquals(1816L, resultFile.length());

        tenantResolver.forceTenant(getDefaultTenant()); // ?

        // 12 files from AVAILABLE to DOWNLOADED + the one already at this state
        List<OrderDataFile> dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.DOWNLOADED);
        Assert.assertEquals(13, dataFiles.size());
        // 1 file at state PENDING
        dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.PENDING);
        Assert.assertEquals(1, dataFiles.size());
        long fileText3TxtId = dataFiles.get(0).getId();

        //////////////////////////////////////////////////////
        // Check that URL from metalink file are correct //
        // ie download all files with public url and token //
        //////////////////////////////////////////////////////
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        @SuppressWarnings("unchecked") JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(
            resultFileMl);
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
            long dataFileId = Long.parseLong(urlParts[4].substring(0, urlParts[4].indexOf('?')));
            // Stop at "scope=PROJECT"
            String token = urlParts[4].substring(urlParts[4].indexOf('=') + 1, urlParts[4].indexOf('&'));

            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                continue;
            }
            i++;
            // Download only 5 files
            if (i > 5) {
                continue;
            }
            RequestBuilderCustomizer customizer = customizer().expectStatusOk().addParameter("orderToken", token);

            customizer.document(RequestDocumentation.relaxedQueryParameters(RequestDocumentation.parameterWithName(
                                                                                                    "orderToken")
                                                                                                .optional()
                                                                                                .description(
                                                                                                    "token generated at order creation and sent by email to user.")
                                                                                                .attributes(Attributes.key(
                                                                                                                          RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                      .value(
                                                                                                                          "String"))));

            customizer.document(RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("aipId")
                                                                                        .description(
                                                                                            "IP_ID of data object of which file belongs to")
                                                                                        .attributes(Attributes.key(
                                                                                                                  RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                              .value(
                                                                                                                  "String")),
                                                                    RequestDocumentation.parameterWithName("dataFileId")
                                                                                        .description("file id ")
                                                                                        .attributes(Attributes.key(
                                                                                                                  RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                              .value(
                                                                                                                  "Long"))));

            // Try downloading file as if, with token given into public file url
            ResultActions results = performDefaultGet(OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID,
                                                      customizer,
                                                      "Should return result",
                                                      dataFileId);

            File tmpFile = File.createTempFile("ORDER", "tmp", new File("/tmp"));
            tmpFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                resultActions.andReturn().getAsyncResult();
                InputStream is = new ByteArrayInputStream(resultActions.andReturn()
                                                                       .getResponse()
                                                                       .getContentAsByteArray());
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

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(),
                                                        "Should return result",
                                                        order.getId());
        Assert.assertEquals("application/metalink+xml", resultActions.andReturn().getResponse().getContentType());
        File resultFileMl = File.createTempFile("ORDER_", ".metalink");
        resultFileMl.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(resultFileMl)) {
            // WAit for availability
            //  resultActions.andReturn().getAsyncResult();
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertTrue(resultFileMl.length() > 8000L); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

        tenantResolver.forceTenant(getDefaultTenant()); // ?

        // 1 file at state PENDING
        List<OrderDataFile> dataFiles = dataFileRepository.findByOrderIdAndStateIn(order.getId(), FileState.PENDING);
        Assert.assertEquals(1, dataFiles.size());
        long fileText3TxtId = dataFiles.get(0).getId();

        //////////////////////////////////////////////////////
        // Check that URL from metalink file are correct //
        //////////////////////////////////////////////////////
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        @SuppressWarnings("unchecked") JAXBElement<MetalinkType> rootElt = (JAXBElement<MetalinkType>) u.unmarshal(
            resultFileMl);
        MetalinkType metalink = rootElt.getValue();
        // Some variable to make data file not yet available download as last action (for Doc with a 202 expectation)
        long lastDataFileId = -1L;
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
            long dataFileId = Long.parseLong(urlParts[4].substring(0, urlParts[4].indexOf('?')));
            // Stop at "scope=PROJECT"
            String token = urlParts[4].substring(urlParts[4].indexOf('=') + 1, urlParts[4].indexOf('&'));

            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                lastDataFileId = dataFileId;
                lastDataFileAipId = URLDecoder.decode(aipId, StandardCharsets.UTF_8);
                lastDataFileToken = token;
                break;
            }
        }
        // Attempt to download not yet available data file
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.ACCEPTED);
        customizer.addParameter("orderToken", lastDataFileToken);
        performDefaultGet(OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID,
                          customizer,
                          "Should return result",
                          lastDataFileId);
    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAll() {
        createSeveralOrdersWithDifferentOwners();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        requestBuilderCustomizer.expectIsArray(JSON_PATH_CONTENT);
        requestBuilderCustomizer.expectToHaveSize(JSON_PATH_CONTENT, 3);

        SearchRequestParameters body = new SearchRequestParameters();

        performDefaultPost(OrderController.SEARCH_ORDER_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Error retrieving all orders");
    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAllSpecificUser() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All specific user orders
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.document(RequestDocumentation.queryParameters(RequestDocumentation.parameterWithName(
                                                                                                       "user")
                                                                                                   .optional()
                                                                                                   .description(
                                                                                                       "Optional - user email whom orders are requested, if not provided all users orders are retrieved")
                                                                                                   .attributes(
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                 .value(
                                                                                                                     "String")),
                                                                               RequestDocumentation.parameterWithName(
                                                                                                       "statuses")
                                                                                                   .optional()
                                                                                                   .description(
                                                                                                       "Option - list of status whom orders are requested, if not provided all orders are retrieved")
                                                                                                   .attributes(
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                 .value(
                                                                                                                     JSON_ARRAY_TYPE),
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                                                 .value(
                                                                                                                     "Values must be strings")),
                                                                               RequestDocumentation.parameterWithName(
                                                                                                       "creationDate")
                                                                                                   .optional()
                                                                                                   .description(
                                                                                                       "Option - creation date whom orders are requested, if not provided all orders are retrieved")
                                                                                                   .attributes(
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                 .value(
                                                                                                                     JSON_OBJECT_TYPE),
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                                                 .value(
                                                                                                                     "Values must be 2 ISO-8601 Dates")),
                                                                               RequestDocumentation.parameterWithName(
                                                                                                       "page")
                                                                                                   .optional()
                                                                                                   .description(
                                                                                                       "page number (from 0)")
                                                                                                   .attributes(
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                 .value(
                                                                                                                     "Integer")),
                                                                               RequestDocumentation.parameterWithName(
                                                                                                       "size")
                                                                                                   .optional()
                                                                                                   .description(
                                                                                                       "page size")
                                                                                                   .attributes(
                                                                                                       Attributes.key(
                                                                                                                     RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                 .value(
                                                                                                                     "Integer"))));
        requestBuilderCustomizer.expectStatusOk();
        requestBuilderCustomizer.expectIsArray(JSON_PATH_CONTENT);
        requestBuilderCustomizer.expectToHaveSize(JSON_PATH_CONTENT, 1);
        SearchRequestParameters body = new SearchRequestParameters();
        body.withStatusesIncluded(OrderStatus.PENDING);
        body.withOwner("other.user2@regards.fr");
        performDefaultPost(OrderController.SEARCH_ORDER_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Error retrieving all orders");
    }

    @Test
    public void testFindAllOwner() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All specific user orders
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        customizer.addParameter("page", "0");
        customizer.addParameter("size", "20");
        // request parameters
        customizer.document(RequestDocumentation.relaxedQueryParameters(RequestDocumentation.parameterWithName("page")
                                                                                            .optional()
                                                                                            .description(
                                                                                                "page number (from 0)")
                                                                                            .attributes(Attributes.key(
                                                                                                                      RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                  .value(
                                                                                                                      "Integer")),
                                                                        RequestDocumentation.parameterWithName("size")
                                                                                            .optional()
                                                                                            .description("page size")
                                                                                            .attributes(Attributes.key(
                                                                                                                      RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                  .value(
                                                                                                                      "Integer"))));
        // response body
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDto.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "orders").optional().type(JSON_ARRAY_TYPE));
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));
        performDefaultGet(OrderController.USER_ROOT_PATH, customizer, "errors");
    }

    private void createSeveralOrdersWithDifferentOwners() {
        Order order1 = new Order();
        order1.setOwner("other.user1@regards.fr");
        order1.setLabel("order1");
        order1.setCreationDate(OffsetDateTime.now());
        order1.setExpirationDate(order1.getCreationDate().plus(3, ChronoUnit.DAYS));
        order1.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT,
                                              OffsetDateTimeAdapter.format(order1.getCreationDate())));
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOwner(getDefaultUserEmail());
        order2.setLabel("order2");
        order2.setCreationDate(OffsetDateTime.now());
        order2.setExpirationDate(order2.getCreationDate().plus(3, ChronoUnit.DAYS));
        order2.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT,
                                              OffsetDateTimeAdapter.format(order2.getCreationDate())));
        orderRepository.save(order2);

        Order order3 = new Order();
        order3.setOwner("other.user2@regards.fr");
        order3.setLabel("order3");
        order3.setCreationDate(OffsetDateTime.now());
        order3.setExpirationDate(order3.getCreationDate().plus(3, ChronoUnit.DAYS));
        order3.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT,
                                              OffsetDateTimeAdapter.format(order3.getCreationDate())));
        orderRepository.save(order3);
    }

    @Test
    public void testCsv() throws UnsupportedEncodingException {

        createSeveralOrdersWithDifferentOwners();

        // Bug 550 - check that a null date somewhere does not break things
        Order order = orderRepository.findAll().stream().findAny().get();
        order.setExpirationDate(null);
        orderRepository.save(order);

        SearchRequestParameters parameters = new SearchRequestParameters();

        String path = OrderController.ADMIN_ROOT_PATH + OrderController.CSV;
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .addHeader(HttpConstants.CONTENT_TYPE, "application/json")
                                                          .addHeader(HttpConstants.ACCEPT, "text/csv");

        ResultActions results = performDefaultPost(path, parameters, customizer, "error");
        // Just test headers are present and CSV format is ok
        Assert.assertTrue(results.andReturn()
                                 .getResponse()
                                 .getContentAsString()
                                 .startsWith("ORDER_ID;CREATION_DATE;EXPIRATION_DATE"));
        // now let check that optional parameter are correctly parsed
        // First status
        parameters = parameters.withStatusesIncluded(OrderStatus.DONE);
        performDefaultPost(path, parameters, customizer, "error");
        // then from
        parameters = parameters.withCreationDateAfter(OffsetDateTime.now().minusHours(3));
        performDefaultPost(path, parameters, customizer, "error");
        // then to
        parameters = parameters.withCreationDateBefore(OffsetDateTime.now().plusSeconds(3));
        performDefaultPost(path, parameters, customizer, "error");
    }

}
