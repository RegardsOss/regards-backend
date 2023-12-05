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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IDatasetTaskRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.OrderLabelErrorEnum;
import fr.cnes.regards.modules.order.service.job.parameters.FilesJobParameter;
import fr.cnes.regards.modules.order.test.OrderTestUtils;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.*;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext
public class OrderServiceIT {

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

    private static final String USER_EMAIL = "leo.mieulet@margoulin.com";

    private static final String PRODUCT_ID = "productId";

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderMaintenanceService orderMaintenanceService;

    @Autowired
    private IOrderRepository orderRepos;

    @Autowired
    private IOrderDataFileService orderDataFileService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private IOrderDataFileRepository dataFileRepos;

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ThreadPoolTaskExecutor pool;

    @Autowired
    private IDatasetTaskRepository datasetTaskRepository;

    @MockBean
    private IProjectUsersClient projectUsersClient;

    @MockBean
    private IEmailClient emailClient;

    @Before
    public void init() {
        clean();

        eventPublisher.publishEvent(new ApplicationStartedEvent(Mockito.mock(SpringApplication.class),
                                                                null,
                                                                null,
                                                                null));

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(project), HttpStatus.OK));
        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(projectUser), HttpStatus.OK));
    }

    public void clean() {
        basketRepos.deleteAll();
        datasetTaskRepository.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();
        jobInfoRepos.deleteAll();
    }

    @Purpose("Check that the admin users can access to any order, and others can only access to their own orders.")
    @Test
    public void test_hasCurrentUserAccessTo() {
        String fakeOwner = "FAKE_OWNER";
        String realOwner = "REAL_OWNER";
        Mockito.reset(authResolver);
        Order order = new Order();
        order.setOwner(realOwner);
        order.setCreationDate(OffsetDateTime.now());
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        // Nominal : user INSTANCE_ADMIN + not order owner
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.INSTANCE_ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn(fakeOwner);
        Assertions.assertTrue(orderService.hasCurrentUserAccessTo(order.getOwner()));

        // Nominal : user PROJECT_ADMIN + not order owner
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PROJECT_ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn(fakeOwner);
        Assertions.assertTrue(orderService.hasCurrentUserAccessTo(order.getOwner()));

        // Error : user not admin + not order owner
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.ADMIN.toString());
        Mockito.when(authResolver.getUser()).thenReturn(fakeOwner);
        Assertions.assertFalse(orderService.hasCurrentUserAccessTo(order.getOwner()));

        // Error : user public + not order owner
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PUBLIC.toString());
        Mockito.when(authResolver.getUser()).thenReturn(fakeOwner);
        Assertions.assertFalse(orderService.hasCurrentUserAccessTo(order.getOwner()));

        // Nominal : user public + real order owner
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PUBLIC.toString());
        Mockito.when(authResolver.getUser()).thenReturn(realOwner);
        Assertions.assertTrue(orderService.hasCurrentUserAccessTo(order.getOwner()));
    }

    @Test
    public void testSearchOrders() {
        Order order = new Order();
        order.setOwner(USER_EMAIL);
        order.setLabel("test order 1");
        order.setStatus(OrderStatus.DONE);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(OffsetDateTime.now().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        orderRepos.save(order);

        PageRequest pr = PageRequest.of(0, 100);
        SearchRequestParameters srp = new SearchRequestParameters();

        srp.withOwner("test");
        Page<Order> ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching wrong owner", 0, ordersInRepo.getTotalElements());

        srp.withOwner(USER_EMAIL);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching correct owner", 1, ordersInRepo.getTotalElements());

        srp.withWaitingForUser(true);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching waiting for user download", 0, ordersInRepo.getTotalElements());

        srp.withWaitingForUser(false);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching not waiting for user download", 1, ordersInRepo.getTotalElements());

        srp.withStatusesIncluded(OrderStatus.DONE);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching DONE orders", 1, ordersInRepo.getTotalElements());

        srp.withStatusesIncluded(OrderStatus.PENDING);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching PENDING orders", 0, ordersInRepo.getTotalElements());

        srp.setStatuses(null);
        OffsetDateTime createdBefore = OffsetDateTime.now().plusDays(2);
        srp.withCreationDateBefore(createdBefore);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching orders before", 1, ordersInRepo.getTotalElements());

        OffsetDateTime createdAfter = OffsetDateTime.now().plusDays(1);
        srp.withCreateDateBeforeAndAfter(createdBefore, createdAfter);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching orders after", 0, ordersInRepo.getTotalElements());

        createdAfter = OffsetDateTime.now().minusDays(1);
        srp.withCreateDateBeforeAndAfter(createdBefore, createdAfter);
        ordersInRepo = orderService.searchOrders(srp, pr);
        Assert.assertEquals("Error searching orders between dates", 1, ordersInRepo.getTotalElements());
    }

    @Test
    public void testCreateOKLabelProvided() throws Exception {
        Basket basket = OrderTestUtils.getBasketSingleSelection("testCreateOKLabelProvided");
        basket = basketRepos.save(basket);
        Order order = orderService.createOrder(basket, "myCommand", "http://perdu.com", 240);
        Assert.assertNotNull(order);
        Assert.assertEquals("myCommand", order.getLabel());
        boolean awaitTermination = pool.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS);
        Assert.assertFalse(awaitTermination);
    }

    @Test
    public void testCreateOKLabelGen() throws Exception {
        Basket basket = OrderTestUtils.getBasketSingleSelection("testCreateOKLabelGen");
        basket = basketRepos.save(basket);
        Order order = orderService.createOrder(basket, null, "http://perdu.com", 240);
        boolean awaitTermination = pool.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS);
        Assert.assertFalse(awaitTermination);
        Assert.assertTrue("Label should be generated using current date (up to second)",
                          Pattern.matches("Order of \\d{4}/\\d{2}/\\d{2} at \\d{2}:\\d{2}:\\d{2}", order.getLabel()));
    }

    @Test
    public void testCreateNOKLabelTooLong() {
        Basket basket = OrderTestUtils.getBasketSingleSelection("testCreateNOKLabelTooLong");
        basket = basketRepos.save(basket);
        try {
            orderService.createOrder(basket,
                                     "this-label-has-too-many-characters-if-we-append(51)",
                                     "http://perdu.com",
                                     240);
            Assert.fail("An exception should have been thrown as label is too long");
        } catch (EntityInvalidException e) {
            Assert.assertEquals("Exception message should hold the right enumerated reason",
                                e.getMessages().get(0),
                                OrderLabelErrorEnum.TOO_MANY_CHARACTERS_IN_LABEL.toString());
        }
    }

    @Test
    public void testCreateNOKLabelAlreadyUsed() throws Exception {
        Basket basket = OrderTestUtils.getBasketSingleSelection("testCreateNOKLabelAlreadyUsed");
        basket = basketRepos.save(basket);
        orderService.createOrder(basket, "myCommand", "http://perdu.com", 240);
        try {
            // create a second time with same label: label should be already used by that owner
            orderService.createOrder(basket, "myCommand", "http://perdu2.com", 240);
            Assert.fail("An exception should have been thrown as label is too long");
        } catch (EntityInvalidException e) {
            Assert.assertEquals("Exception message should hold the right enumerated reason",
                                e.getMessages().get(0),
                                OrderLabelErrorEnum.LABEL_NOT_UNIQUE_FOR_OWNER.toString());
        }
    }

    @Test
    public void testMapping() throws URISyntaxException {
        Order order = new Order();
        order.setOwner(USER_EMAIL);
        order.setLabel("ds1 order");
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(OffsetDateTime.now().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        order = orderRepos.save(order);

        // Dataset order tasks
        DatasetTask ds1OrderTask = new DatasetTask();
        ds1OrderTask.setDatasetIpid(DS1_IP_ID.toString());
        ds1OrderTask.setDatasetLabel("DS1");
        ds1OrderTask.setFilesCount(1);
        ds1OrderTask.setFilesSize(1_000_000L);
        ds1OrderTask.setObjectsCount(1);
        ds1OrderTask.addSelectionRequest(OrderTestUtils.createBasketSelectionRequest("all:ds1"));
        order.addDatasetOrderTask(ds1OrderTask);

        // DS1 files sub order tasks
        FilesTask ds1SubOrder1Task = new FilesTask();
        ds1SubOrder1Task.setOwner(USER_EMAIL);
        DataFile dataFile1 = new DataFile();
        dataFile1.setUri(new URI("staff://toto/titi/tutu").toString());
        dataFile1.setDataType(DataType.RAWDATA);
        dataFile1.setMimeType(MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM.toString()));
        dataFile1.setOnline(true);
        dataFile1.setFilesize(1_000_000L);
        dataFile1.setFilename("tutu");
        dataFile1.setReference(false);
        OrderDataFile df1 = new OrderDataFile(dataFile1, DO1_IP_ID, order.getId(), PRODUCT_ID, 1);
        // dataFile is ONLINE, its state will be AVAILABLE after asking Storage
        df1.setState(FileState.AVAILABLE);

        dataFileRepos.save(df1);
        ds1SubOrder1Task.addFile(df1);

        DataFile dataFile2 = new DataFile();
        dataFile2.setUri(new URI("staff://toto2/titi2/tutu2").toString());
        dataFile2.setOnline(false);
        dataFile2.setFilesize(1L);
        dataFile2.setFilename("tutu2");
        dataFile2.setReference(false);
        dataFile2.setMimeType(MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM.toString()));
        dataFile2.setDataType(DataType.RAWDATA);
        OrderDataFile df2 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId(), PRODUCT_ID, 1);
        dataFileRepos.save(df2);
        ds1SubOrder1Task.addFile(df2);

        JobInfo storageJobInfo = new JobInfo(false);
        storageJobInfo.setClassName("...");
        storageJobInfo.setOwner(USER_EMAIL);
        storageJobInfo.setPriority(1);
        storageJobInfo.updateStatus(JobStatus.PENDING);

        OrderDataFile df3 = new OrderDataFile(dataFile1, DO1_IP_ID, order.getId(), PRODUCT_ID, 1);
        OrderDataFile df4 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId(), PRODUCT_ID, 1);

        storageJobInfo.setParameters(new FilesJobParameter(new Long[] { df3.getId(), df4.getId() }));

        storageJobInfo = jobInfoRepos.save(storageJobInfo);

        ds1SubOrder1Task.setJobInfo(storageJobInfo);

        ds1OrderTask.addReliantTask(ds1SubOrder1Task);

        order = orderRepos.save(order);

        List<OrderDataFile> dataFiles = dataFileRepos.findAllAvailables(order.getId());
        Assert.assertEquals(1, dataFiles.size());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_CMD_050")
    @Requirement("REGARDS_DSL_STO_CMD_050")
    @Requirement("REGARDS_DSL_STO_ARC_470")
    @Requirement("REGARDS_DSL_STO_ARC_490")
    public void testBucketsJobs() throws InterruptedException, EntityInvalidException {

        Basket basket = OrderTestUtils.getBasketSingleSelection("testBucketsJobs");
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "perdu", "http://perdu.com", 240);
        Thread.sleep(5_000);
        List<JobInfo> jobInfos = jobInfoRepo.findAllByStatusStatus(JobStatus.QUEUED);
        Assert.assertEquals(2, jobInfos.size());

        List<OrderDataFile> files = dataFileRepos.findAllAvailables(order.getId());
        Assert.assertEquals(0, files.size());

        jobInfos.forEach(j -> {
            try {
                JobInfo ji = jobInfoRepo.findCompleteById(j.getId());
                jobService.runJob(ji, "ORDER").get();
                tenantResolver.forceTenant("ORDER");
            } catch (InterruptedException | ExecutionException e) {
                tenantResolver.forceTenant("ORDER");
                Assert.fail(e.getMessage());
            }
        });

        // Some files are in error
        files = dataFileRepos.findAllAvailables(order.getId());
        int firstAvailables = files.size();

        // Download all available files
        files.forEach(f -> f.setState(FileState.DOWNLOADED));
        orderDataFileService.save(files);
        // Act as true downloads
        orderJobService.manageUserOrderStorageFilesJobInfos(basket.getOwner());
        // Re-wait a while to permit execution of last jobInfo
        Thread.sleep(10_000);

        files = dataFileRepos.findAllAvailables(order.getId());
        order = orderService.loadSimple(order.getId());
        // Error file count on order should be the same as total files - available files
        Assert.assertEquals(12 - files.size() - firstAvailables, order.getFilesInErrorCount());
        // But order should be at 100 % ever
        Assert.assertEquals(100, order.getPercentCompleted());
        Assert.assertEquals(OrderStatus.DONE, order.getStatus());
    }

    @Test
    @Ignore
    public void testExpiredOrders() throws EntityInvalidException {

        Basket basket = OrderTestUtils.getBasketSingleSelection("testExpiredOrders");
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "perdu", "http://perdu.com", 240);
        order.setExpirationDate(OffsetDateTime.now().minus(1, ChronoUnit.DAYS));
        orderRepos.save(order);

        orderMaintenanceService.cleanExpiredOrders();

        // No files should remain
        List<OrderDataFile> files = dataFileRepos.findAllAvailables(order.getId());
        Assert.assertEquals(0, files.size());
        order = orderService.loadSimple(order.getId());
        Assert.assertEquals(order.getStatus(), OrderStatus.EXPIRED);
    }

    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Requirement("REGARDS_DSL_CMD_ARC_520")
    @Requirement("REGARDS_DSL_CMD_ARC_530")
    @Test
    public void testEmailNotifications() {

        // Create an order with no available files count and no availableUpdateDate (null)
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setOwner(USER_EMAIL);
        order.setLabel("Ego");
        order.setStatus(OrderStatus.PENDING);
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        order = orderRepos.save(order);

        orderMaintenanceService.sendPeriodicNotifications();
        // No mail should have been sent (order hasn't even been started)
        Mockito.verifyNoInteractions(emailClient);

        // Update available files count, availableUpdateDate is updated as well
        order.setAvailableFilesCount(10);
        orderRepos.save(order);

        orderMaintenanceService.sendPeriodicNotifications();
        // No mail should have been sent (it must have more than 3 days between last available update date and now)
        Mockito.verifyNoInteractions(emailClient);

        // Change available update date (-4 days)
        order.setAvailableUpdateDate(OffsetDateTime.now().minus(4, ChronoUnit.DAYS));
        order.setStatus(OrderStatus.DONE);
        orderRepos.save(order);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
        ArgumentCaptor<SimpleMailMessage> messageArgumentCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.when(emailClient.sendEmail(any(), any(), any(), any())).thenCallRealMethod();
        Mockito.when(emailClient.sendEmail(messageArgumentCaptor.capture()))
               .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        OffsetDateTime start = OffsetDateTime.now();
        orderMaintenanceService.sendPeriodicNotifications();

        // A message should have been sent
        SimpleMailMessage message = messageArgumentCaptor.getValue();
        Assertions.assertNotNull(message);
        Assertions.assertNotNull(message.getText());
        Assertions.assertNotNull(message.getTo());
        Assertions.assertEquals(1, message.getTo().length);
        Assertions.assertEquals(order.getOwner(), message.getTo()[0]);
        Assertions.assertTrue(message.getText().contains(sdf.format(Date.from(order.getExpirationDate().toInstant()))));
        Assertions.assertTrue(message.getText().contains(sdf.format(Date.from(order.getCreationDate().toInstant()))));
        // AvailableUpdateDate should have been reset
        Assertions.assertTrue(orderRepos.findCompleteById(order.getId()).getAvailableUpdateDate().isAfter(start));
    }
}
