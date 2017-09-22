package fr.cnes.regards.modules.order.service;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext
public class OrderServiceIT {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

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
        basketRepository.deleteAll();
        orderRepository.deleteAll();

        jobInfoRepository.deleteAll();

        SecurityUtils.mockActualRole(DefaultRole.REGISTERED_USER.toString());
    }

/*    @After
    public void tearDown() {
        basketRepository.deleteAll();
        orderRepository.deleteAll();

        jobInfoRepository.deleteAll();
    }*/

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

        basket = basketRepository.save(basket);

        Order order = orderService.createOrder(basket);
        Assert.assertNotNull(order);
    }

    @Test
    @Commit
    public void testMapping() throws URISyntaxException {
        Order order = new Order();
        order.setOwner(USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(OffsetDateTime.now().plus(3, ChronoUnit.DAYS));
        order = orderRepository.save(order);

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
        dataFileRepository.save(df1);
        ds1SubOrder1Task.addFile(df1);

        DataFile dataFile2 = new DataFile();
        dataFile2.setUri(new URI("staff://toto2/titi2/tutu2"));
        dataFile2.setOnline(false);
        dataFile2.setSize(1l);
        dataFile2.setName("tutu2");
        OrderDataFile df2 = new OrderDataFile(dataFile2, DO2_IP_ID, order.getId());
        dataFileRepository.save(df2);
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

        storageJobInfo = jobInfoRepository.save(storageJobInfo);

        ds1SubOrder1Task.setJobInfo(storageJobInfo);

        ds1OrderTask.addReliantTask(ds1SubOrder1Task);

        order = orderRepository.save(order);

        Order orderFromDb = orderRepository.findCompleteById(order.getId());
        System.out.println("-----------------------------------------------");
        System.out.println(orderFromDb);
        orderFromDb.getDatasetTasks().iterator().next().getReliantTasks().iterator().next().getReliantTasks();
        System.out.println(
                orderFromDb.getDatasetTasks().iterator().next().getReliantTasks().iterator().next().getFiles()
                        .iterator().next().getName());
        OrderDataFile[] datafiles = orderFromDb.getDatasetTasks().iterator().next().getReliantTasks().iterator().next()
                .getJobInfo().getParameters().iterator().next().getValue();
        System.out.println(datafiles);
        System.out.println("-----------------------------------------------");

        Set<Long> fileTaskIds = orderFromDb.getDatasetTasks().stream().flatMap(ds -> ds.getReliantTasks().stream())
                .map(FilesTask::getId).collect(Collectors.toSet());
        List<OrderDataFile> dataFiles = dataFileRepository.findAllAvailables(order.getId());
        Assert.assertEquals(1, dataFiles.size());
    }

}
