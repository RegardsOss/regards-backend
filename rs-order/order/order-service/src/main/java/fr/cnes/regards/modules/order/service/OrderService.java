package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.NotYetAvailableException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.job.ExpirationDateJobParameter;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderService implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private IOrderRepository repos;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private ICatalogClient catalogClient;

    @Autowired
    private IAipClient aipClient;

    @Value("${regards.order.files.bucket.size.Mb:100}")
    private int bucketSizeMb;

    @Value("${regards.order.validation.period.days:3}")
    private int orderValidationPeriodDays;

    private long bucketSize = bucketSizeMb * 1024l * 1024l;

    /**
     * Set of DataTypes to retrieve on DataObjects
     */
    private Set<DataType> DATA_TYPES = Stream.of(DataTypeSelection.ALL.getFileTypes()).map(DataType::valueOf)
            .collect(Collectors.toSet());

    @Override
    public Order createOrder(Basket basket) {
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(orderValidationPeriodDays, ChronoUnit.DAYS));
        order.setOwner(basket.getOwner());
        // To generate orderId
        order = repos.save(order);
        int priority = orderJobService.computePriority(order.getOwner(), SecurityUtils.getActualRole());

        // Dataset selections
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            DatasetTask dsTask = createDatasetTask(dsSel);

            Set<OrderDataFile> bucketFiles = new HashSet<>();

            // Execute opensearch request
            int page = 0;
            List<DataObject> objects = searchDataObjects(dsSel.getOpenSearchRequest(), page);
            while (!objects.isEmpty()) {
                // For each DataObject
                for (DataObject object : objects) {
                    // For each asked DataTypes
                    Multimap<DataType, DataFile> filesMultimap = object.getFiles();
                    for (DataType dataType : DATA_TYPES) {
                        for (DataFile file : filesMultimap.get(dataType)) {
                            bucketFiles.add(new OrderDataFile(file, object.getIpId(), order.getId()));
                        }
                    }
                    // If sum of files size > bucketSize, add a new bucket
                    if (bucketFiles.stream().mapToLong(DataFile::getSize).sum() >= bucketSize) {
                        createSubOrder(basket, dsTask, bucketFiles, order.getExpirationDate(), priority);

                        bucketFiles.clear();
                    }
                }
                objects = searchDataObjects(dsSel.getOpenSearchRequest(), ++page);
            }
            // Manage remaining files
            if (!bucketFiles.isEmpty()) {
                createSubOrder(basket, dsTask, bucketFiles, order.getExpirationDate(), priority);
            }

            order.addDatasetOrderTask(dsTask);
        }
        order = repos.save(order);
        orderJobService.manageUserOrderJobInfos(order.getOwner());
        return order;
    }

    private DatasetTask createDatasetTask(BasketDatasetSelection dsSel) {
        DatasetTask dsTask = new DatasetTask();
        dsTask.setDatasetIpid(dsSel.getDatasetIpid());
        dsTask.setDatasetLabel(dsSel.getDatasetLabel());
        dsTask.setFilesCount(dsSel.getFilesCount());
        dsTask.setFilesSize(dsSel.getFilesSize());
        dsTask.setObjectsCount(dsSel.getObjectsCount());
        dsTask.setOpenSearchRequest(dsSel.getOpenSearchRequest());
        return dsTask;
    }

    /**
     * Create a sub-order ie a FilesTask, a persisted JobInfo (associated to FilesTask) and add it to DatasetTask
     */
    private void createSubOrder(Basket basket, DatasetTask dsTask, Set<OrderDataFile> bucketFiles,
            OffsetDateTime expirationDate, int priority) {
        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.addAllFiles(bucketFiles);

        JobInfo storageJobInfo = new JobInfo();
        storageJobInfo.setParameters(new FilesJobParameter(bucketFiles.toArray(new OrderDataFile[bucketFiles.size()])),
                                     new ExpirationDateJobParameter(expirationDate));
        storageJobInfo.setOwner(basket.getOwner());
        storageJobInfo.setClassName("fr.cnes.regards.modules.order.service.job.StorageFilesJob");
        storageJobInfo.setPriority(priority);
        // Create JobInfo and associate to FilesTask
        currentFilesTask.setJobInfo(jobInfoService.createAsPending(storageJobInfo));
        dsTask.addReliantTask(currentFilesTask);
    }

    private List<DataObject> searchDataObjects(String openSearchRequest, int page) {
        Map<String, String> requestMap = Collections.singletonMap("q", openSearchRequest);

        ResponseEntity<PagedResources<Resource<DataObject>>> pagedResourcesResponseEntity = catalogClient
                .searchDataobjects(requestMap, new PageRequest(page, 10_000));
        return pagedResourcesResponseEntity.getBody().getContent().stream().map(r -> r.getContent())
                .collect(Collectors.toList());
    }

    @Override
    public Order loadSimple(Long id) {
        return repos.findSimpleById(id);
    }

    @Override
    public Order loadComplete(Long id) {
        return repos.findCompleteById(id);
    }

    @Override
    public Page<Order> findAll(Pageable pageRequest) {
        return repos.findAllByOrderByCreationDateDesc(pageRequest);
    }

    @Override
    public Page<Order> findAll(String user, Pageable pageRequest) {
        return repos.findAllByOwnerOrderByCreationDateDesc(user, pageRequest);
    }

    @Override
    public void downloadOrderCurrentZip(Long orderId, HttpServletResponse response) throws NotYetAvailableException {

        List<OrderDataFile> availableFiles = dataFileService.findAllAvailables(orderId);
        if (availableFiles.isEmpty()) {
            throw new NotYetAvailableException();
        }

        response.addHeader("Content-disposition",
                           "attachment;filename=order_" + OffsetDateTime.now().toString() + ".zip");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        OrderDataFile curDataFile = null;
        String curAip = null;
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            Set<String> aips = new HashSet<>();
            for (OrderDataFile dataFile : availableFiles) {
                String aip = dataFile.getIpId().toString();
                curAip = aip;
                curDataFile = dataFile;
                try (InputStream is = aipClient.downloadFile(aip, dataFile.getChecksum())) {
                    if (is == null) {
                        throw new FileNotFoundException(
                                String.format("aip : %s, checksum : %s", aip, dataFile.getChecksum()));
                    }
                    // To avoid adding AIP entry several times
                    if (!aips.contains(aip)) {
                        zos.putNextEntry(new ZipEntry(aip + "/"));
                        aips.add(aip);
                        zos.closeEntry();
                    }
                    zos.putNextEntry(new ZipEntry(aip + "/" + dataFile.getName()));
                    ByteStreams.copy(is, zos);
                    zos.closeEntry();
                }
            }
            zos.flush();
            zos.finish();
        } catch (Throwable t) {
            LOGGER.error("File : " + curDataFile.getName() + ", aip : " + curAip);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
        try {
            availableFiles.forEach(f -> f.setState(FileState.DOWNLOADED));
            dataFileService.save(availableFiles);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
