package fr.cnes.regards.modules.order.service;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.StorageFilesJobParameter;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class OrderServiceIT {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderRepository orderRepository;

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

    @Before
    public void init() {
        basketRepository.deleteAll();
        orderRepository.deleteAll();

        jobInfoRepository.deleteAll();
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
        basket.setEmail(USER_EMAIL);

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
        order.setEmail(USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());

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
        DataFile dataFile = new DataFile();
        dataFile.setUri(new URI("staff://toto/titi/tutu"));
        dataFile.setOnline(true);
        dataFile.setSize(1_000_000l);
        dataFile.setName("tutu");
        ds1SubOrder1Task.addFile(new OrderDataFile(dataFile, DO1_IP_ID));

        JobInfo storageJobInfo = new JobInfo();
        storageJobInfo.setClassName("...");
        storageJobInfo.setDescription("description");
        storageJobInfo.setOwner(USER_EMAIL);
        storageJobInfo.setPriority(1);
        storageJobInfo.updateStatus(JobStatus.PENDING);
        //        Multimap<UniformResourceName, DataFile> filesMultimap = HashMultimap.create();
        //        filesMultimap.put(DO1_IP_ID, dataFile);
        storageJobInfo.setParameters(
                new StorageFilesJobParameter(new OrderDataFile[] { new OrderDataFile(dataFile, DO1_IP_ID) }));
        //        storageJobInfo.setParameters(new JobParameter("files", filesMultimap));

        storageJobInfo = jobInfoRepository.save(storageJobInfo);

        ds1SubOrder1Task.setJobInfo(storageJobInfo);

        ds1OrderTask.addReliantTask(ds1SubOrder1Task);

        order = orderRepository.save(order);

        Order orderFromDb = orderRepository.findCompleteById(order.getId());
        System.out.println("-----------------------------------------------");
        System.out.println(orderFromDb);
        orderFromDb.getDatasetTasks().iterator().next().getReliantTasks().iterator().next().getReliantTasks();
        OrderDataFile[] datafiles = orderFromDb.getDatasetTasks().iterator().next().getReliantTasks().iterator().next().getJobInfo().getParameters().iterator().next().getValue();
        System.out.println(datafiles);
        System.out.println("-----------------------------------------------");
    }
}
