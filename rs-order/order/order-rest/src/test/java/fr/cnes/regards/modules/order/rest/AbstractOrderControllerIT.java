/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import com.google.common.reflect.TypeToken;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
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
import fr.cnes.regards.modules.order.dto.dto.BasketSelectionRequest;
import fr.cnes.regards.modules.order.dto.dto.OrderDto;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import fr.cnes.regards.modules.search.dto.ComplexSearchRequest;
import org.junit.Before;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

/**
 * Abstract to test OrderController
 *
 * @author Iliana Ghazali
 **/
@ContextConfiguration(classes = OrderConfiguration.class)
public abstract class AbstractOrderControllerIT extends AbstractRegardsIT {

    public static final UniformResourceName DS1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DS3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO4_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO5_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    @Autowired
    protected IBasketRepository basketRepository;

    @Autowired
    protected IOrderRepository orderRepository;

    @Autowired
    protected IOrderDataFileRepository dataFileRepository;

    @Autowired
    protected IProjectsClient projectsClient;

    @Autowired
    protected IAuthenticationResolver authResolver;

    @Autowired
    protected IComplexSearchClient searchClient;

    @MockBean
    protected IProjectUsersClient projectUsersClient;

    protected String projectAdminToken;

    protected String projectUserToken;

    protected final String adminEmail = "admin@regards.fr";

    @Before
    public void init() {

        tenantResolver.forceTenant(getDefaultTenant());

        basketRepository.deleteAll();
        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        OrderConfiguration.resetMock(searchClient);

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(ArgumentMatchers.anyString()))
               .thenReturn(ResponseEntity.ok(EntityModel.of(project)));
        authResolver = Mockito.spy(authResolver);
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());

        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.isAdmin(getDefaultUserEmail())).thenReturn(ResponseEntity.ok(false));
        Mockito.when(projectUsersClient.isAdmin(adminEmail)).thenReturn(ResponseEntity.ok(true));
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(projectUser), HttpStatus.OK));

        JWTService service = new JWTService();
        service.setSecret("!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!");
        projectAdminToken = service.generateToken(getDefaultTenant(), adminEmail, DefaultRole.PROJECT_ADMIN.toString());
        projectUserToken = service.generateToken(getDefaultTenant(),
                                                 getDefaultUserEmail(),
                                                 DefaultRole.REGISTERED_USER.toString());
    }

    protected void initForNextOrder() throws URISyntaxException {
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
        EntityFeature feat1 = new DataObjectFeature(UniformResourceName.fromString("URN:AIP:DATA:"
                                                                                   + getDefaultTenant()
                                                                                   + ":"
                                                                                   + UUID.randomUUID()
                                                                                   + ":V1"), "Feature1", "Feature 1");
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

        EntityFeature feat2 = new DataObjectFeature(UniformResourceName.fromString("URN:AIP:DATA:"
                                                                                   + getDefaultTenant()
                                                                                   + ":"
                                                                                   + UUID.randomUUID()
                                                                                   + ":V3"), "Feature2", "Feature 2");
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
            return new ResponseEntity<>(new FacettedPagedModel<>(new HashSet<>(),
                                                                 page == 0 ?
                                                                     Lists.newArrayList(EntityModel.of(feat1),
                                                                                        EntityModel.of(feat2)) :
                                                                     Collections.emptyList(),
                                                                 new PagedModel.PageMetadata(page == 0 ? 2 : 0,
                                                                                             page,
                                                                                             2,
                                                                                             1)),
                                        page == 0 ? HttpStatus.OK : HttpStatus.NO_CONTENT);
        });
    }

    protected void clearForPreviousOrder() {
        basketRepository.deleteAll();
        Mockito.reset(searchClient);
    }

    protected Order createOrderAsRunning() throws URISyntaxException {
        Order order = createOrderAsPending();

        order.setStatus(OrderStatus.RUNNING);
        order.setLabel("order1");
        order.setPercentCompleted(23);
        order.setAvailableFilesCount(2);

        order = orderRepository.save(order);
        return order;
    }

    protected Long createOrderAs(OrderStatus status) throws URISyntaxException, InterruptedException {
        Order order = createOrderAsRunning();
        order.setStatus(status);
        order = orderRepository.save(order);
        // Waiting for maintenance to update order properties
        TimeUnit.SECONDS.sleep(2);
        return order.getId();
    }

    protected Order createOrderAsPending() throws URISyntaxException {
        Order order = new Order();
        order.setOwner(getDefaultUserEmail());
        order.setCreationDate(OffsetDateTime.now());
        order.setLabel("order2");
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
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

    protected OrderDataFile createOrderDataFile(Order order,
                                                UniformResourceName aipId,
                                                String filename,
                                                FileState state) throws URISyntaxException {
        return createOrderDataFile(order, aipId, filename, state, false);
    }

    protected OrderDataFile createOrderDataFile(Order order,
                                                UniformResourceName aipId,
                                                String filename,
                                                FileState state,
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

    protected EntityModel<OrderDto> getOrderDtoEntityModel(Long orderId, String token) throws InterruptedException {
        // Seems to fail with no pause here
        TimeUnit.SECONDS.sleep(5);
        String payload = payload(performGet(OrderController.GET_ORDER_PATH,
                                            token,
                                            customizer().expectStatusOk(),
                                            "error",
                                            orderId));
        return GsonUtil.fromString(payload, new TypeToken<EntityModel<OrderDto>>() {

        }.getType());
    }

}
