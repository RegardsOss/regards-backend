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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.TypeToken;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.dto.OrderDto;
import fr.cnes.regards.modules.order.domain.exception.OrderLabelErrorEnum;
import fr.cnes.regards.modules.order.metalink.schema.FileType;
import fr.cnes.regards.modules.order.metalink.schema.MetalinkType;
import fr.cnes.regards.modules.order.metalink.schema.ObjectFactory;
import fr.cnes.regards.modules.order.metalink.schema.ResourcesType;
import fr.cnes.regards.modules.order.rest.mock.StorageClientMock;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import org.hamcrest.text.MatchesPattern;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.LinkedMultiValueMap;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@TestPropertySource(properties = {"regards.tenant=order1", "spring.jpa.properties.hibernate.default_schema=order1"})
public class OrderControllerIT extends AbstractRegardsIT {

    public static final UniformResourceName DS1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DS2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DS3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DO2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DO3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DO4_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA, "ORDER", UUID.randomUUID(), 1);
    public static final UniformResourceName DO5_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA, "ORDER", UUID.randomUUID(), 1);

    @Autowired
    private IRuntimeTenantResolver tenantResolver;
    @Autowired
    private IBasketRepository basketRepository;
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IOrderDataFileRepository dataFileRepository;
    @Autowired
    private IProjectsClient projectsClient;
    @Autowired
    private IAuthenticationResolver authResolver;
    @Autowired
    private IComplexSearchClient searchClient;

    @MockBean
    private IProjectUsersClient projectUsersClient;

    private String projectAdminToken;
    private String projectUserToken;
    private String adminEmail = "admin@regards.fr";

    @Before
    public void init() {

        tenantResolver.forceTenant(getDefaultTenant());

        basketRepository.deleteAll();
        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(ArgumentMatchers.anyString())).thenReturn(ResponseEntity.ok(new EntityModel<>(project)));
        authResolver = Mockito.spy(authResolver);
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());

        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.isAdmin(getDefaultUserEmail())).thenReturn(ResponseEntity.ok(false));
        Mockito.when(projectUsersClient.isAdmin(adminEmail)).thenReturn(ResponseEntity.ok(true));
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(new ResponseEntity<>(new EntityModel<>(projectUser), HttpStatus.OK));

        JWTService service = new JWTService();
        service.setSecret("!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!");
        projectAdminToken = service.generateToken(getDefaultTenant(), adminEmail, DefaultRole.PROJECT_ADMIN.toString());
        projectUserToken = service.generateToken(getDefaultTenant(), getDefaultUserEmail(), DefaultRole.REGISTERED_USER.toString());
    }

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
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer().expectStatusOk(), "error", order.getId());
    }

    @Test
    public void testPauseFailed() throws URISyntaxException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer().expectStatusOk(), "error", order.getId());

        // Re-pause Order => fail
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer().expectStatus(HttpStatus.UNAUTHORIZED), "error", order.getId());
    }

    @Test
    public void testResume() throws URISyntaxException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer().expectStatusOk(), "error", order.getId());

        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH, null, customizer().expectStatusOk(), "error", order.getId());
    }

    @Test
    public void testResumeFailed() throws URISyntaxException {
        Order order = createOrderAsPending();
        // Pause Order
        performDefaultPut(OrderController.RESUME_ORDER_PATH, null, customizer().expectStatus(HttpStatus.UNAUTHORIZED), "error", order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testDelete() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsPending();

        // Pause Order
        performDefaultPut(OrderController.PAUSE_ORDER_PATH, null, customizer().expectStatusOk(), "error", order.getId());

        Thread.sleep(1000);

        // Delete Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer().expectStatusOk(), "error", order.getId());
    }

    @Test
    public void testDeleteFailed() throws URISyntaxException {
        Order order = createOrderAsPending();
        // Pause Order
        performDefaultDelete(OrderController.DELETE_ORDER_PATH, customizer().expectStatus(HttpStatus.UNAUTHORIZED), "error", order.getId());
    }

    @Test
    public void testRemoveFailed() throws URISyntaxException {
        Order order = createOrderAsRunning();
        // Pause Order
        performDefaultDelete(OrderController.REMOVE_ORDER_PATH, customizer().expectStatus(HttpStatus.UNAUTHORIZED), "error", order.getId());
    }

    @Requirement("REGARDS_DSL_STO_CMD_450")
    @Test
    public void testRemove() throws URISyntaxException {
        Order order = createOrderAsPending();

        // Delete Order
        performDelete(OrderController.REMOVE_ORDER_PATH, projectAdminToken, customizer().expectStatusOk(), "error", order.getId());
    }

    /**
     * Create order parameters description)
     */
    private Snippet getCreateOrderDocumentation() {
        ConstrainedFields constrainedFields = new ConstrainedFields(OrderController.OrderRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields
                .withPath("label",
                          "command label, 1 to 50 characters, must be unique for current user (generated when not provided)")
                .optional().type(JSON_STRING_TYPE));
        fields.add(constrainedFields
                .withPath("onSuccessUrl", "url used by frontend to display a page if order creation has succeeded")
                .optional().type(JSON_STRING_TYPE));
        return PayloadDocumentation.relaxedRequestFields(fields);
    }

    private void initForNextOrder() throws URISyntaxException {
        BasketSelectionRequest selectionRequest = new BasketSelectionRequest();
        selectionRequest.setDatasetUrn("URN:DATASET:EXAMPLE-DATASET:V1");
        selectionRequest.setEngineType("legacy");
        selectionRequest.setEntityIdsToExclude(new HashSet<>());
        selectionRequest.setEntityIdsToInclude(null);
        selectionRequest.setSearchParameters(new LinkedMultiValueMap<>());

        BasketDatedItemsSelection bDIS = new BasketDatedItemsSelection();
        bDIS.setDate(OffsetDateTime.now());
        bDIS.setObjectsCount(2);
        bDIS.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        bDIS.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        bDIS.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", 2L);
        bDIS.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", 45050L);
        bDIS.setFileTypeCount(DataType.RAWDATA.name(), 2L);
        bDIS.setFileTypeSize(DataType.RAWDATA.name(), 45050L);
        bDIS.setSelectionRequest(selectionRequest);

        BasketDatasetSelection bDS = new BasketDatasetSelection();
        bDS.setDatasetIpid("URN:DATASET:EXAMPLE-DATASET:V1");
        bDS.setDatasetLabel("example dataset");
        bDS.setObjectsCount(2);
        bDS.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        bDS.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        bDS.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", 2L);
        bDS.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", 45050L);
        bDS.setFileTypeCount(DataType.RAWDATA.name(), 2L);
        bDS.setFileTypeSize(DataType.RAWDATA.name(), 45050L);
        bDS.addItemsSelection(bDIS);

        Basket b = new Basket();
        b.setOwner(getDefaultUserEmail());
        b.addDatasetSelection(bDS);
        basketRepository.save(b);

        // required mock on search: return the 2 entities
        EntityFeature feat1 = new DataObjectFeature(
                UniformResourceName.fromString("URN:AIP:DATA:" + getDefaultTenant() + ":" + UUID.randomUUID() + ":V1"),
                "Feature1", "Feature 1");
        Multimap<DataType, DataFile> fileMultimapF1 = ArrayListMultimap.create();
        DataFile feat1File1 = new DataFile();
        feat1File1.setOnline(true);
        feat1File1.setUri(new URI("file:///test/feat1_file1.txt").toString());
        feat1File1.setFilename("feat1_file1");
        feat1File1.setFilesize(42000L);
        feat1File1.setReference(false);
        feat1File1.setChecksum("feat1_file1");
        feat1File1.setDigestAlgorithm("MD5");
        feat1File1.setMimeType(MediaType.TEXT_PLAIN);
        feat1File1.setDataType(DataType.RAWDATA);
        fileMultimapF1.put(DataType.RAWDATA, feat1File1);

        EntityFeature feat2 = new DataObjectFeature(
                UniformResourceName.fromString("URN:AIP:DATA:" + getDefaultTenant() + ":" + UUID.randomUUID() + ":V3"),
                "Feature2", "Feature 2");
        Multimap<DataType, DataFile> fileMultimapF2 = ArrayListMultimap.create();
        DataFile feat2File2 = new DataFile();
        feat2File2.setOnline(true);
        feat2File2.setUri(new URI("file:///test/feat2_file2.txt").toString());
        feat2File2.setFilename("feat2_file2");
        feat2File2.setFilesize(3050L);
        feat2File2.setReference(false);
        feat2File2.setChecksum("feat2_file2");
        feat2File2.setDigestAlgorithm("MD5");
        feat2File2.setMimeType(MediaType.TEXT_PLAIN);
        feat2File2.setDataType(DataType.RAWDATA);
        fileMultimapF2.put(DataType.RAWDATA, feat1File1);
        feat2.setFiles(fileMultimapF2);

        Mockito.when(searchClient.searchDataObjects(Mockito.any())).thenAnswer(invocationOnMock -> {
            ComplexSearchRequest cSR = invocationOnMock.getArgument(0, ComplexSearchRequest.class);
            int page = cSR == null ? 0 : cSR.getPage();
            return new ResponseEntity<>(
                    new FacettedPagedModel<>(new HashSet<>(),
                            page == 0 ? Lists.newArrayList(new EntityModel<>(feat1), new EntityModel<>(feat2))
                                    : Collections.emptyList(),
                            new PagedModel.PageMetadata(page == 0 ? 2 : 0, page, 2, 1)),
                    page == 0 ? HttpStatus.OK : HttpStatus.NO_CONTENT);
        });
    }

    private void clearForPreviousOrder() {
        basketRepository.deleteAll();
        Mockito.reset(searchClient);
    }

    @Test
    public void testCreateOKSimpleLabel() throws URISyntaxException, InterruptedException {
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // Expectations
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.CREATED)
                .expectValue("content.owner", getDefaultUserEmail()).expectValue("content.label", "myCommand");
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest("myCommand", "http://perdu.com"), customizer, "error");
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
                .expect(MockMvcResultMatchers.jsonPath("content.label", MatchesPattern
                        .matchesPattern("Order of \\d{4}/\\d{2}/\\d{2} at \\d{2}:\\d{2}:\\d{2}"))); // value should match generated pattern
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest(null, "http://perdu.com"), customizer, "error");
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
                .expectValue("content.owner", getDefaultUserEmail()).expectValue("content.label", "myDoubleCommand");
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myDoubleCommand", "http://perdu.com"), customizer,
                           "error");
        // Second request expectations: NOK (label already used by a command for that user)
        RequestBuilderCustomizer customizer2 = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectValue("messages[0]", OrderLabelErrorEnum.LABEL_NOT_UNIQUE_FOR_OWNER.toString());
        // Send second request
        performDefaultPost(OrderController.USER_ROOT_PATH,
                           new OrderController.OrderRequest("myDoubleCommand", "http://perdu2.com"), customizer2,
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
                .expectValue("messages[0]", OrderLabelErrorEnum.TOO_MANY_CHARACTERS_IN_LABEL.toString());
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // Send
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest(
                "this-label-has-too-many-characters-if-we-append(51)", "http://perdu.com"), customizer, "error");
        // After: clear
        clearForPreviousOrder();
    }

    @Test
    public void testCreateNOKNoBasket() {
        RequestBuilderCustomizer customizer = customizer().expectStatusNoContent();
        // Add doc
        customizer.document(getCreateOrderDocumentation());
        // No basket available
        performDefaultPost(OrderController.USER_ROOT_PATH, new OrderController.OrderRequest("myCommand", "http://perdu.com"), customizer, "error");
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
                                                        customizer().expectStatusOk(), "Should return result",
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
        Assert.assertTrue(resultFileMl.length() > 8000l); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

    }

    @Test
    public void testPublicDownloadMetalinkFile() throws IOException, URISyntaxException, JAXBException {
        // Create order
        Order order = createOrderAsRunning();

        // Download metalink file
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(), "Should return result",
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
        String token = urlParts[4].substring(urlParts[4].indexOf('=') + 1, urlParts[4].indexOf('&'));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addParameter("orderToken", token);
        // request parameters
        customizer.document(RequestDocumentation
                .relaxedRequestParameters(RequestDocumentation.parameterWithName("orderToken").optional()
                        .description("token generated at order creation and sent by email to user.")
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"))));

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
    public void testDownloadZipFile() throws URISyntaxException, IOException, InterruptedException, JAXBException,
            SAXException, ParserConfigurationException {
        Order order = createOrderAsRunning();

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.ZIP_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(), "Should return result",
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
        Assert.assertEquals(1816l, resultFile.length());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderControllerIT.class);

    @Test
    public void testDownloadZipFile_contains_notice_when_inner_downloads_fail() throws URISyntaxException, IOException,
            InterruptedException, JAXBException, SAXException, ParserConfigurationException {
        Order order = createOrderAsRunning();

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.ZIP_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(), "Should return result",
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
        Assert.assertTrue(failures.stream().findFirst().get()
                .matches(String.format("Failed to download file \\(.*\\): %s.", StorageClientMock.NO_QUOTA_MSG_STUB)));
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
        customizer.document(RequestDocumentation.relaxedRequestParameters(RequestDocumentation.parameterWithName("page")
                .optional().description("page number (from 0)").attributes(Attributes
                        .key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")), RequestDocumentation
                                .parameterWithName("size").optional().description("page size")
                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
        customizer.document(RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("datasetId")
                .description("dataset task id (from order)").attributes(Attributes
                        .key(RequestBuilderCustomizer.PARAM_TYPE).value("Long")), RequestDocumentation
                                .parameterWithName("orderId").description("order id")
                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Long"))));

        ConstrainedFields constrainedFields = new ConstrainedFields(OrderDataFile.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "files").optional().type(JSON_ARRAY_TYPE));
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultGet(OrderControllerEndpointConfiguration.ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES, customizer,
                          "error", order.getId(), order.getDatasetTasks().first().getId());

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
    public void testDownloadFile() throws URISyntaxException, IOException, InterruptedException, JAXBException,
            SAXException, ParserConfigurationException {
        Order order = createOrderAsRunning();

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(), "Should return result",
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
        Assert.assertTrue(resultFileMl.length() > 8000l); // 14 files listed into metalink file (size is
        // slightely different into jenkins)

        //////////////////////////////////
        // Then download order Zip file //
        //////////////////////////////////
        resultActions = performDefaultGet("/user/orders/{orderId}/download", customizer().expectStatusOk(),
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

            customizer.document(RequestDocumentation
                    .relaxedRequestParameters(RequestDocumentation.parameterWithName("orderToken").optional()
                            .description("token generated at order creation and sent by email to user.")
                            .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"))));

            customizer.document(RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("aipId")
                    .description("IP_ID of data object of which file belongs to").attributes(Attributes
                            .key(RequestBuilderCustomizer.PARAM_TYPE).value("String")), RequestDocumentation
                                    .parameterWithName("dataFileId").description("file id ")
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Long"))));

            // Try downloading file as if, with token given into public file url
            ResultActions results = performDefaultGet(OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID,
                                                      customizer, "Should return result", dataFileId);

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

        ////////////////////////////////////////
        // First Download metalink order file //
        ////////////////////////////////////////
        ResultActions resultActions = performDefaultGet(OrderController.METALINK_DOWNLOAD_PATH,
                                                        customizer().expectStatusOk(), "Should return result",
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
        Assert.assertTrue(resultFileMl.length() > 8000l); // 14 files listed into metalink file (size is
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
            long dataFileId = Long.parseLong(urlParts[4].substring(0, urlParts[4].indexOf('?')));
            // Stop at "scope=PROJECT"
            String token = urlParts[4].substring(urlParts[4].indexOf('=') + 1, urlParts[4].indexOf('&'));

            // File file3.txt has a status PENDING...
            if (dataFileId == fileText3TxtId) {
                lastDataFileId = dataFileId;
                lastDataFileAipId = URLDecoder.decode(aipId, "UTF-8");
                lastDataFileToken = token;
                break;
            }
        }
        // Attempt to download not yet available data file
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.ACCEPTED);
        customizer.addParameter("orderToken", lastDataFileToken);
        performDefaultGet(OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID, customizer,
                          "Should return result", lastDataFileId);
    }

    private Order createOrderAsRunning() throws URISyntaxException {
        Order order = createOrderAsPending();

        order.setStatus(OrderStatus.RUNNING);
        order.setLabel("order1");
        order.setPercentCompleted(23);
        order.setAvailableFilesCount(2);

        order = orderRepository.save(order);
        return order;
    }

    private Order createOrderAsPending() throws URISyntaxException {
        Order order = new Order();
        order.setOwner(getDefaultUserEmail());
        order.setCreationDate(OffsetDateTime.now());
        order.setLabel("order2");
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
    public void testFindAll() {
        createSeveralOrdersWithDifferentOwners();

        // All orders
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(3)));
        performDefaultGet(OrderController.ADMIN_ROOT_PATH, customizer, "errors");
    }

    @Requirement("REGARDS_DSL_STO_CMD_420")
    @Test
    public void testFindAllSpecificUser() throws UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        // All specific user orders
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)));
        customizer.addParameter("user", "other.user2@regards.fr");
        customizer.addParameter("page", "0");
        customizer.addParameter("size", "20");
        // request parameters
        customizer.document(RequestDocumentation
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
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));
        performDefaultGet(OrderController.ADMIN_ROOT_PATH, customizer, "errors");
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
        customizer.document(RequestDocumentation.relaxedRequestParameters(RequestDocumentation.parameterWithName("page")
                .optional().description("page number (from 0)").attributes(Attributes
                        .key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer")), RequestDocumentation
                                .parameterWithName("size").optional().description("page size")
                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Integer"))));
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
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOwner(getDefaultUserEmail());
        order2.setLabel("order2");
        order2.setCreationDate(OffsetDateTime.now());
        order2.setExpirationDate(order2.getCreationDate().plus(3, ChronoUnit.DAYS));

        orderRepository.save(order2);

        Order order3 = new Order();
        order3.setOwner("other.user2@regards.fr");
        order3.setLabel("order3");
        order3.setCreationDate(OffsetDateTime.now());
        order3.setExpirationDate(order3.getCreationDate().plus(3, ChronoUnit.DAYS));
        orderRepository.save(order3);
    }

    @Test
    public void testCsv() throws URISyntaxException, UnsupportedEncodingException {
        createSeveralOrdersWithDifferentOwners();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                .addHeader(HttpConstants.CONTENT_TYPE, "application/json").addHeader(HttpConstants.ACCEPT, "text/csv");

        ResultActions results = performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV, customizer,
                                                  "error");
        // Just test headers are present and CSV format is ok
        Assert.assertTrue(results.andReturn().getResponse().getContentAsString()
                .startsWith("ORDER_ID;CREATION_DATE;EXPIRATION_DATE"));
        // now let check that optional parameter are correctly parsed
        // First status
        performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV,
                          customizer.addParameter("status", OrderStatus.DONE.toString()), "error");
        // then from
        performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV,
                          customizer.addParameter("from", OffsetDateTime.now().minusHours(3).toString()), "error");
        // then to
        performDefaultGet(OrderController.ADMIN_ROOT_PATH + OrderController.CSV,
                          customizer.addParameter("to", OffsetDateTime.now().plusSeconds(3).toString()), "error");
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

    @Test
    public void testHateoasLinks_Running() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        EntityModel<OrderDto> orderDtoEntityModel = getOrderDtoAsAdmin(order.getId());
        // Order should have 3 basic links, plus PAUSE
        checkLinks(orderDtoEntityModel, "pause");
        orderDtoEntityModel = getOrderDtoAsUser(order.getId());
        // Order should have 3 basic links, plus PAUSE
        checkLinks(orderDtoEntityModel, "pause");
    }

    @Test
    public void testHateoasLinks_Paused() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        order.setStatus(OrderStatus.PAUSED);
        order = orderRepository.save(order);
        EntityModel<OrderDto> orderDtoEntityModel = getOrderDtoAsAdmin(order.getId());
        // Order should have 3 basic links, plus RESUME, DELETE, REMOVE
        checkLinks(orderDtoEntityModel, "resume", "delete", "remove");
        orderDtoEntityModel = getOrderDtoAsUser(order.getId());
        // Order should have 3 basic links, plus RESUME, DELETE
        checkLinks(orderDtoEntityModel, "resume", "delete");
    }

    @Test
    public void testHateoasLinks_Done() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        order.setStatus(OrderStatus.DONE);
        order = orderRepository.save(order);
        EntityModel<OrderDto> orderDtoEntityModel = getOrderDtoAsAdmin(order.getId());
        // Order should have 3 basic links, plus RESTART, DELETE, REMOVE
        checkLinks(orderDtoEntityModel, "restart", "delete", "remove");
        orderDtoEntityModel = getOrderDtoAsUser(order.getId());
        // Order should have 3 basic links, plus RESTART, DELETE
        checkLinks(orderDtoEntityModel, "restart", "delete");
    }

    @Test
    public void testHateoasLinks_DoneWithWarning() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        order.setStatus(OrderStatus.DONE_WITH_WARNING);
        order = orderRepository.save(order);
        EntityModel<OrderDto> orderDtoEntityModel = getOrderDtoAsAdmin(order.getId());
        // Order should have 3 basic links, plus RESTART, RETRY, DELETE, REMOVE
        checkLinks(orderDtoEntityModel, "restart", "retry", "delete", "remove");
        orderDtoEntityModel = getOrderDtoAsUser(order.getId());
        // Order should have 3 basic links, plus RESTART, RETRY, DELETE
        checkLinks(orderDtoEntityModel, "restart", "retry", "delete");
    }

    @Test
    public void testHateoasLinks_Failed() throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        order.setStatus(OrderStatus.FAILED);
        order = orderRepository.save(order);
        EntityModel<OrderDto> orderDtoEntityModel = getOrderDtoAsAdmin(order.getId());
        // Order should have 3 basic links, plus RESTART, RETRY, DELETE, REMOVE
        checkLinks(orderDtoEntityModel, "restart", "retry", "delete", "remove");
        orderDtoEntityModel = getOrderDtoAsUser(order.getId());
        // Order should have 3 basic links, plus RESTART, RETRY, DELETE
        checkLinks(orderDtoEntityModel, "restart", "retry", "delete");
    }

    private EntityModel<OrderDto> getOrderDtoAsAdmin(Long orderId) throws InterruptedException {
        return getOrderDtoEntityModel(orderId, projectAdminToken);
    }

    private EntityModel<OrderDto> getOrderDtoAsUser(Long orderId) throws InterruptedException {
        return getOrderDtoEntityModel(orderId, projectUserToken);
    }

    private EntityModel<OrderDto> getOrderDtoEntityModel(Long orderId, String token) throws InterruptedException {
        // Seems to fail with no pause here
        TimeUnit.SECONDS.sleep(5);
        String payload = payload(performGet(OrderController.GET_ORDER_PATH, token, customizer().expectStatusOk(), "error", orderId));
        return GsonUtil.fromString(payload, new TypeToken<EntityModel<OrderDto>>() {
        }.getType());
    }

    private void checkLinks(EntityModel<OrderDto> entityModel, String... links) {
        // Check basic links (always there)
        Assertions.assertTrue(entityModel.getLink(LinkRels.SELF).isPresent());
        Assertions.assertTrue(entityModel.getLink(LinkRels.LIST).isPresent());
        Assertions.assertTrue(entityModel.getLink("download").isPresent());
        // Check proper number of links
        Assertions.assertTrue(entityModel.getLinks().hasSize(3 + links.length));
        // Check additional links
        Arrays.stream(links).forEach(link -> Assertions.assertTrue(entityModel.getLink(link).isPresent()));

    }

}
