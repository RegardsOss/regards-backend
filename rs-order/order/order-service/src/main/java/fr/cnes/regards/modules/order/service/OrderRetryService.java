package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;
import fr.cnes.regards.modules.order.service.utils.SuborderSizeCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RefreshScope
@MultitenantTransactional
public class OrderRetryService implements IOrderRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRetryService.class);

    private static final List<String> ERROR_STATE_LIST = Arrays.asList(FileState.ERROR.name(), FileState.DOWNLOAD_ERROR.name());
    private static final int LIMIT = 1_000;
    private static final int MAX_BUCKET_FILE_COUNT = 5_000;

    @Autowired
    private IOrderRetryService self;

    private final IOrderDataFileRepository orderDataFileRepository;
    private final IOrderRepository orderRepository;
    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IOrderJobService orderJobService;
    private final IDatasetTaskService datasetTaskService;
    private final OrderHelperService orderHelperService;
    private final SuborderSizeCounter suborderSizeCounter;

    public OrderRetryService(IOrderDataFileRepository orderDataFileRepository, IOrderRepository orderRepository, IRuntimeTenantResolver runtimeTenantResolver,
                             IOrderJobService orderJobService, IDatasetTaskService datasetTaskService, OrderHelperService orderHelperService,
                             SuborderSizeCounter suborderSizeCounter
    ) {
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderRepository = orderRepository;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.orderJobService = orderJobService;
        this.datasetTaskService = datasetTaskService;
        this.orderHelperService = orderHelperService;
        this.suborderSizeCounter = suborderSizeCounter;
    }


    @Override
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void asyncCompleteRetry(long orderId, String role, int subOrderDuration, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        self.retry(orderId, role, subOrderDuration);
    }

    @Override
    public void retry(long orderId, String role, int subOrderDuration) {

        Order order = orderRepository.findSimpleById(orderId);
        String owner = order.getOwner();
        LOGGER.info("Retrying order (id: {}) with owner {}...", order.getId(), owner);

        try {

            int priority = orderJobService.computePriority(owner, role);
            OrderCounts orderCounts = new OrderCounts();
            order.getDatasetTasks().forEach(
                    datasetTask -> self.retryDatasetTask(datasetTask.getId(), orderCounts, order.getId(), owner, subOrderDuration, priority, role)
            );

            // Update Order expiration date and JobInfo expiration date
            OffsetDateTime expirationDate = orderHelperService.computeOrderExpirationDate(order.getExpirationDate(), orderCounts.getSubOrderCount(), subOrderDuration);
            order.setExpirationDate(expirationDate);
            orderHelperService.updateJobInfosExpirationDate(expirationDate, orderCounts.getJobInfoIdSet());

            order.setStatus(OrderStatus.RUNNING);

            orderJobService.manageUserOrderStorageFilesJobInfos(owner);

        } catch (Exception e) {
            LOGGER.error("Error while retrying order", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryDatasetTask(Long datasetTaskId, OrderCounts orderCounts, long orderId, String owner, int subOrderDuration, int priority, String role) {

        DatasetTask datasetTask = datasetTaskService.loadComplete(datasetTaskId);

        List<OrderDataFile> orderDataFiles;
        Set<OrderDataFile> storageBucketFiles = new HashSet<>();
        Set<OrderDataFile> externalBucketFiles = new HashSet<>();

        do {
            orderDataFiles = orderDataFileRepository.selectByDatasetTaskAndStateAndLimit(datasetTask.getId(), ERROR_STATE_LIST, LIMIT);
            orderDataFiles.forEach(orderDataFile -> {
                resetState(orderDataFile);
                if (Boolean.TRUE.equals(orderDataFile.isReference())) {
                    externalBucketFiles.add(orderDataFile);
                    manageExternalBucket(externalBucketFiles, false, datasetTask, orderCounts, orderId, owner);
                } else {
                    storageBucketFiles.add(orderDataFile);
                    manageStorageBucket(storageBucketFiles, false, datasetTask, orderCounts, orderId, owner, subOrderDuration, priority, role);
                }
            });
        } while (orderDataFiles.size() >= LIMIT);

        manageExternalBucket(externalBucketFiles, true, datasetTask, orderCounts, orderId, owner);
        manageStorageBucket(storageBucketFiles, true, datasetTask, orderCounts, orderId, owner, subOrderDuration, priority, role);
    }

    private void manageStorageBucket(Set<OrderDataFile> bucket, boolean last, DatasetTask datasetTask, OrderCounts counts, long orderId, String owner, int subOrderDuration,
                                     int priority, String role
    ) {
        if (!bucket.isEmpty() && (last || bucket.size() >= MAX_BUCKET_FILE_COUNT || suborderSizeCounter.storageBucketTooBig(bucket))) {
            UUID jobInfoId = self.createStorageSubOrder(datasetTask, bucket, orderId, owner, subOrderDuration, role, priority);
            counts.addJobInfoId(jobInfoId);
            counts.incrSubOrderCount();
            bucket.clear();
        }
    }

    private void manageExternalBucket(Set<OrderDataFile> bucket, boolean last, DatasetTask datasetTask, OrderCounts counts, long orderId, String owner) {
        if (!bucket.isEmpty() && (last || bucket.size() >= MAX_BUCKET_FILE_COUNT || suborderSizeCounter.externalBucketTooBig(bucket))) {
            self.createExternalSubOrder(datasetTask, bucket, orderId, owner);
            counts.incrSubOrderCount();
            bucket.clear();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createStorageSubOrder(DatasetTask datasetTask, Set<OrderDataFile> orderDataFiles, long orderId, String owner, int subOrderDuration, String role, int priority) {
        return orderHelperService.createStorageSubOrder(datasetTask, orderDataFiles, orderId, owner, subOrderDuration, role, priority);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createExternalSubOrder(DatasetTask datasetTask, Set<OrderDataFile> orderDataFiles, long orderId, String owner) {
        orderHelperService.createExternalSubOrder(datasetTask, orderDataFiles, orderId, owner);
    }

    private void resetState(OrderDataFile orderDataFile) {
        orderDataFile.setState(FileState.PENDING);
        orderDataFile.setDownloadError(null);
    }

}
