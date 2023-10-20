package fr.cnes.regards.modules.order.client.env.utils;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.*;
import fr.cnes.regards.modules.order.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.order.client.env.utils.OrderTestConstants.*;
import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

@Service
public class OrderTestUtilsService {

    @Autowired
    protected IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IFilesTasksRepository filesTasksRepository;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IDatasetTaskRepository datasetTaskRepository;

    /**
     * Mock creation of an order by directly saving it into the db. There are :
     * <ul>
     *     <li>2 Dataset tasks</li>
     *     <li>3 File tasks</li>
     *     <li>14 Data files</li>
     * </ul>
     *
     * @return order created
     */

    public Order createOrder() {
        Order order = new Order();
        order.setOwner(USER_EMAIL);
        order.setCreationDate(OffsetDateTime.now());
        order.setLabel("order2");
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        order = orderRepository.save(order);

        // create dataset task 1
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");
        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        files1Task.setOwner(USER_EMAIL);
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1.txt", FileState.AVAILABLE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_hd.txt", FileState.AVAILABLE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_md.txt", FileState.AVAILABLE));
        files1Task.addFile(createOrderDataFile(order, DO1_IP_ID, "file1_ql_sd.txt", FileState.AVAILABLE));
        files1Task.setOrderId(order.getId());
        filesTasksRepository.save(files1Task);
        ds1Task.addReliantTask(files1Task);
        ds1Task.setFilesCount(4);
        ds1Task.setObjectsCount(4);
        ds1Task.setFilesSize(52221122);

        // create dataset task 2-0
        DatasetTask ds2Task = new DatasetTask();
        ds2Task.setDatasetIpid(DS2_IP_ID.toString());
        ds2Task.setDatasetLabel("DS2");
        order.addDatasetOrderTask(ds2Task);

        FilesTask files20Task = new FilesTask();
        files20Task.setOwner(USER_EMAIL);
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2.txt", FileState.DOWNLOADED));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_ql_hd.txt", FileState.DOWNLOADED));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_0_ql_md.txt", FileState.DOWNLOADED));
        files20Task.addFile(createOrderDataFile(order, DO2_IP_ID, "file2_0_ql_sd.txt", FileState.DOWNLOADED));
        files20Task.setOrderId(order.getId());
        filesTasksRepository.save(files20Task);
        ds2Task.addReliantTask(files20Task);

        // create dataset task 2-1
        FilesTask files21Task = new FilesTask();
        files21Task.setOwner(USER_EMAIL);
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_hd_bis.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_md.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO3_IP_ID, "file2_ql_sd.txt", FileState.AVAILABLE));
        files21Task.addFile(createOrderDataFile(order, DO4_IP_ID, "file3.txt", FileState.PENDING));
        files21Task.addFile(createOrderDataFile(order, DO5_IP_ID, "file4.txt", FileState.PENDING));
        files21Task.setOrderId(order.getId());
        filesTasksRepository.save(files21Task);
        ds2Task.addReliantTask(files21Task);
        ds2Task.setFilesCount(10);
        ds2Task.setObjectsCount(10);
        ds2Task.setFilesSize(52221122);
        return order;
    }

    private OrderDataFile createOrderDataFile(Order order,
                                              UniformResourceName aipId,
                                              String filename,
                                              FileState state) {
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/" + filename);
        dataFile1.setFilename(filename);
        File file = new File("src/test/resources/files/" + filename);
        dataFile1.setFilesize(file.length());
        dataFile1.setReference(false);
        dataFile1.setIpId(aipId);
        dataFile1.setOnline(true);
        dataFile1.setState(state);
        dataFile1.setChecksum(filename);
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        dataFile1.setDataType(DataType.RAWDATA);
        dataFileRepository.save(dataFile1);
        return dataFile1;
    }

    public List<FilesTask> getAllFilesTasks() {
        return filesTasksRepository.findAll(Sort.by("id"));
    }

    public List<OrderDataFile> getPendingDataFiles(Long orderId) {
        return dataFileRepository.findByOrderIdAndStateIn(orderId, FileState.PENDING);
    }

    public void cleanRepositories() {
        basketRepository.deleteAll();
        datasetTaskRepository.deleteAll();
        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
        jobInfoRepository.deleteAll();
        filesTasksRepository.deleteAll();
    }

}
