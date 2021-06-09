/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.io.ByteStreams;
import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.dao.OrderSpecifications;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.exception.*;
import fr.cnes.regards.modules.order.metalink.schema.*;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.service.job.parameters.FilesJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.SubOrderAvailabilityPeriodJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserRoleJobParameter;
import fr.cnes.regards.modules.order.service.processing.IOrderProcessingService;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.order.service.settings.IOrderSettingsService;
import fr.cnes.regards.modules.order.service.utils.BasketSelectionPageSearch;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;
import fr.cnes.regards.modules.order.service.utils.SuborderSizeCounter;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.templates.service.TemplateService;
import freemarker.template.TemplateException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriUtils;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
@RefreshScope
@EnableScheduling
public class OrderService implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private static final String METALINK_XML_SCHEMA_NAME = "metalink.xsd";

    /**
     * Format for generated order label
     */
    private static final String ORDER_GENERATED_LABEL_FORMAT = "Order of %s";

    private static final int MAX_BUCKET_FILE_COUNT = 5_000;

    /**
     * Date formatter for order generated label
     */
    private static final DateTimeFormatter ORDER_GENERATED_LABEL_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd 'at' HH:mm:ss");

    private final Set<String> noProxyHosts = Sets.newHashSet();

    @Autowired
    private IOrderRepository repos;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private BasketSelectionPageSearch basketSelectionPageSearch;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IProjectsClient projectClient;

    @Autowired
    private IStorageRestClient storageClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IOrderService self;

    @Autowired
    private IOrderProcessingService orderProcessingService;

    @Autowired
    private IProcessingEventSender processingEventSender;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IOrderSettingsService orderSettingsService;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.order.secret}")
    private String secret;

    @Value("${zuul.prefix}")
    private String urlPrefix;

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{null}}")
    private String noProxyHostsString;

    @Autowired
    private IEmailClient emailClient;

    @Autowired
    private SuborderSizeCounter suborderSizeCounter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private Proxy proxy;

    private static String encode4Uri(String str) {
        return new String(UriUtils.encode(str, Charset.defaultCharset().name()).getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Method called at creation AND after a resfresh
     */
    @PostConstruct
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void init() {
        proxy = Strings.isNullOrEmpty(proxyHost) ? Proxy.NO_PROXY
                : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        if (noProxyHostsString != null) {
            Collections.addAll(noProxyHosts, noProxyHostsString.split("\\s*,\\s*"));
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void init(ApplicationReadyEvent event) {
        LOGGER.info("OrderService created with : userOrderParameters: {}, appSubOrderDuration: {}",
                    orderSettingsService.getUserOrderParameters(), orderSettingsService.getAppSubOrderDuration()
        );
    }

    @Override
    public Order getOrder(Long orderId) {
        return repos.findCompleteById(orderId);
    }

    @Override
    public Order createOrder(Basket basket, String label, String url, int subOrderDuration) throws EntityInvalidException {

        LOGGER.info("Generate and / or check label is unique for owner before creating back");
        // generate label when none is provided
        String basketOwner = basket.getOwner();

        String orderLabel = label;

        if (Strings.isNullOrEmpty(orderLabel)) {
            orderLabel = String.format(OrderService.ORDER_GENERATED_LABEL_FORMAT,
                                       OrderService.ORDER_GENERATED_LABEL_DATE_FORMAT.format(OffsetDateTime.now())
            );
        }
        // check length (>0 is already checked above)

        if (orderLabel.length() > Order.LABEL_FIELD_LENGTH) {
            throw new EntityInvalidException(OrderLabelErrorEnum.TOO_MANY_CHARACTERS_IN_LABEL.toString());
        } else { // check unique for current owner
            Optional<Order> sameOrderLabelOpt = repos.findByLabelAndOwner(orderLabel, basketOwner);
            if (sameOrderLabelOpt.isPresent()) {
                throw new EntityInvalidException(OrderLabelErrorEnum.LABEL_NOT_UNIQUE_FOR_OWNER.toString());
            }
        }
        // In any case: check generated label is unique for owner
        LOGGER.info("Creating order with owner {}", basketOwner);
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setOwner(basketOwner);
        order.setLabel(orderLabel);
        order.setFrontendUrl(url);
        order.setStatus(OrderStatus.PENDING);
        // expiration date is set during asyncCompleteOrderCreation execution
        // To generate orderId
        order = repos.save(order);
        // Asynchronous operation
        self.asyncCompleteOrderCreation(basket, order, subOrderDuration, authResolver.getRole(), runtimeTenantResolver.getTenant());
        return order;
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void asyncCompleteOrderCreation(Basket basket, Order order, int subOrderDuration, String role, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        self.completeOrderCreation(basket, order, subOrderDuration, role, tenant);
    }

    @Override
    public void completeOrderCreation(Basket basket, Order order, int subOrderDuration, String role, String tenant) {
        boolean hasProcessing = false;
        try {
            String owner = order.getOwner();

            LOGGER.info("Completing order (id: {}) with owner {}...", order.getId(), owner);
            // To search objects with SearchClient
            FeignSecurityManager.asUser(owner, role);
            int priority = orderJobService.computePriority(owner, role);

            OrderCounts orderCounts = new OrderCounts();

            // Dataset selections
            Set<OrderDataFile> alreadyHandledFiles = new HashSet<>();
            for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
                if (dsSel.hasProcessing()) {
                    orderCounts = orderProcessingService.manageProcessedDatasetSelection(order, dsSel, tenant, owner, role, orderCounts, subOrderDuration);
                    hasProcessing = true;
                } else {
                    orderCounts = manageDatasetSelection(order, subOrderDuration, role, priority, orderCounts, dsSel, alreadyHandledFiles);
                }
            }

            // Compute order expiration date using number of sub order created + 2 days (48 hours),
            // that gives time to users to download there last suborders
            order.setExpirationDate(OffsetDateTime.now().plusHours((long) (orderCounts.getSubOrderCount() + 48) * subOrderDuration));

            // In case order contains only external files, percent completion can be set to 100%, else completion is
            // computed when files are available (even if some external files exist, this case will not (often) occur
            if (!hasProcessing && (orderCounts.getInternalFilesCount() == 0)
                    && (orderCounts.getExternalFilesCount() > 0)) {
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
            order.setExpirationDate(OffsetDateTime.now().plusHours(subOrderDuration));
        }
        // Be careful to not unset FAILED status
        if (order.getStatus() != OrderStatus.FAILED) {
            // if order doesn't contain at least one DatasetTask, set a FAILED status
            if (order.getDatasetTasks().isEmpty()) {
                order.setStatus(OrderStatus.FAILED);
            } else if (order.getPercentCompleted() == 100) { // Order contains only external files
                order.setStatus(OrderStatus.DONE);
            } else {
                // Order is ready to be taken into account
                order.setStatus(OrderStatus.RUNNING);
            }
        }
        order = repos.save(order);
        LOGGER.info("Order (id: {}) saved with status {}", order.getId(), order.getStatus());
        if (order.getStatus() != OrderStatus.FAILED) {
            try {
                sendOrderCreationEmail(order);
            } catch (ModuleException e) {
                LOGGER.warn("Error while attempting to send order creation email (order has been created anyway)", e);
            }
            orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
        }
        // Remove basket only if order has been well-created
        if (order.getStatus() != OrderStatus.FAILED) {
            LOGGER.info("Basket emptied");
            basketRepository.deleteById(basket.getId());
        }

        applicationEventPublisher.publishEvent(new OrderCreationCompletedEvent(order));
    }

    /**
     * Event type (used by tests) to be notified when the order creation process is finished.
     */
    @SuppressWarnings("serial")
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

    private OrderCounts manageDatasetSelection(Order order, int subOrderDuration, String role, int priority, OrderCounts orderCounts,
                                               BasketDatasetSelection dsSel, Set<OrderDataFile> alreadyHandledFiles
    ) {

        DatasetTask dsTask = DatasetTask.fromBasketSelection(dsSel, DataTypeSelection.ALL.getFileTypes());

        // Bucket of internal files (managed by Storage)
        Set<OrderDataFile> storageBucketFiles = new HashSet<>();
        // Bucket of external files (not managed by Storage, directly downloadable)
        Set<OrderDataFile> externalBucketFiles = new HashSet<>();

        // Execute opensearch request
        for (List<EntityFeature> features : basketSelectionPageSearch.pagedSearchDataObjects(dsSel)) {
            // For each DataObject
            for (EntityFeature feature : features) {
                dispatchFeatureFilesInBuckets(order, feature, storageBucketFiles, externalBucketFiles,
                                              alreadyHandledFiles);

                // If sum of files size > storageBucketSize, add a new bucket
                if ((storageBucketFiles.size() >= MAX_BUCKET_FILE_COUNT)
                        || suborderSizeCounter.storageBucketTooBig(storageBucketFiles)) {
                    orderCounts.addToInternalFilesCount(storageBucketFiles.size());
                    self.createStorageSubOrder(dsTask, storageBucketFiles, order, subOrderDuration, role, priority);
                    orderCounts.incrSubOrderCount();
                    storageBucketFiles.clear();
                }
                // If external bucket files count > MAX_EXTERNAL_BUCKET_FILE_COUNT, add a new bucket
                if ((externalBucketFiles.size() >= MAX_BUCKET_FILE_COUNT)
                        || suborderSizeCounter.externalBucketTooBig(externalBucketFiles)) {
                    orderCounts.addToExternalFilesCount(externalBucketFiles.size());
                    self.createExternalSubOrder(dsTask, externalBucketFiles, order);
                    externalBucketFiles.clear();
                }
            }
        }
        // Manage remaining files on each type of buckets
        if (!storageBucketFiles.isEmpty()) {
            orderCounts.addToInternalFilesCount(storageBucketFiles.size());
            self.createStorageSubOrder(dsTask, storageBucketFiles, order, subOrderDuration, role, priority);
            orderCounts.incrSubOrderCount();
        }
        if (!externalBucketFiles.isEmpty()) {
            orderCounts.addToExternalFilesCount(externalBucketFiles.size());
            self.createExternalSubOrder(dsTask, externalBucketFiles, order);
        }

        // Add dsTask ONLY IF it contains at least one FilesTask
        if (!dsTask.getReliantTasks().isEmpty()) {
            order.addDatasetOrderTask(dsTask);
        }
        return orderCounts;
    }

    /**
     * Dispatch {@link DataFile}s of given {@link EntityFeature} into internal or external buckets.
     */
    private void dispatchFeatureFilesInBuckets(Order order, EntityFeature feature,
            Set<OrderDataFile> storageBucketFiles, Set<OrderDataFile> externalBucketFiles,
            Set<OrderDataFile> alreadyHandledFiles) {
        for (DataFile dataFile : feature.getFiles().values()) {
            // ONLY orderable data files can be ordered !!! (ie RAWDATA and QUICKLOOKS
            if (DataTypeSelection.ALL.getFileTypes().contains(dataFile.getDataType())) {
                // Referenced dataFiles are externaly stored.
                if (!dataFile.isReference()) {
                    addInternalFileToStorageBucket(order, storageBucketFiles, dataFile, feature, alreadyHandledFiles);
                } else {
                    addExternalFileToExternalBucket(order, externalBucketFiles, dataFile, feature, alreadyHandledFiles);
                }
            }
        }
    }

    private void addExternalFileToExternalBucket(Order order, Set<OrderDataFile> externalBucketFiles, DataFile datafile,
            EntityFeature feature, Set<OrderDataFile> alreadyHandledFiles) {
        OrderDataFile orderDataFile = new OrderDataFile(datafile, feature.getId(), order.getId());
        // An external file is immediately set to AVAILABLE status because it needs
        // nothing more to be doswnloaded
        orderDataFile.setState(FileState.AVAILABLE);
        if (!alreadyHandledFiles.contains(orderDataFile)) {
            externalBucketFiles.add(orderDataFile);
        }

    }

    private void addInternalFileToStorageBucket(Order order, Set<OrderDataFile> storageBucketFiles, DataFile dataFile,
            EntityFeature feature, Set<OrderDataFile> alreadyHandledFiles) {
        OrderDataFile orderDataFile = new OrderDataFile(dataFile, feature.getId(), order.getId());
        if (!alreadyHandledFiles.contains(orderDataFile)) {
            storageBucketFiles.add(orderDataFile);
        }
        // Send a very useful notification if file is bigger than bucket size
        if (orderDataFile.getFilesize() > suborderSizeCounter.getStorageBucketSize()) {
            // To send a notification, NotificationClient needs it
            notificationClient
                    .notify(String.format("File \"%s\" is bigger than sub-order size", orderDataFile.getFilename()),
                            "Order creation", NotificationLevel.WARNING, DefaultRole.PROJECT_ADMIN);
        }
    }

    /**
     * Generate a token containing orderId and expiration date to be used with public download URL (of metalink file and
     * order data files)
     */
    private String generateToken4PublicEndpoint(Order order) {
        return jwtService.generateToken(runtimeTenantResolver.getTenant(), authResolver.getUser(),
                                        authResolver.getUser(), authResolver.getRole(), order.getExpirationDate(),
                                        Collections.singletonMap(ORDER_ID_KEY, order.getId().toString()), secret, true);
    }

    private void sendOrderCreationEmail(Order order) throws ModuleException {
        // Generate token
        String tokenRequestParam = ORDER_TOKEN + "=" + generateToken4PublicEndpoint(order);

        FeignSecurityManager.asSystem();
        try {
            Project project = projectClient.retrieveProject(runtimeTenantResolver.getTenant()).getBody().getContent();
            String host = project.getHost();
            FeignSecurityManager.reset();

            String urlStart = host + urlPrefix + "/" + encode4Uri(microserviceName);

            // Metalink file public url
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("expiration_date", order.getExpirationDate().toString());
            dataMap.put("project", runtimeTenantResolver.getTenant());
            dataMap.put("order_label", order.getId().toString());
            dataMap.put("metalink_download_url", urlStart + "/user/orders/metalink/download?" + tokenRequestParam
                    + "&scope=" + runtimeTenantResolver.getTenant());
            dataMap.put("regards_downloader_url", "https://github.com/RegardsOss/RegardsDownloader/releases");
            dataMap.put("orders_url", host + order.getFrontendUrl());

            // Create mail
            String message;
            try {
                message = templateService.render(OrderTemplateConf.ORDER_CREATED_TEMPLATE_NAME, dataMap);
            } catch (TemplateException e) {
                throw new RsRuntimeException(e);
            }

            // Send it
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(message, String.format("Order number %d is confirmed", order.getId()), null,
                                  order.getOwner());
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            throw new ModuleException(e);
        }
        FeignSecurityManager.reset();
    }

    /**
     * Create a storage sub-order ie a FilesTask, a persisted JobInfo (associated to FilesTask) and add it to DatasetTask
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createStorageSubOrder(DatasetTask dsTask, Set<OrderDataFile> bucketFiles, Order order, int subOrderDuration, String role, int priority) {
        String owner = order.getOwner();
        LOGGER.info("Creating storage sub-order of {} files (owner={})", bucketFiles.size(), owner);
        dataFileService.create(bucketFiles);

        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(order.getId());
        currentFilesTask.setOwner(owner);
        currentFilesTask.addAllFiles(bucketFiles);

        // storageJobInfo is pointed by currentFilesTask so it must be locked to avoid being cleaned before FilesTask
        JobInfo storageJobInfo = new JobInfo(true);
        storageJobInfo.setParameters(
                new FilesJobParameter(bucketFiles.stream().map(OrderDataFile::getId).toArray(Long[]::new)),
                new SubOrderAvailabilityPeriodJobParameter(subOrderDuration),
                new UserJobParameter(owner), new UserRoleJobParameter(role)
        );
        storageJobInfo.setOwner(owner);
        storageJobInfo.setClassName(StorageFilesJob.class.getName());
        storageJobInfo.setPriority(priority);
        storageJobInfo.setExpirationDate(order.getExpirationDate());
        // Create JobInfo and associate to FilesTask
        currentFilesTask.setJobInfo(jobInfoService.createAsPending(storageJobInfo));
        dsTask.addReliantTask(currentFilesTask);
    }

    /**
     * Create an external sub-order ie a FilesTask, and add it to DatasetTask
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createExternalSubOrder(DatasetTask dsTask, Set<OrderDataFile> bucketFiles, Order order) {
        LOGGER.info("Creating external sub-order of {} files", bucketFiles.size());
        dataFileService.create(bucketFiles);
        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(order.getId());
        currentFilesTask.setOwner(order.getOwner());
        currentFilesTask.addAllFiles(bucketFiles);
        dsTask.addReliantTask(currentFilesTask);
    }

    @Override
    public Order loadSimple(Long id) {
        return repos.findSimpleById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order loadComplete(Long id) {
        return repos.findCompleteById(id);
    }

    @Override
    public void pause(Long id) throws CannotPauseOrderException {
        Order order = repos.findCompleteById(id);
        // Only a pending or running order can be paused
        if ((order.getStatus() != OrderStatus.PENDING) && (order.getStatus() != OrderStatus.RUNNING)) {
            throw new CannotPauseOrderException();
        }
        // Ask for all jobInfos abortion
        order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream()).map(FilesTask::getJobInfo)
                .forEach(jobInfo -> {
                    if (jobInfo != null) {
                        jobInfoService.stopJob(jobInfo.getId());
                    }
                });
        order.setStatus(OrderStatus.PAUSED);
        repos.save(order);
    }

    @Override
    public void resume(Long id) throws CannotResumeOrderException {
        Order order = repos.findCompleteById(id);
        if (order.getStatus() != OrderStatus.PAUSED) {
            throw new CannotResumeOrderException("ONLY_PAUSED_ORDER_CAN_BE_RESUMED");
        }
        // Look at all associated JobInfos : they must be at a paused compatible status
        boolean inPause = orderEffectivelyInPause(order);
        if (!inPause) {
            throw new CannotResumeOrderException("ORDER_NOT_COMPLETELY_PAUSED");
        }
        // Passes all ABORTED jobInfo to PENDING
        order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream()).map(FilesTask::getJobInfo)
                .filter(jobInfo -> jobInfo.getStatus().getStatus() == JobStatus.ABORTED)
                .peek(jobInfo -> jobInfo.updateStatus(JobStatus.PENDING)).forEach(jobInfoService::save);
        order.setStatus(OrderStatus.RUNNING);
        repos.save(order);
        // Don't forget to manage user order jobs again (PENDING -> QUEUED)
        orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
    }

    @Override
    public void delete(Long id) throws CannotDeleteOrderException {
        Order order = repos.findCompleteById(id);
        if (order.getStatus() != OrderStatus.PAUSED) {
            throw new CannotDeleteOrderException("ORDER_MUST_BE_PAUSED_BEFORE_BEING_DELETED");
        }
        // Look at all associated JobInfos : they must be at a paused compatible status
        boolean inPause = orderEffectivelyInPause(order);
        if (!inPause) {
            throw new CannotDeleteOrderException("ORDER_NOT_COMPLETELY_STOPPED");
        }
        // Delete all order data files
        dataFileService.removeAll(order.getId());
        // Delete all filesTasks
        for (DatasetTask dsTask : order.getDatasetTasks()) {
            dsTask.getReliantTasks().clear();
        }
        // Deactivate waitingForUser tag
        order.setWaitingForUser(false);
        order.setStatus(OrderStatus.DELETED);
        repos.save(order);
    }

    /**
     * Test if order is truely in pause (ie none of its jobs is running)
     */
    private boolean orderEffectivelyInPause(Order order) {
        // No associated jobInfo or all associated jobs finished
        return (order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .filter(ft -> ft.getJobInfo() != null).count() == 0)
                || order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                        .filter(ft -> ft.getJobInfo() != null).map(ft -> ft.getJobInfo().getStatus().getStatus())
                        .allMatch(JobStatus::isFinished);
    }

    @Override
    public void remove(Long id) throws CannotRemoveOrderException {
        Order order = repos.findCompleteById(id);
        switch (order.getStatus()) {
            case PENDING: // not yet run
                try {
                    self.pause(order.getId());
                } catch (CannotPauseOrderException e) {
                    // Cannot occur because order has pending state
                    throw new RsRuntimeException(e); // NOSONAR
                }
                if (!this.orderEffectivelyInPause(order)) {
                    // Too late !!! order finally wasn't in PENDING state when asked to be paused
                    throw new CannotRemoveOrderException();
                }
                // No break (deliberately) => Java for dumb
            case DONE:
            case DONE_WITH_WARNING:
            case FAILED:
            case PAUSED:
                // Don't forget no relation is hardly mapped between OrderDataFile and Order
                dataFileService.removeAll(order.getId());
                break;
            case DELETED:
            case EXPIRED:
                // data files have already been removed so it only remains order to be removed
                break;
            default:
                // RUNNING needs a pause before being removed
                // REMOVED is a final state (order no more exists, this state is unreachable)
                throw new CannotRemoveOrderException();
        }
        repos.deleteById(order.getId());
    }

    @Override
    public Page<Order> findAll(Pageable pageRequest) {
        return repos.findAllByOrderByCreationDateDesc(pageRequest);
    }

    @Override
    public void writeAllOrdersInCsv(BufferedWriter writer, OrderStatus status, OffsetDateTime from, OffsetDateTime to)
            throws IOException {
        List<Order> orders = repos.findAll(OrderSpecifications.search(status, from, to),
                                           Sort.by(Sort.Direction.ASC, "id"));
        writer.append("ORDER_ID;CREATION_DATE;EXPIRATION_DATE;OWNER;STATUS;STATUS_DATE;PERCENT_COMPLETE;FILES_IN_ERROR");
        writer.newLine();
        for (Order order : orders) {
            writer.append(order.getId().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getCreationDate())).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getExpirationDate())).append(';');
            writer.append(order.getOwner()).append(';');
            writer.append(order.getStatus().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getStatusDate())).append(';');
            writer.append(Integer.toString(order.getPercentCompleted())).append(';');
            writer.append(Integer.toString(order.getFilesInErrorCount()));
            writer.newLine();
        }
        writer.close();
    }

    @Override
    public Page<Order> findAll(String user, Pageable pageRequest, OrderStatus... excludeStatuses) {
        if (excludeStatuses.length == 0) {
            return repos.findAllByOwnerOrderByCreationDateDesc(user, pageRequest);
        } else {
            return repos.findAllByOwnerAndStatusNotInOrderByCreationDateDesc(user, excludeStatuses, pageRequest);
        }
    }

    @Override
    public void downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os) {
        List<OrderDataFile> availableFiles = new ArrayList<>(inDataFiles);
        List<Pair<OrderDataFile, String>> downloadErrorFiles = new ArrayList<>();

        String externalDlErrorPrefix = "Error while downloading external file";
        String storageDlErrorPrefix = "Error while downloading file from Archival Storage";

        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(os)) {
            zos.setEncoding("ASCII");
            zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
            // A multiset to manage multi-occurrences of files
            Multiset<String> dataFiles = HashMultiset.create();
            for (Iterator<OrderDataFile> i = availableFiles.iterator(); i.hasNext();) {
                OrderDataFile dataFile = i.next();
                // Externally downloadable
                if (dataFile.isReference()) {
                    // Connection timeout
                    int timeout = 10_000;
                    String dataObjectIpId = dataFile.getIpId().toString();
                    dataFile.setDownloadError(null);
                    downloadDataFileToZip(downloadErrorFiles, externalDlErrorPrefix, zos, dataFiles, i, dataFile,
                                          timeout, dataObjectIpId);
                } else { // Managed by Storage
                    String aip = dataFile.getIpId().toString();
                    dataFile.setDownloadError(null);
                    Response response = null;
                    try {
                        // To download through storage client we must be authenticate as user in order to
                        // impact the download quotas, but we upgrade the privileges so that the request passes.
                        FeignSecurityManager.asUser(authResolver.getUser(), DefaultRole.PROJECT_ADMIN.name());
                        // To download file with accessrights checked, we should use catalogDownloadClient
                        // but the accessRight have already been checked here.
                        response = storageClient.downloadFile(dataFile.getChecksum());
                    } catch (RuntimeException e) {
                        String stack = getStack(e);
                        LOGGER.error(storageDlErrorPrefix, e);
                        dataFile.setDownloadError(String.format("%s\n%s", storageDlErrorPrefix, stack));
                    } finally {
                        FeignSecurityManager.reset();
                    }
                    // Unable to download file from storage
                    if ((response == null) || (response.status() != HttpStatus.OK.value())) {
                        downloadErrorFiles.add(Pair.of(dataFile, humanizeError(Optional.ofNullable(response))));
                        i.remove();
                        LOGGER.warn("Cannot retrieve data file from storage (aip : {}, checksum : {})", aip,
                                    dataFile.getChecksum());
                        dataFile.setDownloadError("Cannot retrieve data file from storage, feign downloadFile method returns "
                                + (response == null ? "null" : response.toString()));
                    } else { // Download ok
                        try (InputStream is = response.body().asInputStream()) {
                            readInputStreamAndAddToZip(downloadErrorFiles, zos, dataFiles, i, dataFile, aip, is);
                        }
                    }
                }
            }
            if (!downloadErrorFiles.isEmpty()) {
                zos.putArchiveEntry(new ZipArchiveEntry("NOTICE.txt"));
                StringJoiner joiner = new StringJoiner("\n");
                downloadErrorFiles.forEach(p -> joiner.add(String.format("Failed to download file (%s): %s.",
                                                                         p.getLeft().getFilename(), p.getRight())));
                zos.write(joiner.toString().getBytes());
                zos.closeArchiveEntry();
            }
            zos.flush();
            zos.finish();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot create ZIP file.", e);
        }
        // Set statuses of all downloaded files
        availableFiles.forEach(f -> f.setState(FileState.DOWNLOADED));
        // Set statuses of all not downloaded files
        downloadErrorFiles.forEach(f -> f.getLeft().setState(FileState.DOWNLOAD_ERROR));
        // use one set to save everybody
        availableFiles.addAll(downloadErrorFiles.stream().map(Pair::getLeft).collect(Collectors.toList()));
        dataFileService.save(availableFiles);

        processingEventSender.sendDownloadedFilesNotification(availableFiles);

        // Don't forget to manage user order jobs (maybe order is in waitingForUser state)
        orderJobService.manageUserOrderStorageFilesJobInfos(orderOwner);
    }

    protected void downloadDataFileToZip(List<Pair<OrderDataFile, String>> downloadErrorFiles,
            String externalDlErrorPrefix, ZipArchiveOutputStream zos, Multiset<String> dataFiles,
            Iterator<OrderDataFile> i, OrderDataFile dataFile, int timeout, String dataObjectIpId) {
        try (InputStream is = DownloadUtils.getInputStreamThroughProxy(new URL(dataFile.getUrl()), proxy, noProxyHosts,
                                                                       timeout)) {
            readInputStreamAndAddToZip(downloadErrorFiles, zos, dataFiles, i, dataFile, dataObjectIpId, is);
        } catch (IOException e) {
            String stack = getStack(e);
            LOGGER.error(String.format("%s (url : %s)", externalDlErrorPrefix, dataFile.getUrl()), e);
            dataFile.setDownloadError(String.format("%s\n%s", externalDlErrorPrefix, stack));
            downloadErrorFiles.add(Pair.of(dataFile, "I/O error during external download"));
            i.remove();
        }
    }

    private String humanizeError(Optional<Response> response) {
        return response.map(r -> {
            Response.Body body = r.body();
            boolean nullBody = body == null;
            switch (r.status()) {
                case 429:
                    if (nullBody) {
                        return "Download failed due to exceeded quota";
                    }

                    try (InputStream is = body.asInputStream()) {
                        return IOUtils.toString(is, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        LOGGER.debug("I/O error ready response body", e);
                        return "Download failed due to exceeded quota";
                    }
                default:
                    return String.format("Server returned HTTP error code %d", r.status());
            }
        }).orElse("Server returned no content");
    }

    protected String getStack(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void readInputStreamAndAddToZip(List<Pair<OrderDataFile, String>> downloadErrorFiles,
            ZipArchiveOutputStream zos, Multiset<String> dataFiles, Iterator<OrderDataFile> i, OrderDataFile dataFile,
            String dataObjectIpId, InputStream is) throws IOException {
        // Add filename to multiset
        String filename = dataFile.getFilename();
        if (filename == null) {
            filename = dataFile.getUrl().substring(dataFile.getUrl().lastIndexOf('/') + 1);
        }
        dataFiles.add(filename);
        // If same file appears several times, add "(n)" juste before extension (n is occurrence of course
        // you dumb ass ! What do you thing it could be ?)
        int filenameCount = dataFiles.count(filename);
        if (filenameCount > 1) {
            String suffix = " (" + (filenameCount - 1) + ")";
            int lastDotIdx = filename.lastIndexOf('.');
            if (lastDotIdx != -1) {
                filename = filename.substring(0, lastDotIdx) + suffix + filename.substring(lastDotIdx);
            } else { // No extension
                filename += suffix;
            }
        }
        zos.putArchiveEntry(new ZipArchiveEntry(filename));
        long copiedBytes = ByteStreams.copy(is, zos);
        zos.closeArchiveEntry();
        // We can only check copied bytes if we know expected size (ie if file is internal)
        if (dataFile.getFilesize() != null) {
            // Check that file has been completely been copied
            if (copiedBytes != dataFile.getFilesize()) {
                i.remove();
                LOGGER.warn("Cannot completely download data file (data object IP_ID: {}, file name: {})",
                            dataObjectIpId, dataFile.getFilename());
                String downloadError = String
                        .format("Cannot completely download data file from storage, only %d/%d bytes", copiedBytes,
                                dataFile.getFilesize());
                downloadErrorFiles.add(Pair.of(dataFile, downloadError));
                dataFile.setDownloadError(downloadError);
            }
        }
    }

    @Override
    public void downloadOrderMetalink(Long orderId, OutputStream os) {
        Order order = repos.findSimpleById(orderId);
        String tokenRequestParam = ORDER_TOKEN + "=" + generateToken4PublicEndpoint(order);
        String scopeRequestParam = SCOPE + "=" + runtimeTenantResolver.getTenant();

        List<OrderDataFile> files = dataFileService.findAll(orderId);

        // Retrieve host for generating datafiles download urls
        FeignSecurityManager.asSystem();
        Project project = projectClient.retrieveProject(runtimeTenantResolver.getTenant()).getBody().getContent();
        String host = project.getHost();
        FeignSecurityManager.reset();

        // Create XML metalink object
        ObjectFactory factory = new ObjectFactory();
        MetalinkType xmlMetalink = factory.createMetalinkType();
        FilesType xmlFiles = factory.createFilesType();
        // For all data files
        for (OrderDataFile file : files) {
            FileType xmlFile = factory.createFileType();
            String filename = file.getFilename() != null ? file.getFilename()
                    : file.getUrl().substring(file.getUrl().lastIndexOf('/') + 1);
            xmlFile.setIdentity(filename);
            xmlFile.setName(filename);
            if (file.getFilesize() != null) {
                xmlFile.setSize(BigInteger.valueOf(file.getFilesize()));
            }
            if (file.getMimeType() != null) {
                xmlFile.setMimetype(file.getMimeType().toString());
            }
            ResourcesType xmlResources = factory.createResourcesType();
            ResourcesType.Url xmlUrl = factory.createResourcesTypeUrl();
            // Build URL to publicdownloadFile
            StringBuilder buff = new StringBuilder();
            buff.append(host);
            buff.append(urlPrefix).append("/").append(encode4Uri(microserviceName));
            buff.append(OrderControllerEndpointConfiguration.ORDERS_PUBLIC_FILES_MAPPING);
            buff.append("/").append(file.getId()).append("?").append(tokenRequestParam);
            buff.append("&").append(scopeRequestParam);
            xmlUrl.setValue(buff.toString());
            xmlResources.getUrl().add(xmlUrl);
            xmlFile.setResources(xmlResources);
            xmlFiles.getFile().add(xmlFile);
        }
        xmlMetalink.setFiles(xmlFiles);
        createXmlAndSendResponse(os, factory, xmlMetalink);

    }

    private void createXmlAndSendResponse(OutputStream os, ObjectFactory factory, MetalinkType xmlMetalink) {
        // Create XML and send reponse
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MetalinkType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // Format output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Enable validation
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(METALINK_XML_SCHEMA_NAME);
            StreamSource xsdSource = new StreamSource(in);
            jaxbMarshaller
                    .setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdSource));

            // Marshall data
            jaxbMarshaller.marshal(factory.createMetalink(xmlMetalink), os);
            os.close();
        } catch (JAXBException | SAXException | IOException t) {
            LOGGER.error("Error while generating metalink order file", t);
            throw new RsRuntimeException(t);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(fixedDelayString = "${regards.order.computation.update.rate.ms:1000}")
    public void updateCurrentOrdersComputations() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.updateTenantOrdersComputations();
        }
    }

    @Override
    public void updateTenantOrdersComputations() {
        Set<Order> orders = dataFileService.updateCurrentOrdersComputedValues();
        if (!orders.isEmpty()) {
            repos.saveAll(orders);
        }
        // Because previous method (updateCurrentOrdersComputedValues) takes care of CURRENT jobs, it is necessary
        // to update finished ones ie setting availableFilesCount to 0 for finished jobs not waiting for user
        List<Order> finishedOrders = repos.findFinishedOrdersToUpdate();
        if (!finishedOrders.isEmpty()) {
            finishedOrders.forEach(o -> o.setAvailableFilesCount(0));
            repos.saveAll(finishedOrders);
        }
    }

    /**
     * 0 0 7 * * MON-FRI : every working day at 7 AM
     */
    @Override
    @Transactional(propagation = Propagation.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(cron = "${regards.order.periodic.files.availability.check.cron:0 0 7 * * MON-FRI}")
    public void sendPeriodicNotifications() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.sendTenantPeriodicNotifications();
        }
    }

    @Override
    public void sendTenantPeriodicNotifications() {

        List<Order> asideOrders = repos.findAsideOrders(orderSettingsService.getUserOrderParameters().getDelayBeforeEmailNotification());
        Multimap<String, Order> orderMultimap = TreeMultimap.create(Comparator.naturalOrder(), Comparator.comparing(Order::getCreationDate));
        asideOrders.forEach(o -> orderMultimap.put(o.getOwner(), o));

        // For each owner
        for (Map.Entry<String, Collection<Order>> entry : orderMultimap.asMap().entrySet()) {
            OffsetDateTime now = OffsetDateTime.now();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orders", entry.getValue());
            dataMap.put("project", runtimeTenantResolver.getTenant());
            // Create mail
            String message;
            try {
                message = templateService.render(OrderTemplateConf.ASIDE_ORDERS_NOTIFICATION_TEMPLATE_NAME, dataMap);
            } catch (TemplateException e) {
                throw new RsRuntimeException(e);
            }

            // Send it
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(message, "Reminder: some orders are waiting for you", null, entry.getKey());
            FeignSecurityManager.reset();
            // Update order availableUpdateDate to avoid another microservice instance sending notification emails
            entry.getValue().forEach(order -> order.setAvailableUpdateDate(now));
            repos.saveAll(entry.getValue());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(fixedDelayString = "${regards.order.clean.expired.rate.ms:3600000}")
    public void cleanExpiredOrders() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            // In a transaction
            Optional<Order> optional = findOneOrderAndMarkAsExpired();
            while (optional.isPresent()) {
                // in another transaction
                self.cleanExpiredOrder(optional.get());
                // and again
                optional = findOneOrderAndMarkAsExpired();
            }
        }
    }

    @Override
    public Optional<Order> findOneOrderAndMarkAsExpired() {
        Optional<Order> optional = repos.findOneExpiredOrder();
        optional.ifPresent(order -> {
            order.setStatus(OrderStatus.EXPIRED);
            repos.save(order);
        });
        return optional;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    // No transaction because :
    // - loadComplete use a new one and so when delete is called, order state is at start of transaction (so with state
    // EXPIRED)
    // - loadComplete needs a new transaction each time it is called. If nothing is specified, it seems that the same
    // transaction is used each time loadComplete is called (I think it is due to Hibernate Flush mode set as NEVER
    // specified by Spring so it is cached in first level)
    public void cleanExpiredOrder(Order order) {
        // Ask for all jobInfos abortion (don't call self.pause() because of status, order must stay EXPIRED)
        order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream()).map(FilesTask::getJobInfo)
                .forEach(jobInfo -> jobInfoService.stopJob(jobInfo.getId()));

        // Wait for its complete stop
        order = self.loadComplete(order.getId());
        while (!orderEffectivelyInPause(order)) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RsRuntimeException(e); // NOSONAR
            }
            order = self.loadComplete(order.getId());
        }
        // Delete all its data files
        // Don't forget no relation is hardly mapped between OrderDataFile and Order
        dataFileService.removeAll(order.getId());
        // Delete all filesTasks
        for (DatasetTask dsTask : order.getDatasetTasks()) {
            dsTask.getReliantTasks().clear();
        }
        // Deactivate waitingForUser tag
        order.setWaitingForUser(false);
        // Order is already at EXPIRED state so let it be
        repos.save(order);
    }

    @Override
    public boolean isPaused(Long orderId) {
        Order order = loadComplete(orderId);
        if (order.getStatus() != OrderStatus.PAUSED) {
            return false;
        }
        return this.orderEffectivelyInPause(order);
    }

    @Override
    public boolean hasProcessing(Order order) {
        return order.getDatasetTasks().stream().anyMatch(DatasetTask::hasProcessing);
    }

}
