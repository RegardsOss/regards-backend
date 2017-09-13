package fr.cnes.regards.modules.order.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.StorageFilesJobParameter;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderService implements IOrderService {

    @Autowired
    private IOrderRepository repos;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private ICatalogClient catalogClient;

    @Value("${regards.order.files.bucket.size.Mb:100}")
    private int bucketSizeMb;

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
        order.setEmail(basket.getEmail());

        // Dataset selections
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            DatasetTask dsTask = createDatasetTask(dsSel);

            Set<OrderDataFile> bucketFiles = new HashSet<>();

            // Execute opensearch request
            int page = 1;
            List<DataObject> objects = searchDataObjects(dsSel.getOpenSearchRequest(), page);
            while (!objects.isEmpty()) {
                // For each DataObject
                for (DataObject object : objects) {
                    // For each asked DataTypes
                    Multimap<DataType, DataFile> filesMultimap = object.getFiles();
                    for (DataType dataType : DATA_TYPES) {
                        for (DataFile file : filesMultimap.get(dataType)) {
                            bucketFiles.add(new OrderDataFile(file, object.getIpId()));
                        }
                    }
                    // If sum of files size > bucketSize, add a new bucket
                    if (bucketFiles.stream().mapToLong(DataFile::getSize).sum() >= bucketSize) {
                        createSubOrder(basket, dsTask, bucketFiles);

                        bucketFiles.clear();
                    }
                }
                objects = searchDataObjects(dsSel.getOpenSearchRequest(), ++page);
            }
            // Manage remaining files
            if (!bucketFiles.isEmpty()) {
                createSubOrder(basket, dsTask, bucketFiles);
            }

            order.addDatasetOrderTask(dsTask);
        }

        return repos.save(order);
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
    private void createSubOrder(Basket basket, DatasetTask dsTask, Set<OrderDataFile> bucketFiles) {
        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.addAllFiles(bucketFiles);

        JobInfo storageJobInfo = new JobInfo();
        storageJobInfo.setParameters(
                new StorageFilesJobParameter(bucketFiles.toArray(new OrderDataFile[bucketFiles.size()])));
        storageJobInfo.setOwner(basket.getEmail());
        storageJobInfo.setClassName("fr.cnes.regards.modules.order.domain.StorageFilesJob");
        // TODO cf. Token
        //                        storageJobInfo.setExpirationDate();
        storageJobInfo.setPriority(50);
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
        return repos.findAllOrderByCreationDateDesc(pageRequest);
    }

    @Override
    public Page<Order> findAll(String user, Pageable pageRequest) {
        return repos.findAllByEmailOrderByCreationDateDesc(user, pageRequest);
    }
}
