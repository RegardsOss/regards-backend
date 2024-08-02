/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockServiceResponse;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.log.CorrelationIdUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IDatasetTaskRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescriptionDto;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.service.processing.IOrderProcessingService;
import fr.cnes.regards.modules.order.service.utils.BasketSelectionPageSearch;
import fr.cnes.regards.modules.order.service.utils.FileSelectionDescriptionValidator;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;
import fr.cnes.regards.modules.order.service.utils.SuborderSizeCounter;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.templates.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.OffsetDateTime;
import java.util.*;

import static fr.cnes.regards.modules.order.service.utils.LogUtils.ORDER_ID_LOG_KEY;

@Service
@RefreshScope
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrderCreationService implements IOrderCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreationService.class);

    private static final int MAX_BUCKET_FILE_COUNT = 5_000;

    private final IOrderRepository orderRepository;

    private final IDatasetTaskRepository datasetTaskRepository;

    private final IOrderDataFileService dataFileService;

    private final IOrderJobService orderJobService;

    private final BasketSelectionPageSearch basketSelectionPageSearch;

    private final OrderHelperService orderHelperService;

    private final IProjectsClient projectClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IOrderProcessingService orderProcessingService;

    private final TemplateService templateService;

    private final INotificationClient notificationClient;

    private final IEmailClient emailClient;

    private final SuborderSizeCounter suborderSizeCounter;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final OrderResponseService orderResponseService;

    private final OrderAttachmentDataSetService orderAttachmentDataSetService;

    private final IOrderCreationService self;

    private final LockService lockService;

    public OrderCreationService(IOrderRepository orderRepository,
                                IOrderDataFileService dataFileService,
                                IOrderJobService orderJobService,
                                BasketSelectionPageSearch basketSelectionPageSearch,
                                SuborderSizeCounter suborderSizeCounter,
                                INotificationClient notificationClient,
                                IEmailClient emailClient,
                                OrderHelperService orderHelperService,
                                IProjectsClient projectClient,
                                ApplicationEventPublisher applicationEventPublisher,
                                IRuntimeTenantResolver runtimeTenantResolver,
                                IOrderProcessingService orderProcessingService,
                                TemplateService templateService,
                                OrderResponseService orderResponseService,
                                OrderAttachmentDataSetService orderAttachmentDataSetService,
                                IOrderCreationService orderCreationService,
                                LockService lockeService,
                                IDatasetTaskRepository datasetTaskRepository) {
        this.orderRepository = orderRepository;
        this.dataFileService = dataFileService;
        this.orderJobService = orderJobService;
        this.basketSelectionPageSearch = basketSelectionPageSearch;
        this.suborderSizeCounter = suborderSizeCounter;
        this.notificationClient = notificationClient;
        this.emailClient = emailClient;
        this.orderHelperService = orderHelperService;
        this.projectClient = projectClient;
        this.applicationEventPublisher = applicationEventPublisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.orderProcessingService = orderProcessingService;
        this.templateService = templateService;
        this.orderResponseService = orderResponseService;
        this.orderAttachmentDataSetService = orderAttachmentDataSetService;
        this.lockService = lockeService;
        this.datasetTaskRepository = datasetTaskRepository;
        this.self = orderCreationService;
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void asyncCompleteOrderCreation(Basket basket,
                                           String owner,
                                           Long orderId,
                                           int subOrderDuration,
                                           String role,
                                           String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + orderId);
            // Lock with username to avoid modifying same jobs associated to a given user for different orders
            LockServiceResponse<Object> lockResult = lockService.runWithLock(String.format("order-completion-%s",
                                                                                           owner), () -> {
                self.completeOrderCreation(basket, orderId, role, subOrderDuration, tenant);
                return null;
            });
            if (!lockResult.isExecuted()) {
                computeOrderFailure(orderId,
                                    String.format(
                                        "Order computation failed, an other order for user %s is still computing. "
                                        + "Retry later.",
                                        owner));
            }
        } catch (InterruptedException e) {
            computeOrderFailure(orderId, String.format(e.getMessage(), owner));
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    /**
     * Update order with the given id with failure status and given error cause.
     */
    public void computeOrderFailure(Long orderId, String cause) {
        Order order = orderRepository.getById(orderId);
        order.setStatus(OrderStatus.FAILED);
        order.setMessage(cause);
        orderRepository.save(order);
    }

    @Override
    public void completeOrderCreation(Basket basket, Long orderId, String role, int subOrderDuration, String tenant) {
        boolean hasProcessing = false;

        Order order = orderRepository.findCompleteById(orderId);
        OrderCounts orderCounts = new OrderCounts();
        try {
            String owner = order.getOwner();

            LOGGER.info("Completing order (id: {}) with owner {}...", order.getId(), owner);
            // To search objects with SearchClient
            int priority = orderJobService.computePriority(owner, role);

            // Dataset selections
            for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
                if (dsSel.hasProcessing()) {
                    orderCounts = orderProcessingService.manageProcessedDatasetSelection(order,
                                                                                         owner,
                                                                                         role,
                                                                                         dsSel,
                                                                                         tenant,
                                                                                         owner,
                                                                                         role,
                                                                                         orderCounts,
                                                                                         subOrderDuration);
                    hasProcessing = true;
                } else {
                    orderCounts = self.manageDatasetSelection(order,
                                                              owner,
                                                              role,
                                                              priority,
                                                              orderCounts,
                                                              dsSel,
                                                              subOrderDuration);
                }
            }

            OffsetDateTime expirationDate = orderHelperService.computeOrderExpirationDate(orderCounts.getInternalSubOrderCount(),
                                                                                          subOrderDuration);
            order.setExpirationDate(expirationDate);
            orderHelperService.updateJobInfosExpirationDate(expirationDate, orderCounts.getJobInfoIdSet());

            // In case order contains only external files, percent completion can be set to 100%, else completion is
            // computed when files are available (even if some external files exist, this case will not (often) occur
            if (!hasProcessing && (orderCounts.getInternalFilesCount() == 0) && (orderCounts.getExternalFilesCount()
                                                                                 > 0)) {
                // Because external files haven't size set (files.size isn't allowed to be mapped on DatasourcePlugins
                // other than AipDatasourcePlugin which manage only internal files), these will not be taken into
                // account by {@see OrderService#updateCurrentOrdersComputedValues}
                order.setPercentCompleted(100);
                order.setAvailableFilesCount(orderCounts.getExternalFilesCount());
                order.setWaitingForUser(true);
                // No need to set order as waitingForUser because these files do not block anything
            }
        } catch (ModuleException e) {
            LOGGER.error("Error while completing order creation", e);
            order.setStatus(OrderStatus.FAILED);
            order.setMessage(e.getMessage());
            order.setExpirationDate(orderHelperService.computeOrderExpirationDate(0, subOrderDuration));
        }
        // Notify external sub-orders in DONE state
        notifyFinishedSubOrder(order, orderCounts);

        manageOrderState(order, orderCounts);
        order = orderRepository.save(order);
        LOGGER.info("Order (id: {}) saved with status {}", order.getId(), order.getStatus());

        notifyIfFinishedOrder(order);

        if (order.getStatus() != OrderStatus.FAILED && order.getStatus() != OrderStatus.DONE_WITH_WARNING) {
            if (order.getFrontendUrl() != null) {
                sendOrderCreationEmail(order);
            }
            orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
        }
        applicationEventPublisher.publishEvent(new OrderCreationCompletedEvent(order));
    }

    /**
     * Notify all sub-orders in D0NE state of order.
     *
     * @param order       the order
     * @param orderCounts the count of order
     */
    private void notifyFinishedSubOrder(Order order, OrderCounts orderCounts) {
        for (DatasetTask datasetTask : order.getDatasetTasks()) {
            for (FilesTask subOrder : datasetTask.getReliantTasks()) {
                if (subOrder.isEnded()) {
                    orderResponseService.notifySuborderDone(order.getCorrelationId(),
                                                            order.getOwner(),
                                                            order.getId(),
                                                            subOrder.getId(),
                                                            orderCounts.getExternalSubOrderCount()
                                                            + orderCounts.getInternalSubOrderCount());
                }
            }
        }
    }

    /**
     * Notify a notification if order status is : DONE, FAILED, DONE_WITH_WARNING.
     *
     * @param order order
     */
    private void notifyIfFinishedOrder(Order order) {
        if (order.getStatus().isOneOfStatuses(OrderStatus.DONE, OrderStatus.FAILED, OrderStatus.DONE_WITH_WARNING)) {
            orderResponseService.notifyFinishedOrder(order);
        }
    }

    private void manageOrderState(Order order, OrderCounts orderCounts) {
        // Be careful to not unset FAILED status
        if (order.getStatus() != OrderStatus.FAILED) {
            // if order doesn't contain at least one DatasetTask, set a DONE_WITH_WARNING status
            if (order.getDatasetTasks().isEmpty()) {
                order.setStatus(OrderStatus.DONE_WITH_WARNING);
                if (orderCounts.getFeaturesCount() == 0) {
                    order.setMessage("It seems that the queried features do not exist anymore.");
                } else {
                    order.setMessage("No file match filters.");
                }
            } else if (order.getPercentCompleted() == 100) { // Order contains only external files
                order.setStatus(OrderStatus.DONE);
            } else {
                // Order is ready to be taken into account
                order.setStatus(OrderStatus.RUNNING);
            }
        }
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public OrderCounts manageDatasetSelection(Order order,
                                              String owner,
                                              String role,
                                              int priority,
                                              OrderCounts orderCountsGlobal,
                                              BasketDatasetSelection dsSel,
                                              int subOrderDuration) throws ModuleException {

        OrderCounts orderCounts = new OrderCounts();
        // Bucket of internal files (managed by Storage)
        Set<OrderDataFile> storageBucketFiles = new HashSet<>();
        // Bucket of external files (not managed by Storage, directly downloadable)
        Set<OrderDataFile> externalBucketFiles = new HashSet<>();

        List<Long> externalSubOrderIds = new ArrayList<>();

        DatasetTask dsTask = DatasetTask.fromBasketSelection(dsSel, DataTypeSelection.ALL.getFileTypes());

        // Firstly, get dataset attached rawdata files, and add them to buckets
        orderAttachmentDataSetService.fillBucketsWithDataSetFiles(order,
                                                                  dsSel,
                                                                  storageBucketFiles,
                                                                  externalBucketFiles);

        // Execute opensearch request
        List<EntityFeature> features;
        int page = 0;
        do {
            try {
                FeignSecurityManager.asUser(owner, role);
                features = basketSelectionPageSearch.searchDataObjects(dsSel, page);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                LOGGER.error("Cannot retrieve features ", e);
                throw new ModuleException(e.getMessage());
            } finally {
                FeignSecurityManager.reset();
            }
            // For each DataObject
            for (EntityFeature feature : features) {
                dispatchFeatureFilesInBuckets(order, feature, storageBucketFiles, externalBucketFiles, dsSel);

                // If sum of files size > storageBucketSize, add a new bucket
                if ((storageBucketFiles.size() >= MAX_BUCKET_FILE_COUNT) || suborderSizeCounter.storageBucketTooBig(
                    storageBucketFiles)) {
                    orderCounts.addToInternalFilesCount(storageBucketFiles.size());
                    orderCounts.addTotalFileSizeOf(storageBucketFiles);
                    orderCounts.addJobInfoId(orderHelperService.createStorageSubOrderAndStoreDataFiles(dsTask,
                                                                                                       storageBucketFiles,
                                                                                                       order,
                                                                                                       subOrderDuration,
                                                                                                       role,
                                                                                                       priority));
                    orderCounts.incrInternalSubOrderCount();
                    storageBucketFiles.clear();
                }
                // If external bucket files count > MAX_EXTERNAL_BUCKET_FILE_COUNT, add a new bucket
                if ((externalBucketFiles.size() >= MAX_BUCKET_FILE_COUNT) || suborderSizeCounter.externalBucketTooBig(
                    externalBucketFiles)) {
                    orderCounts.addToExternalFilesCount(externalBucketFiles.size());
                    orderCounts.addTotalFileSizeOf(externalBucketFiles);
                    orderHelperService.createExternalSubOrder(dsTask, externalBucketFiles, order);
                    orderCounts.incrExternalSubOrderCount();
                    externalBucketFiles.clear();
                }
            }
            orderCounts.addFeaturesCount(features.size());
            page++;
        } while (!features.isEmpty());
        // Manage remaining files on each type of buckets
        if (!storageBucketFiles.isEmpty()) {
            orderCounts.addToInternalFilesCount(storageBucketFiles.size());
            orderCounts.addTotalFileSizeOf(storageBucketFiles);
            orderCounts.addJobInfoId(orderHelperService.createStorageSubOrderAndStoreDataFiles(dsTask,
                                                                                               storageBucketFiles,
                                                                                               order,
                                                                                               subOrderDuration,
                                                                                               role,
                                                                                               priority));
            orderCounts.incrInternalSubOrderCount();
        }
        if (!externalBucketFiles.isEmpty()) {
            orderCounts.addToExternalFilesCount(externalBucketFiles.size());
            orderCounts.addTotalFileSizeOf(externalBucketFiles);
            orderHelperService.createExternalSubOrder(dsTask, externalBucketFiles, order);
            orderCounts.incrExternalSubOrderCount();
        }

        // Add dsTask ONLY IF it contains at least one FilesTask
        if (!dsTask.getReliantTasks().isEmpty()) {
            order.addDatasetOrderTask(dsTask);
        }
        // recalculate files counters if filters have been applied
        dsTask.setFilesCount(orderCounts.getExternalFilesCount() + orderCounts.getInternalFilesCount());
        dsTask.setFilesSize(orderCounts.getTotalFilesSize());

        return OrderCounts.add(orderCountsGlobal, orderCounts);
    }

    /**
     * Dispatch {@link DataFile}s of given {@link EntityFeature} into internal or external buckets.
     */
    private void dispatchFeatureFilesInBuckets(Order order,
                                               EntityFeature feature,
                                               Set<OrderDataFile> storageBucketFiles,
                                               Set<OrderDataFile> externalBucketFiles,
                                               BasketDatasetSelection dsSel) {
        FileSelectionDescriptionDto fileSelectDescr = dsSel.getFileSelectionDescription();
        List<DataFile> dataFileFiltered = feature.getFiles()
                                                 .values()
                                                 .stream()
                                                 .filter(orderHelperService::isDataFileOrderable)
                                                 .filter(dataFile -> FileSelectionDescriptionValidator.validate(dataFile,
                                                                                                                fileSelectDescr))
                                                 .toList();
        for (DataFile dataFile : dataFileFiltered) {
            // Referenced dataFiles are externally stored.
            if (!dataFile.isReference()) {
                addInternalFileToStorageBucket(order, storageBucketFiles, dataFile, feature);
            } else {
                addExternalFileToExternalBucket(order, externalBucketFiles, dataFile, feature);
            }
        }
    }

    private void addExternalFileToExternalBucket(Order order,
                                                 Set<OrderDataFile> externalBucketFiles,
                                                 DataFile datafile,
                                                 EntityFeature feature) {
        OrderDataFile orderDataFile = new OrderDataFile(datafile,
                                                        feature.getId(),
                                                        order.getId(),
                                                        feature.getProviderId(),
                                                        feature.getVersion());
        // An external file is immediately set to AVAILABLE status because it needs nothing more to be downloaded
        orderDataFile.setState(FileState.AVAILABLE);
        externalBucketFiles.add(orderDataFile);
    }

    private void addInternalFileToStorageBucket(Order order,
                                                Set<OrderDataFile> storageBucketFiles,
                                                DataFile dataFile,
                                                EntityFeature feature) {
        if (dataFile.getFilesize() != null) {
            OrderDataFile orderDataFile = new OrderDataFile(dataFile,
                                                            feature.getId(),
                                                            order.getId(),
                                                            feature.getProviderId(),
                                                            feature.getVersion());
            storageBucketFiles.add(orderDataFile);
            // Send a very useful notification if file is bigger than bucket size
            if (orderDataFile.getFilesize() > suborderSizeCounter.getStorageBucketSize()) {
                // To send a notification, NotificationClient needs it
                notificationClient.notify(String.format("File \"%s\" is bigger than sub-order size",
                                                        orderDataFile.getFilename()),
                                          "Order creation",
                                          NotificationLevel.WARNING,
                                          DefaultRole.PROJECT_ADMIN);
            }
        } else {
            LOGGER.debug("Data file with name '{}' cannot be ordered because its size is null!",
                         dataFile.getFilename());
        }
    }

    private void sendOrderCreationEmail(Order order) {
        // Generate token
        String tokenRequestParam = IOrderService.ORDER_TOKEN + "=" + orderHelperService.generateToken4PublicEndpoint(
            order);

        FeignSecurityManager.asSystem();
        try {
            Project project = ResponseEntityUtils.extractContentOrThrow(projectClient.retrieveProject(
                runtimeTenantResolver.getTenant()), "An error occurred while retrieving project");
            String host = project.getHost();
            FeignSecurityManager.reset();

            String urlStart = host + orderHelperService.buildUrl();

            // Metalink file public url
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("expiration_date", order.getExpirationDate().toString());
            dataMap.put("project", runtimeTenantResolver.getTenant());
            dataMap.put("order_label", order.getId().toString());
            dataMap.put("metalink_download_url",
                        urlStart
                        + "/user/orders/metalink/download?"
                        + tokenRequestParam
                        + "&scope="
                        + runtimeTenantResolver.getTenant());
            dataMap.put("regards_downloader_url", "https://github.com/RegardsOss/RegardsDownloader/releases");
            dataMap.put("orders_url", host + order.getFrontendUrl());

            // Create mail
            String message = templateService.render(OrderTemplateConf.ORDER_CREATED_TEMPLATE_NAME, dataMap);

            // Send it
            FeignSecurityManager.asInstance();
            emailClient.sendEmail(message,
                                  String.format("Order number %d is confirmed", order.getId()),
                                  null,
                                  order.getOwner());
        } catch (Exception e) {
            LOGGER.warn("Error while attempting to send order creation email (order has been created anyway)", e);
        }
        FeignSecurityManager.reset();
    }

    /**
     * Event type (used by tests) to be notified when the order creation process is finished.
     */

    public static class OrderCreationCompletedEvent extends ApplicationEvent {

        private final Order order;

        public OrderCreationCompletedEvent(Order order) {
            super(order);
            this.order = order;
        }

        public Order getOrder() {
            return order;
        }
    }

}
