package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.ByteStreams;
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
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
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
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.metalink.schema.FileType;
import fr.cnes.regards.modules.order.metalink.schema.FilesType;
import fr.cnes.regards.modules.order.metalink.schema.MetalinkType;
import fr.cnes.regards.modules.order.metalink.schema.ObjectFactory;
import fr.cnes.regards.modules.order.metalink.schema.ResourcesType;
import fr.cnes.regards.modules.order.service.job.ExpirationDateJobParameter;
import fr.cnes.regards.modules.order.service.job.FilesJobParameter;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.templates.service.TemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderService implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private static final String METALINK_XML_SCHEMA_NAME = "metalink.xsd";

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

    @Value("${regards.order.files.bucket.size.Mb:100}")
    private int bucketSizeMb;

    @Value("${regards.order.validation.period.days:3}")
    private int orderValidationPeriodDays;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.order.secret}")
    private String secret;

    @Autowired
    private IEmailClient emailClient;

    private final long bucketSize = bucketSizeMb * 1024l * 1024l;

    /**
     * Set of DataTypes to retrieve on DataObjects
     */
    private final Set<DataType> DATA_TYPES = Stream.of(DataTypeSelection.ALL.getFileTypes()).map(DataType::valueOf)
            .collect(Collectors.toSet());

    @Override
    public Order createOrder(Basket basket) {
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(orderValidationPeriodDays, ChronoUnit.DAYS));
        order.setOwner(basket.getOwner());
        order.setStatus(OrderStatus.PENDING);
        // To generate orderId
        order = repos.save(order);
        int priority = orderJobService.computePriority(order.getOwner(), authResolver.getRole());

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
                            OrderDataFile orderDataFile = new OrderDataFile(file, object.getIpId(), order.getId());
                            dataFileService.save(orderDataFile);
                            bucketFiles.add(orderDataFile);
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
        // Order is ready to be taken into account
        order.setStatus(OrderStatus.RUNNING);
        order = repos.save(order);
        sendOrderCreationEmail(order);
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

        // FIXME => Sylvain Vessière Guerinet (cf. OpenSearchDescriptionBuilder)
        String urlStart = host + "/api/v2/" + encode4Uri(microserviceName);

        // Metalink file public url
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("expiration_date", order.getExpirationDate().toString());
        dataMap.put("metalink_download_url", urlStart + "/user/orders/metalink/download?" + tokenRequestParam);
        dataMap.put("regards_downloader_url", "http://perdu.com");
        dataMap.put("orders_url", urlStart + "/orders");

        // Create mail
        SimpleMailMessage email;
        try {
            email = templateService
                    .writeToEmail(TemplateServiceConfiguration.ORDER_CREATED_TEMPLATE_CODE, dataMap, order.getOwner());
        } catch (EntityNotFoundException e) {
            throw new RuntimeException(e);
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
    public void pause(Long id) {
        Order order = repos.findCompleteById(id);
        // Ask for all jobInfos abortion
        order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream()).map(FilesTask::getJobInfo)
                .forEach(jobInfo -> jobInfoService.stopJob(jobInfo.getId()));
        order.setStatus(OrderStatus.PAUSED);
        repos.save(order);
    }

    @Override
    public void resume(Long id) throws CannotResumeOrderException {
        Order order = repos.findCompleteById(id);
        // Look at all associated JobInfos : they must be at a paused compatible status
        boolean inPause = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(ft -> ft.getJobInfo().getStatus().getStatus()).allMatch(JobStatus::isCompatibleWithPause);
        if (!inPause) {
            throw new CannotResumeOrderException();
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
            throw new CannotDeleteOrderException(
                    String.format("An order must be paused before being deleted (current status is %s).",
                                  order.getStatus()));
        }
        // Look at all associated JobInfos : they must be at a paused compatible status
        boolean inPause = order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(ft -> ft.getJobInfo().getStatus().getStatus()).allMatch(JobStatus::isCompatibleWithPause);
        if (!inPause) {
            throw new CannotDeleteOrderException(
                    "Order is not completely stopped, some tasks are still running, please "
                            + "wait a while and retry later");
        }
        // Delete all order data files
        dataFileService.removeAll(order.getId());
        order.setStatus(OrderStatus.DELETED);
        repos.save(order);
    }

    @Override
    public void remove(Long id) throws CannotRemoveOrderException {
        Order order = repos.findCompleteById(id);
        if (order.getStatus() != OrderStatus.DELETED) {
            throw new CannotRemoveOrderException(
                    String.format("An order must be deleted before being removed (current status is %s).",
                                  order.getStatus()));
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
    public Page<Order> findAll(String user, Pageable pageRequest) {
        return repos.findAllByOwnerOrderByCreationDateDesc(user, pageRequest);
    }

    @Override
    public void downloadOrderCurrentZip(List<OrderDataFile> inDataFiles, OutputStream os) throws IOException {
        List<OrderDataFile> availableFiles = new ArrayList<>(inDataFiles);
        List<OrderDataFile> inErrorFiles = new ArrayList<>();

        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            // A multiset to manage multi-occurrences of files
            Multiset<String> dataFiles = HashMultiset.create();
            for (Iterator<OrderDataFile> i = availableFiles.iterator(); i.hasNext(); ) {
                OrderDataFile dataFile = i.next();
                String aip = dataFile.getIpId().toString();
                try (InputStream is = aipClient.downloadFile(aip, dataFile.getChecksum()).body().asInputStream()) {
                    // If storage cannot provide file
                    if (is == null) {
                        inErrorFiles.add(dataFile);
                        i.remove();
                        LOGGER.warn(
                                String.format("Cannot retrieve data file from storage (aip : %s, checksum : %s)", aip,
                                              dataFile.getChecksum()));
                        continue;
                    }
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
                    ByteStreams.copy(is, zos);
                    zos.closeEntry();
                }
            }
            zos.flush();
            zos.finish();
        }
        availableFiles.forEach(f -> f.setState(FileState.DOWNLOADED));
        inErrorFiles.forEach(f -> f.setState(FileState.ERROR));
        availableFiles.addAll(inErrorFiles);
        dataFileService.save(availableFiles);
    }

    @Override
    public void downloadOrderMetalink(Long orderId, OutputStream os) {
        Order order = repos.findSimpleById(orderId);
        String tokenRequestParam = ORDER_TOKEN + "=" + generateToken4PublicEndpoint(order);

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
            // FIXME => Sylvain Vessière Guerinet (cf. OpenSearchDescriptionBuilder)
            buff.append("/api/v2/").append(encode4Uri(microserviceName));
            buff.append("/orders/aips/").append(encode4Uri(file.getIpId().toString())).append("/files/");
            buff.append(file.getChecksum()).append("?").append(tokenRequestParam);
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
        } catch (Throwable t) {
            LOGGER.error("Error while generating metalink order file", t);
            throw new RuntimeException(t);
        }
    }

    private static String encode4Uri(String str) {
        try {
            return new String(UriUtils.encode(str, Charset.defaultCharset().name()).getBytes(),
                              StandardCharsets.US_ASCII);
        } catch (UnsupportedEncodingException e) {
            // Will never occurs
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(fixedDelayString = "${regards.order.completion.update.rate.ms:1000}")
    public void updateCurrentOrdersCompletions() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.updateTenantCurrentOrdersCompletions();
        }
    }

    @Override
    public void updateTenantCurrentOrdersCompletions() {
        Set<Order> orders = dataFileService.updateCurrentOrdersCompletionValues();
        if (!orders.isEmpty()) {
            repos.save(orders);
        }

    }
}
