package fr.cnes.regards.modules.order.service;

import javax.transaction.Transactional;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.xml.sax.SAXException;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.ByteStreams;
import feign.Response;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.metalink.schema.FileType;
import fr.cnes.regards.modules.order.metalink.schema.FilesType;
import fr.cnes.regards.modules.order.metalink.schema.MetalinkType;
import fr.cnes.regards.modules.order.metalink.schema.ObjectFactory;
import fr.cnes.regards.modules.order.metalink.schema.ResourcesType;
import fr.cnes.regards.modules.order.service.job.ExpirationDateJobParameter;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.order.service.job.UserJobParameter;
import fr.cnes.regards.modules.order.service.job.UserRoleJobParameter;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.ISearchClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.templates.service.TemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
@RefreshScope
public class OrderService implements IOrderService, ApplicationListener<RefreshScopeRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private static final String METALINK_XML_SCHEMA_NAME = "metalink.xsd";

    public static final int MAX_PAGE_SIZE = 10_000;

    @Autowired
    private IOrderRepository repos;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private ISearchClient searchClient;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IProjectsClient projectClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IOrderService self;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private INotificationClient notificationClient;

    @Value("${regards.order.files.bucket.size.Mb:100}")
    private int bucketSizeMb;

    @Value("${regards.order.validation.period.days:3}")
    private int orderValidationPeriodDays;

    @Value("${regards.order.days.before.considering.order.as.aside:7}")
    private int daysBeforeSendingNotifEmail;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.order.secret}")
    private String secret;

    @Value("${zuul.prefix}")
    private String urlPrefix;

    @Autowired
    private IEmailClient emailClient;

    private Long bucketSize = null;

    /**
     * Set of DataTypes to retrieve on DataObjects
     */
    private static final Set<DataType> DATA_TYPES = Stream.of(DataTypeSelection.ALL.getFileTypes())
            .map(DataType::valueOf).collect(Collectors.toSet());

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        LOGGER.info("OrderService refreshed...");
    }


    @Override
    public Order createOrder(Basket basket, String url) {
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(orderValidationPeriodDays, ChronoUnit.DAYS));
        order.setOwner(basket.getOwner());
        order.setFrontendUrl(url);
        order.setStatus(OrderStatus.PENDING);
        // To generate orderId
        order = repos.save(order);
        int priority = orderJobService.computePriority(order.getOwner(), authResolver.getRole());

        // Be careful ! bucketSize must be computed AFTER bucketSizeMb is filled by Spring
        if (bucketSize == null) {
            bucketSize = bucketSizeMb * 1024l * 1024l;
        }
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
                            // Only DataFile with a size (and != 0) are taken into account (others are not managed by
                            // rs-storage hence cannot be ordered)
                            if ((file.getSize() != null) && (file.getSize().longValue() != 0l)) {
                                OrderDataFile orderDataFile = new OrderDataFile(file, object.getIpId(), order.getId());
                                dataFileService.save(orderDataFile);
                                bucketFiles.add(orderDataFile);
                                // Send a very useful notification if file is bigger than bucket size
                                if (orderDataFile.getSize() > bucketSize) {
                                    FeignSecurityManager.asSystem();
                                    NotificationDTO notif = new NotificationDTO(
                                            String.format("File \"%s\" is bigger than sub-order size",
                                                          orderDataFile.getName()), Collections.emptySet(),
                                            Collections.singleton(DefaultRole.PROJECT_ADMIN.name()), microserviceName,
                                            "Order creation", NotificationType.WARNING);
                                    notificationClient.createNotification(notif);
                                    FeignSecurityManager.reset();
                                }
                            }
                        }
                    }
                    // If sum of files size > bucketSize, add a new bucket
                    if (bucketFiles.stream().mapToLong(DataFile::getSize).sum() >= bucketSize) {
                        createSubOrder(basket, dsTask, bucketFiles, order, priority);

                        bucketFiles.clear();
                    }
                }
                page++;
                objects = searchDataObjects(dsSel.getOpenSearchRequest(), page);
            }
            // Manage remaining files
            if (!bucketFiles.isEmpty()) {
                createSubOrder(basket, dsTask, bucketFiles, order, priority);
            }

            order.addDatasetOrderTask(dsTask);
        }
        // Order is ready to be taken into account
        order.setStatus(OrderStatus.RUNNING);
        order = repos.save(order);
        try {
            sendOrderCreationEmail(order);
        } catch (Exception e) {
            LOGGER.warn("Error while attempting to send order creation email (order has been created anyway)", e);
        }
        orderJobService.manageUserOrderJobInfos(order.getOwner());
        return order;
    }

    /**
     * Generate a token containing orderId and expiration date to be used with public download URL (of metalink file and
     * order data files)
     */
    private String generateToken4PublicEndpoint(Order order) {
        return jwtService
                .generateToken(runtimeTenantResolver.getTenant(), authResolver.getUser(), authResolver.getRole(),
                               order.getExpirationDate(),
                               Collections.singletonMap(ORDER_ID_KEY, order.getId().toString()), secret, true);
    }

    private void sendOrderCreationEmail(Order order) {
        // Generate token
        String tokenRequestParam = ORDER_TOKEN + "=" + generateToken4PublicEndpoint(order);

        FeignSecurityManager.asSystem();
        Project project = projectClient.retrieveProject(runtimeTenantResolver.getTenant()).getBody().getContent();
        String host = project.getHost();
        FeignSecurityManager.reset();

        String urlStart = host + urlPrefix + "/" + encode4Uri(microserviceName);

        // Metalink file public url
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("expiration_date", order.getExpirationDate().toString());
        dataMap.put("project", runtimeTenantResolver.getTenant());
        dataMap.put("metalink_download_url",
                    urlStart + "/user/orders/metalink/download?" + tokenRequestParam + "&scope=" + runtimeTenantResolver
                            .getTenant());
        dataMap.put("regards_downloader_url", "https://github.com/RegardsOss/RegardsDownloader/releases");
        dataMap.put("orders_url", host + order.getFrontendUrl());

        // Create mail
        SimpleMailMessage email;
        try {
            email = templateService
                    .writeToEmail(TemplateServiceConfiguration.ORDER_CREATED_TEMPLATE_CODE, dataMap, order.getOwner());
        } catch (EntityNotFoundException e) {
            throw new RsRuntimeException(e);
        }

        // Send it
        FeignSecurityManager.asSystem();
        emailClient.sendEmail(email);
        FeignSecurityManager.reset();
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
    private void createSubOrder(Basket basket, DatasetTask dsTask, Set<OrderDataFile> bucketFiles, Order order,
            int priority) {
        OffsetDateTime expirationDate = order.getExpirationDate();
        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(order.getId());
        currentFilesTask.setOwner(order.getOwner());
        currentFilesTask.addAllFiles(bucketFiles);

        // storageJobInfo is pointed by currentFilesTask so it must be locked to avoid being cleaned before FilesTask
        JobInfo storageJobInfo = new JobInfo(true);
        storageJobInfo.setParameters(new FilesJobParameter(bucketFiles.toArray(new OrderDataFile[bucketFiles.size()])),
                                     new ExpirationDateJobParameter(expirationDate),
                                     new UserJobParameter(authResolver.getUser()),
                                     new UserRoleJobParameter(authResolver.getRole()));
        storageJobInfo.setOwner(basket.getOwner());
        storageJobInfo.setClassName("fr.cnes.regards.modules.order.service.job.StorageFilesJob");
        storageJobInfo.setPriority(priority);
        storageJobInfo.setExpirationDate(order.getExpirationDate());
        // Create JobInfo and associate to FilesTask
        currentFilesTask.setJobInfo(jobInfoService.createAsPending(storageJobInfo));
        dsTask.addReliantTask(currentFilesTask);
    }

    private List<DataObject> searchDataObjects(String openSearchRequest, int page) {
        Map<String, String> requestMap = Collections.singletonMap("q", openSearchRequest);

        ResponseEntity<PagedResources<Resource<DataObject>>> pagedResourcesResponseEntity = searchClient
                .searchDataobjects(requestMap, page, MAX_PAGE_SIZE);
        return pagedResourcesResponseEntity.getBody().getContent().stream().map(r -> r.getContent())
                .collect(Collectors.toList());
    }

    @Override
    public Order loadSimple(Long id) {
        return repos.findSimpleById(id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
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
                .forEach(jobInfo -> jobInfoService.stopJob(jobInfo.getId()));
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
        orderJobService.manageUserOrderJobInfos(order.getOwner());
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
        return order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(ft -> ft.getJobInfo().getStatus().getStatus()).allMatch(JobStatus::isFinished);
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
        repos.delete(order.getId());
    }

    @Override
    public Page<Order> findAll(Pageable pageRequest) {
        return repos.findAllByOrderByCreationDateDesc(pageRequest);
    }

    @Override
    public void writeAllOrdersInCsv(BufferedWriter writer) throws IOException {
        List<Order> orders = repos.findAll();
        writer.append(
                "ORDER_ID;CREATION_DATE;EXPIRATION_DATE;OWNER;STATUS;STATUS_DATE;PERCENT_COMPLETE;FILES_IN_ERROR");
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
    public void downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os)
            throws IOException {
        List<OrderDataFile> availableFiles = new ArrayList<>(inDataFiles);
        List<OrderDataFile> downloadErrorFiles = new ArrayList<>();

        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            // A multiset to manage multi-occurrences of files
            Multiset<String> dataFiles = HashMultiset.create();
            for (Iterator<OrderDataFile> i = availableFiles.iterator(); i.hasNext(); ) {
                OrderDataFile dataFile = i.next();
                String aip = dataFile.getIpId().toString();
                Response response = null;
                try {
                    response = aipClient.downloadFile(aip, dataFile.getChecksum());
                } catch (RuntimeException e) {
                    LOGGER.error("Error while downloading file from Archival Storage", e);
                }
                // Unable to download file from storage
                if ((response == null) || (response.status() != HttpStatus.OK.value())) {
                    downloadErrorFiles.add(dataFile);
                    i.remove();
                    LOGGER.warn("Cannot retrieve data file from storage (aip : {}, checksum : {})", aip,
                                dataFile.getChecksum());
                    continue;
                } else { // Download ok
                    try (InputStream is = response.body().asInputStream()) {
                        // Add filename to multiset
                        String filename = dataFile.getName();
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
                        zos.putNextEntry(new ZipEntry(filename));
                        long copiedBytes = ByteStreams.copy(is, zos);
                        zos.closeEntry();
                        // Check that file has been completely been copied
                        if (copiedBytes != dataFile.getSize()) {
                            downloadErrorFiles.add(dataFile);
                            i.remove();
                            LOGGER.warn("Cannot completely retrieve data file from storage (aip : {}, checksum : {})",
                                        aip, dataFile.getChecksum());
                        }
                    }
                }
            }
            zos.flush();
            zos.finish();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot create ZIP file.", e);
        }
        // Set statuses of all downloaded files
        availableFiles.forEach(f -> f.setState(FileState.DOWNLOADED));
        // Set statuses of all not downloaded files
        downloadErrorFiles.forEach(f -> f.setState(FileState.DOWNLOAD_ERROR));
        // use one set to save everybody
        availableFiles.addAll(downloadErrorFiles);
        dataFileService.save(availableFiles);

        // Don't forget to manage user order jobs (maybe order is in waitingForUser state)
        orderJobService.manageUserOrderJobInfos(orderOwner);
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
            xmlFile.setIdentity(file.getName());
            xmlFile.setSize(BigInteger.valueOf(file.getSize()));
            if (file.getMimeType() != null) {
                xmlFile.setMimetype(file.getMimeType().toString());
            }
            ResourcesType xmlResources = factory.createResourcesType();
            ResourcesType.Url xmlUrl = factory.createResourcesTypeUrl();
            // Build URL to publicdownloadFile
            StringBuilder buff = new StringBuilder();
            buff.append(host);
            buff.append(urlPrefix).append("/").append(encode4Uri(microserviceName));
            buff.append("/orders/aips/").append(encode4Uri(file.getIpId().toString())).append("/files/");
            buff.append(file.getChecksum()).append("?").append(tokenRequestParam);
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

    private static String encode4Uri(String str) {
        try {
            return new String(UriUtils.encode(str, Charset.defaultCharset().name()).getBytes(),
                              StandardCharsets.US_ASCII);
        } catch (UnsupportedEncodingException e) {
            // Will never occurs
            throw new RsRuntimeException(e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.NEVER) // Must not create a transaction, it is a multitenant method
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
            repos.save(orders);
        }
        // Because previous method (updateCurrentOrdersComputedValues) takes care of CURRENT jobs, it is necessary
        // to update finished ones ie setting availableFilesCount to 0 for finished jobs not waiting for user
        List<Order> finishedOrders = repos.findFinishedOrdersToUpdate();
        if (!finishedOrders.isEmpty()) {
            finishedOrders.forEach(o -> o.setAvailableFilesCount(0));
            repos.save(finishedOrders);
        }
    }

    /**
     * 0 0 7 * * MON-FRI : every working day at 7 AM
     */
    @Override
    @Transactional(Transactional.TxType.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(cron = "${regards.order.periodic.files.availability.check.cron:0 0 7 * * MON-FRI}")
    public void sendPeriodicNotifications() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.sendTenantPeriodicNotifications();
        }
    }

    @Override
    public void sendTenantPeriodicNotifications() {
        List<Order> asideOrders = repos.findAsideOrders(daysBeforeSendingNotifEmail);

        Multimap<String, Order> orderMultimap = TreeMultimap
                .create(Comparator.naturalOrder(), Comparator.comparing(Order::getCreationDate));
        asideOrders.forEach(o -> orderMultimap.put(o.getOwner(), o));

        // For each owner
        for (Map.Entry<String, Collection<Order>> entry : orderMultimap.asMap().entrySet()) {
            OffsetDateTime now = OffsetDateTime.now();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orders", entry.getValue());
            dataMap.put("project", runtimeTenantResolver.getTenant());
            // Create mail
            SimpleMailMessage email;
            try {
                email = templateService
                        .writeToEmail(TemplateServiceConfiguration.ASIDE_ORDERS_NOTIFICATION_TEMPLATE_CODE, dataMap,
                                      entry.getKey());
            } catch (EntityNotFoundException e) {
                throw new RsRuntimeException(e);
            }

            // Send it
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(email);
            FeignSecurityManager.reset();
            // Update order availableUpdateDate to avoid another microservice instance sending notification emails
            entry.getValue().forEach(order -> order.setAvailableUpdateDate(now));
            repos.save(entry.getValue());
        }
    }

    @Override
    @Transactional(Transactional.TxType.NEVER) // Must not create a transaction, it is a multitenant method
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
    @Transactional(Transactional.TxType.NEVER)
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

}
