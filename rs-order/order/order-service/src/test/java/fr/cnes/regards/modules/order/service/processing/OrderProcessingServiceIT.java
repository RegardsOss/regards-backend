package fr.cnes.regards.modules.order.service.processing;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.OrderServiceTestIT;
import fr.cnes.regards.modules.order.test.SearchClientMock;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.order.test.StorageClientMock;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.dto.*;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.order.*;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles(value = {"default", "test", "testAmqp"}, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.default_schema=order_processing_test_it",
    "regards.amqp.enabled=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE",
    "logging.level.org.springframework.transaction=TRACE",
    //"spring.jpa.show-sql=true",
    //"spring.jpa.properties.hibernate.format_sql=true",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class OrderProcessingServiceIT extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);

    private static final UUID PROCESS_BUSINESS_ID = UUID.randomUUID();
    private static final java.util.Map<String, String> PARAMETERS = HashMap.of("param", "value").toJavaMap();
    private static final OrderProcessInfoMapper MAPPER  = new OrderProcessInfoMapper();
    private static final OrderProcessInfo PROCESS_INFO = new OrderProcessInfo(
            Scope.ITEM,
            Cardinality.ONE_PER_EXECUTION,
            List.of(DataType.RAWDATA),
            new SizeLimit(SizeLimit.Type.FILES, 1L)
    );

    @Autowired
    private IBasketDatasetSelectionRepository dsSelRepo;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderRepository orderRepos;
    @Autowired
    private IOrderDataFileRepository dataFileRepos;
    @Autowired
    private IOrderDataFileService dataFileService;
    @Autowired
    private IBasketRepository basketRepos;
    @Autowired
    private IJobInfoRepository jobInfoRepos;
    @Autowired
    private IAuthenticationResolver authResolver;
    @Autowired
    private IProjectsClient projectsClient;
    @Autowired
    private IRuntimeTenantResolver tenantResolver;
    @Autowired
    private StorageClientMock storageClientMock;
    @Autowired
    private IProcessingRestClient processingClient;
    @Autowired
    private IProcessingEventSender processingEventSender;
    @Autowired
    private IPublisher publisher;

    public void clean() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();
        jobInfoRepos.deleteAll();
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setWaitMode(false);

        //clean();
        when(authResolver.getRole()).thenAnswer(i -> {
            LOGGER.info("Asking for role");
            return DefaultRole.REGISTERED_USER.toString();
        });
        Project project = new Project();
        project.setHost("regardsHost");
        when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(project), HttpStatus.OK));
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    @Test
    public void simpleOrderWithProcess() throws InterruptedException, EntityInvalidException, ExecutionException {
        // GIVEN
        tenantResolver.forceTenant(getDefaultTenant());
        String tenant = tenantResolver.getTenant();

        when(processingClient.findByUuid(PROCESS_BUSINESS_ID.toString())).thenAnswer(i -> {
            return new ResponseEntity<>(new PProcessDTO(
                    PROCESS_BUSINESS_ID,
                    "the-process-name",
                    true,
                    MAPPER.toMap(PROCESS_INFO),
                    List.of(new ExecutionParamDTO("the-param-name", ExecutionParameterType.STRING, "The param desc"))
            ), HttpStatus.OK);
        });
        java.util.Map<UUID, String> batchCorrelations = new java.util.HashMap<>();
        when(processingClient.createBatch(any())).thenAnswer(i -> {
            PBatchRequest req = i.getArgument(0);
            String correlationId = req.getCorrelationId();
            UUID batchId = UUID.randomUUID();
            batchCorrelations.put(batchId, correlationId);
            return new ResponseEntity<>(new PBatchResponse(batchId, correlationId), HttpStatus.OK);
        });
        when(processingEventSender.sendProcessingRequest(any())).thenAnswer(i -> {
            PExecutionRequestEvent requestEvent = i.getArgument(0);
            new Thread(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                Try.run(() -> Thread.sleep(50L));
                UUID executionId = UUID.randomUUID();
                File outFile = new File("src/test/resources/processing/execResult");
                String absolutePath = outFile.getAbsolutePath();
                publisher.publish(new PExecutionResultEvent(
                    executionId,
                    requestEvent.getExecutionCorrelationId(),
                    requestEvent.getBatchId(),
                    batchCorrelations.get(requestEvent.getBatchId()),
                    PROCESS_BUSINESS_ID,
                    MAPPER.toMap(PROCESS_INFO),
                    ExecutionStatus.SUCCESS,
                    List.of(new POutputFileDTO(
                        UUID.randomUUID(),
                        executionId,
                        Try.of(() -> outFile.toURI().toURL()).get(),
                        "execResult",
                        17L,
                        "MD5",
                        Try.of(() -> Files.hash(outFile, Hashing.md5()).toString()).get(),
                        requestEvent.getInputFiles().map(PInputFile::getInputCorrelationId).toList()
                    )),
                    List.empty()
                ));
            }).start();
            return Try.success(requestEvent);
        });


        String orderOwner = randomLabel("simpleOrder");
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        BasketDatedItemsSelection itemSelection = createDatasetItemSelection(1L, 12, 3, "ALL");
        dsSelection.addItemsSelection(itemSelection);
        dsSelection.setProcessDatasetDescription(new ProcessDatasetDescription(PROCESS_BUSINESS_ID, PARAMETERS));
        basket.addDatasetSelection(dsSelection);

        saveBasket(getDefaultTenant(), basket);

        // WHEN
        Order order = orderService.createOrder(basket, orderOwner, "http://frontend.com");

        // THEN
        LOGGER.info("Order has been created !!");
        // Wait order ends.
        int loop = 0;
        while (!orderService.loadComplete(order.getId()).getStatus().equals(OrderStatus.DONE) && (loop < 10)) {
            Thread.sleep(5_000);
            loop++;
        }
        Assert.assertEquals(OrderStatus.DONE, orderService.loadComplete(order.getId()).getStatus());
        LOGGER.info("Order is done !!");

        Thread.sleep(10_000L);
    }

    @MultitenantTransactional
    private Basket saveBasket(String tenant, Basket basket) {
        runtimeTenantResolver.forceTenant(tenant);
        Basket savedBasket = basketRepos.saveAndFlush(basket);
        return savedBasket;
    }

    private String randomLabel(String prefix) {
        return prefix + "_" + Long.toHexString(new Random().nextLong());
    }

    private BasketDatedItemsSelection createDatasetItemSelection(long filesSize, long filesCount, int objectsCount,
                                                                 String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }
}