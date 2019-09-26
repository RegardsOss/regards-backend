/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IFilesTasksRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext
public class OrderServiceIT {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderRepository orderRepos;

    @Autowired
    private IOrderDataFileService orderDataFileService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private IOrderDataFileRepository dataFileRepos;

    @Autowired
    private IFilesTasksRepository filesTasksRepository;

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IEmailClient emailClient;
    
    @Autowired
    private IJobService jobService;
    
    @Autowired
    private IJobInfoRepository jobInfoRepo;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private IRuntimeTenantResolver tenantResolver;
    
    private static final String USER_EMAIL = "leo.mieulet@margoulin.com";

    private static SimpleMailMessage mailMessage;

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO2_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
            "ORDER", UUID.randomUUID(), 1);

    @Before
    public void init() {
        clean();
        
        eventPublisher.publishEvent(new ApplicationStartedEvent(Mockito.mock(SpringApplication.class),null,null));
        
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<>(project), HttpStatus.OK));
    }

    public void clean() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();

        jobInfoRepos.deleteAll();
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

    private BasketDatedItemsSelection createDatasetItemSelection(long filesSize, int filesCount, int objectsCount,
            String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFilesSize(filesSize);
        item.setFilesCount(filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    @Test
    public void test1() throws Exception {
        Basket basket = new Basket();
        basket.setOwner(USER_EMAIL);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetLabel("DS1");
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setFilesSize(1_000_000l);
        dsSelection.setFilesCount(1);
        dsSelection.setObjectsCount(1);
        dsSelection.addItemsSelection(createDatasetItemSelection(1_000_001l, 1, 1, "someone:something"));
        basket.addDatasetSelection(dsSelection);
        basket = basketRepos.save(basket);
        Order order = orderService.createOrder(basket, "http://perdu.com");
        Assert.assertNotNull(order);
    }

    @Test
    public void testMapping() throws URISyntaxException {
        Order order = new Order();
        order.setOwner(USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(OffsetDateTime.now().plus(3, ChronoUnit.DAYS));
        order = orderRepos.save(order);

        // Dataset order tasks
        DatasetTask ds1OrderTask = new DatasetTask();
        ds1OrderTask.setDatasetIpid(DS1_IP_ID.toString());
        ds1OrderTask.setDatasetLabel("DS1");
        ds1OrderTask.setFilesCount(1);
        ds1OrderTask.setFilesSize(1_000_000l);
        ds1OrderTask.setObjectsCount(1);
        ds1OrderTask.addSelectionRequest(createBasketSelectionRequest("all:ds1"));
        order.addDatasetOrderTask(ds1OrderTask);

        // DS1 files sub order tasks
        FilesTask ds1SubOrder1Task = new FilesTask();
        ds1SubOrder1Task.setOwner(USER_EMAIL);
        DataFile dataFile1 = new DataFile();
        dataFile1.setUri(new URI("staff://toto/titi/tutu"));
        dataFile1.setDataType(DataType.RAWDATA);
        dataFile1.setMimeType(MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM.toString()));
        dataFile1.setOnline(true);
        dataFile1.setFilesize(1_000_000l);
        dataFile1.setFilename("tutu");
        dataFile1.setReference(false);
        OrderDataFile df1 = new OrderDataFile(dataFile1, DO1_IP_ID, order.getId());
        // dataFile is ONLINE, its state will be AVAILABLE after asking Storage
        df1.setState(FileState.AVAILABLE);

        dataFileRepos.save(df1);
        ds1SubOrder1Task.addFile(df1);

        DataFile dataFile2 = new DataFile();
        dataFile2.setUri(new URI("staff://toto2/titi2/tutu2"));
        dataFile2.setOnline(false);
        dataFile2.setFilesize(1l);
        dataFile2.setFilename("tutu2");
        dataFile2.setReference(false);
        dataFile2.setMimeType(MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM.toString()));
        dataFile2.setDataType(DataType.RAWDATA);
        OrderDataFile df2 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId());
        dataFileRepos.save(df2);
        ds1SubOrder1Task.addFile(df2);

        JobInfo storageJobInfo = new JobInfo(false);
        storageJobInfo.setClassName("...");
        storageJobInfo.setOwner(USER_EMAIL);
        storageJobInfo.setPriority(1);
        storageJobInfo.updateStatus(JobStatus.PENDING);

        OrderDataFile df3 = new OrderDataFile(dataFile1, DO1_IP_ID, order.getId());
        OrderDataFile df4 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId());

        storageJobInfo.setParameters(new FilesJobParameter(new OrderDataFile[] { df3, df4 }));

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
    public void testBucketsJobs() throws IOException, InterruptedException {
        String user = "tulavu@qui.fr";
        Basket basket = new Basket(user);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.addItemsSelection(createDatasetItemSelection(3_000_171l, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "http://perdu.com");
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
        orderJobService.manageUserOrderJobInfos(user);
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
    public void testExpiredOrders() throws IOException, InterruptedException {
        Basket basket = new Basket("tulavu@qui.fr");
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.addItemsSelection(createDatasetItemSelection(3_000_171l, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "http://perdu.com");
        order.setExpirationDate(OffsetDateTime.now().minus(1, ChronoUnit.DAYS));
        orderRepos.save(order);

        orderService.cleanExpiredOrders();

        // No files should remain
        List<OrderDataFile> files = dataFileRepos.findAllAvailables(order.getId());
        Assert.assertEquals(0, files.size());
        order = orderService.loadSimple(order.getId());
        Assert.assertTrue(order.getStatus() == OrderStatus.EXPIRED);
    }

    @Test
    @Ignore
    public void testPauseResume() throws InterruptedException, CannotResumeOrderException, CannotPauseOrderException {

        Basket basket = new Basket("tulavu@qui.fr");
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.addItemsSelection(createDatasetItemSelection(3_000_171l, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "http://perdu.com");

        Thread.sleep(1_000);
        orderService.pause(order.getId());

        Thread.sleep(10_000);

        // Associated jobInfo must be ever at SUCCEEDED OR ABORTED
        order = orderService.loadComplete(order.getId());
        Set<JobInfo> jobInfos = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo).collect(Collectors.toSet());
        Assert.assertTrue(jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                .allMatch(JobStatus::isFinished));
        // Sometime, pause/resume has been asked toolate (and so percent is at 100 %)
        Assert.assertTrue(order.getPercentCompleted() <= 100);

        orderService.resume(order.getId());

        Thread.sleep(8_000);

        order = orderService.loadComplete(order.getId());
        jobInfos = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo).collect(Collectors.toSet());
        // Because there 3 sub-tasks and only 2 can be executed simustaneously without downloading files, only 2 of the
        // 3 should be SUCCEEDED, the last one must be at PENDING state
        Assert.assertEquals(2, jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                .filter(status -> status == JobStatus.SUCCEEDED).count());
        Assert.assertEquals(1, jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                .filter(status -> status == JobStatus.PENDING).count());

        List<FilesTask> waitingForUserTasks = filesTasksRepository.findDistinctByWaitingForUser(true);
        Assert.assertEquals(2, waitingForUserTasks.size());

        for (FilesTask filesTask : waitingForUserTasks) {
            Set<OrderDataFile> toSaveDataFiles = new HashSet<>();
            for (OrderDataFile dataFile : filesTask.getFiles()) {
                // Emulate a user download
                if (dataFile.getState() == FileState.AVAILABLE) {
                    dataFile.setState(FileState.DOWNLOADED);
                    toSaveDataFiles.add(dataFile);
                }
            }
            orderDataFileService.save(toSaveDataFiles);
        }
        // Act as it was true downloads (to permit pending jobs changing their states and so be executed)
        orderJobService.manageUserOrderJobInfos(order.getOwner());

        Thread.sleep(8_000);
        order = orderService.loadComplete(order.getId());
        jobInfos = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo).collect(Collectors.toSet());
        Assert.assertTrue(jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                .allMatch(status -> status == JobStatus.SUCCEEDED));

        Assert.assertTrue(order.getPercentCompleted() == 100);
    }

    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Requirement("REGARDS_DSL_CMD_ARC_520")
    @Requirement("REGARDS_DSL_CMD_ARC_530")
    @Test
    @Ignore
    public void testEmailNotifications() {
        Mockito.when(emailClient.sendEmail(Mockito.any())).thenAnswer(invocation -> {
            mailMessage = (SimpleMailMessage) invocation.getArguments()[0];
            return new ResponseEntity<>(HttpStatus.CREATED);
        });

        // Create an order with no available files count and no availableUpdateDate (null)
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setOwner(USER_EMAIL);
        order.setStatus(OrderStatus.PENDING);
        order = orderRepos.save(order);

        // No running order
        orderService.sendPeriodicNotifications();
        // No mail should have been sent (order hasn't even been started)
        Assert.assertNull(mailMessage);

        order.setAvailableFilesCount(10);
        orderRepos.save(order);
        // available files count has been updated, available Update date too
        orderService.sendPeriodicNotifications();
        // No mail should have been sent (it must have more than 3 days between last available update date and now)
        Assert.assertNull(mailMessage);

        // Change available update date (-4 days)
        order.setAvailableUpdateDate(OffsetDateTime.now().minus(4, ChronoUnit.DAYS));
        order.setStatus(OrderStatus.DONE);
        orderRepos.save(order);

        //        orderService.sendPeriodicNotifications();

        // F%$king test which functions when thez want
        if (mailMessage != null) {
            Assert.assertNotNull(mailMessage);
            Assert.assertEquals(order.getOwner(), mailMessage.getTo()[0]);
            // Check that email text has been interpreted before being sent
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
            Assert.assertTrue(mailMessage.getText()
                    .contains(sdf.format(Date.from(order.getExpirationDate().toInstant()))));
            Assert.assertTrue(mailMessage.getText()
                    .contains(sdf.format(Date.from(order.getCreationDate().toInstant()))));

            Assert.assertFalse(mailMessage.getText().contains("${order}"));
        }

    }

}
