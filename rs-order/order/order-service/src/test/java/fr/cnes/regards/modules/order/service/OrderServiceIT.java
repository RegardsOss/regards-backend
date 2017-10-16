package fr.cnes.regards.modules.order.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.DataFile;
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
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.templates.service.TemplateService;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrderServiceIT {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderRepository orderRepos;

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
    private IEmailClient emailClient;

    @Autowired
    private TemplateService templateService;

    private static final String USER_EMAIL = "leo.mieulet@margoulin.com";

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
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();

        jobInfoRepos.deleteAll();

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<>(project), HttpStatus.OK));
    }

    @After
    public void clean() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();

        jobInfoRepos.deleteAll();

        templateService.deleteAll();
    }

    @Test
    public void test1() throws Exception {
        Basket basket = new Basket();
        basket.setOwner(USER_EMAIL);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setOpenSearchRequest("someone:something");
        dsSelection.setDatasetLabel("DS1");
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setFilesSize(1_000_000l);
        dsSelection.setFilesCount(1);
        dsSelection.setObjectsCount(1);

        BasketDatedItemsSelection itemsSelection = new BasketDatedItemsSelection();
        itemsSelection.setFilesSize(1_000_000l);
        itemsSelection.setFilesCount(1);
        itemsSelection.setObjectsCount(1);
        itemsSelection.setOpenSearchRequest("someone:something");
        itemsSelection.setDate(OffsetDateTime.now());
        dsSelection.setItemsSelections(Sets.newTreeSet(Collections.singleton(itemsSelection)));
        basket.setDatasetSelections(Sets.newTreeSet(Collections.singleton(dsSelection)));

        basket = basketRepos.save(basket);

        Order order = orderService.createOrder(basket);
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
        ds1OrderTask.setOpenSearchRequest("all:ds1");
        ds1OrderTask.setFilesCount(1);
        ds1OrderTask.setFilesSize(1_000_000l);
        ds1OrderTask.setObjectsCount(1);
        order.addDatasetOrderTask(ds1OrderTask);

        // DS1 files sub order tasks
        FilesTask ds1SubOrder1Task = new FilesTask();
        DataFile dataFile1 = new DataFile();
        dataFile1.setUri(new URI("staff://toto/titi/tutu"));
        dataFile1.setOnline(true);
        dataFile1.setSize(1_000_000l);
        dataFile1.setName("tutu");
        OrderDataFile df1 = new OrderDataFile(dataFile1, DO1_IP_ID, order.getId());
        dataFileRepos.save(df1);
        ds1SubOrder1Task.addFile(df1);

        DataFile dataFile2 = new DataFile();
        dataFile2.setUri(new URI("staff://toto2/titi2/tutu2"));
        dataFile2.setOnline(false);
        dataFile2.setSize(1l);
        dataFile2.setName("tutu2");
        OrderDataFile df2 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId());
        dataFileRepos.save(df2);
        ds1SubOrder1Task.addFile(df2);

        JobInfo storageJobInfo = new JobInfo();
        storageJobInfo.setClassName("...");
        storageJobInfo.setDescription("description");
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
    public void testBucketsJobs() throws IOException, InterruptedException {

        Basket basket = new Basket("tulavu@qui.fr");
        SortedSet<BasketDatasetSelection> dsSelections = new TreeSet<>();
        basket.setDatasetSelections(dsSelections);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.setOpenSearchRequest("ALL");
        dsSelections.add(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket);
        // Wait for end of jobs AND update of order completion values
        Thread.sleep(15_000);
        // Some files are in error
        List<OrderDataFile> files = dataFileRepos.findAllAvailables(order.getId());
        order = orderService.loadSimple(order.getId());
        // Error file count on order should be the same as total files - available files
        Assert.assertEquals(12 - files.size(), order.getFilesInErrorCount());
        // But order should be at 100 % ever
        Assert.assertEquals(100, order.getPercentCompleted());
    }

    @Test
    public void testPauseResume() throws IOException, InterruptedException, CannotResumeOrderException {

        Basket basket = new Basket("tulavu@qui.fr");
        SortedSet<BasketDatasetSelection> dsSelections = new TreeSet<>();
        basket.setDatasetSelections(dsSelections);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.setOpenSearchRequest("ALL");
        dsSelections.add(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket);

        Thread.sleep(1_000);
        orderService.pause(order.getId());

        Thread.sleep(10_000);

        // Associated jobInfo must be ever at SUCCEEDED OR ABORTED
        order = orderService.loadComplete(order.getId());
        Set<JobInfo> jobInfos = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo).collect(Collectors.toSet());
        Assert.assertTrue(jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                                  .allMatch(JobStatus::isCompatibleWithPause));
        Assert.assertTrue(order.getPercentCompleted() < 100);

        orderService.resume(order.getId());

        Thread.sleep(8_000);

        order = orderService.loadComplete(order.getId());
        jobInfos = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo).collect(Collectors.toSet());
        Assert.assertTrue(jobInfos.stream().map(jobInfo -> jobInfo.getStatus().getStatus())
                                  .allMatch(status -> status == JobStatus.SUCCEEDED));
        Assert.assertTrue(order.getPercentCompleted() == 100);
    }

    @Test
    public void testEmailNotifications()  {
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
        orderRepos.save(order);

        orderService.sendPeriodicNotifications();

        Assert.assertNotNull(mailMessage);
        Assert.assertEquals(order.getOwner(), mailMessage.getTo()[0]);
        // Check that email text has been interpreted before being sent
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Assert.assertTrue(mailMessage.getText().contains(sdf.format(Date.from(order.getExpirationDate().toInstant()))));
        Assert.assertTrue(mailMessage.getText().contains(sdf.format(Date.from(order.getCreationDate().toInstant()))));

        Assert.assertFalse(mailMessage.getText().contains("${order}"));


    }

    private static SimpleMailMessage mailMessage;

    private OrderDataFile createOrderDataFile(Order order, UniformResourceName aipId, String filename, boolean online)
            throws URISyntaxException {
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/" + filename);
        dataFile1.setName(filename);
        dataFile1.setIpId(aipId);
        if (online) {
            dataFile1.setOnline(true);
        } else {
            dataFile1.setOnline(false);
            dataFile1.setState(FileState.AVAILABLE);
        }
        dataFile1.setChecksum(filename);
        dataFile1.setSize(new File("src/test/resources/files/", filename).length());
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        return dataFile1;
    }
}
