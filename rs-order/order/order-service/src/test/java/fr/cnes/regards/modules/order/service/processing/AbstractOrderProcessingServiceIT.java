/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.processing;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.*;
import fr.cnes.regards.modules.order.service.commons.OrderCreationCompletedEventHandler;
import fr.cnes.regards.modules.order.service.job.ProcessExecutionJob;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.test.SearchClientMock;
import fr.cnes.regards.modules.order.test.StorageClientMock;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.dto.*;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@ContextConfiguration(classes = ProcessingServiceConfiguration.class)
@TestPropertySource(
    properties = { "regards.amqp.enabled=true", "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE",
        "logging.level.org.springframework.transaction=TRACE", "regards.order.files.bucket.size.Mb=50",
        // We regulate the suborder sizes with process info limits
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractOrderProcessingServiceIT extends AbstractMultitenantServiceIT {

    protected static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTestIT.class);

    @Autowired
    protected IBasketDatasetSelectionRepository dsSelRepo;

    @Autowired
    protected IOrderService orderService;

    @Autowired
    protected IOrderDownloadService orderDownloadService;

    @Autowired
    protected IOrderRepository orderRepos;

    @Autowired
    protected IOrderDataFileRepository dataFileRepos;

    @Autowired
    protected IOrderDataFileService dataFileService;

    @Autowired
    protected IBasketRepository basketRepos;

    @Autowired
    protected IJobInfoRepository jobInfoRepos;

    @Autowired
    protected IAuthenticationResolver authResolver;

    @Autowired
    protected IProjectsClient projectsClient;

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    @Autowired
    protected StorageClientMock storageClientMock;

    @Autowired
    protected IProcessingRestClient processingClient;

    @Autowired
    protected IProcessingEventSender processingEventSender;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    protected TaskExecutor taskExecutor;

    @Autowired
    protected ExecResultHandlerResultEventHandler execResultHandlerResultEventHandler;

    @Autowired
    protected OrderCreationCompletedEventHandler orderCreationCompletedEventHandler;

    @Autowired
    protected IBasketService basketService;

    @MockBean
    protected IProjectUsersClient projectUsersClient;

    // Mutable map to hold the correspondence between batch ID and batch correlations ID
    protected java.util.Map<UUID, String> batchCorrelations = new java.util.HashMap<>();

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        clean();

        storageClientMock.setWaitMode(false);

        //clean();
        when(authResolver.getRole()).thenAnswer(i -> {
            LOGGER.info("Asking for role");
            return DefaultRole.REGISTERED_USER.toString();
        });
        Project project = new Project();
        project.setHost("regardsHost");
        when(projectsClient.retrieveProject(Mockito.anyString())).thenReturn(new ResponseEntity<>(EntityModel.of(project),
                                                                                                  HttpStatus.OK));
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    protected void clean() {
        basketRepos.deleteAll();
        dsSelRepo.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();
        jobInfoRepos.deleteAll();
        execResultHandlerResultEventHandler.clear();
        orderCreationCompletedEventHandler.clear();
    }

    protected void showMetalink(Order order) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            orderDownloadService.downloadOrderMetalink(order.getId(), out);
            out.flush();
            LOGGER.info("Metalink:\n>>>\n{}\n<<<", out);
        }
    }

    protected void assertProcessingEventSizes(int expectedExecutions,
                                              ProcessingMock processingMock,
                                              Long storageJobsSuccessBefore,
                                              Long execJobsSuccessBefore) {
        assertThat(processingMock.getExecRequestEvents()).hasSize(expectedExecutions);

        Long storageJobsSuccessAfter = jobInfoRepos.countByClassNameAndStatusStatusIn(StorageFilesJob.class.getName(),
                                                                                      JobStatus.SUCCEEDED);
        Long execJobsSuccessAfter = jobInfoRepos.countByClassNameAndStatusStatusIn(ProcessExecutionJob.class.getName(),
                                                                                   JobStatus.SUCCEEDED);
        assertThat(storageJobsSuccessAfter - storageJobsSuccessBefore).isEqualTo(expectedExecutions);
        assertThat(execJobsSuccessAfter - execJobsSuccessBefore).isEqualTo(expectedExecutions);

    }

    protected void awaitLatches(CountDownLatch orderCreatedLatch, CountDownLatch receivedExecutionResultsLatch)
        throws InterruptedException {
        if (!orderCreatedLatch.await(5, TimeUnit.MINUTES)) {
            fail("Did not create the order in a large amount of time.");
        }
        LOGGER.info("Order has been created !!");

        if (!receivedExecutionResultsLatch.await(5L, TimeUnit.MINUTES)) {
            fail("Did not reach the expected number of treated execution results in a large amount of time.");
        }

        //assertThat(orderService.loadComplete(order.getId()).getStatus()).isEqualTo(OrderStatus.DONE);
    }

    protected void setupMocksAndHandlers(UUID processBusinessId,
                                         OrderProcessInfoMapper processInfoMapper,
                                         OrderProcessInfo processInfo,
                                         String defaultTenant,
                                         ProcessingMock processingMock,
                                         AtomicInteger sendProcessingRequestCallCount,
                                         String orderOwner,
                                         CountDownLatch orderCreatedLatch,
                                         CountDownLatch receivedExecutionResultsLatch) {
        Mockito.reset(processingEventSender);

        setUpProcessingClient(processBusinessId, processInfoMapper, processInfo);

        when(processingEventSender.sendProcessingRequest(any())).thenAnswer(i -> {
            sendProcessingRequestCallCount.incrementAndGet();
            PExecutionRequestEvent requestEvent = i.getArgument(0);
            return processingMock.dealWithEvent(defaultTenant,
                                                requestEvent,
                                                processBusinessId,
                                                processInfo,
                                                processInfoMapper);
        });

        orderCreationCompletedEventHandler.setConsumer(order -> orderCreatedLatch.countDown());

        execResultHandlerResultEventHandler.setConsumer(evt -> {
            // When a file is available, simulate a download for this file by calling downloadOrderCurrentZip
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                List<OrderDataFile> updatedOrderDataFiles = evt.getUpdatedOrderDataFiles();
                LOGGER.info("Downloading {}", updatedOrderDataFiles);
                orderDownloadService.downloadOrderCurrentZip(orderOwner, updatedOrderDataFiles.asJava(), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            receivedExecutionResultsLatch.countDown();
        });
    }

    protected void setUpProcessingClient(UUID processBusinessId,
                                         OrderProcessInfoMapper processInfoMapper,
                                         OrderProcessInfo processInfo) {
        Mockito.reset(processingClient);

        when(processingClient.findByUuid(processBusinessId.toString())).thenAnswer(i -> new ResponseEntity<>(new PProcessDTO(
            processBusinessId,
            "the-process-name",
            true,
            processInfoMapper.toMap(processInfo),
            List.of(new ExecutionParamDTO("the-param-name", ExecutionParameterType.STRING, "The param desc"))),
                                                                                                             HttpStatus.OK));

        when(processingClient.createBatch(any())).thenAnswer(i -> {
            PBatchRequest req = i.getArgument(0);
            String correlationId = req.getCorrelationId();
            UUID batchId = UUID.randomUUID();
            batchCorrelations.put(batchId, correlationId);
            return new ResponseEntity<>(new PBatchResponse(batchId, correlationId), HttpStatus.OK);
        });
    }

    protected Basket createBasket(String orderOwner,
                                  String defaultTenant,
                                  UUID processBusinessId,
                                  Map<String, String> processParameters) {
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(SearchClientMock.DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        BasketDatedItemsSelection itemSelection = createDatasetItemSelection(1L, 12, 3, "ALL");
        dsSelection.addItemsSelection(itemSelection);
        dsSelection.setProcessDatasetDescription(new ProcessDatasetDescription(processBusinessId, processParameters));
        basket.addDatasetSelection(dsSelection);

        saveBasket(defaultTenant, basket);
        return basket;
    }

    protected Basket saveBasket(String tenant, Basket basket) {
        runtimeTenantResolver.forceTenant(tenant);
        return basketRepos.saveAndFlush(basket);
    }

    protected String randomLabel(String prefix) {
        return prefix + "_" + Long.toHexString(new Random().nextLong());
    }

    protected BasketDatedItemsSelection createDatasetItemSelection(long filesSize,
                                                                   long filesCount,
                                                                   int objectsCount,
                                                                   String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        item.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        item.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    protected BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

    public static class ProcessingMock {

        protected final ConcurrentLinkedQueue<PExecutionRequestEvent> execRequestEvents = new ConcurrentLinkedQueue<>();

        protected final long processingMillis = 500L;

        protected final IRuntimeTenantResolver runtimeTenantResolver;

        protected final IPublisher publisher;

        protected final TaskExecutor taskExecutor;

        protected final java.util.Map<UUID, String> batchCorrelations;

        public ProcessingMock(IRuntimeTenantResolver runtimeTenantResolver,
                              IPublisher publisher,
                              TaskExecutor taskExecutor,
                              Map<UUID, String> batchCorrelations) {
            this.runtimeTenantResolver = runtimeTenantResolver;
            this.publisher = publisher;
            this.taskExecutor = taskExecutor;
            this.batchCorrelations = batchCorrelations;
        }

        public Try<PExecutionRequestEvent> dealWithEvent(String tenant,
                                                         PExecutionRequestEvent requestEvent,
                                                         UUID processBusinessId,
                                                         OrderProcessInfo processInfo,
                                                         OrderProcessInfoMapper processInfoMapper) {
            execRequestEvents.add(requestEvent);
            new Thread(() -> {
                runtimeTenantResolver.forceTenant(tenant);
                UUID executionId = UUID.randomUUID();
                File outFile = new File("src/test/resources/processing/execResult");
                publisher.publish(new PExecutionResultEvent(executionId,
                                                            requestEvent.getExecutionCorrelationId(),
                                                            requestEvent.getBatchId(),
                                                            batchCorrelations.get(requestEvent.getBatchId()),
                                                            processBusinessId,
                                                            processInfoMapper.toMap(processInfo),
                                                            ExecutionStatus.SUCCESS,
                                                            List.of(new POutputFileDTO(UUID.randomUUID(),
                                                                                       executionId,
                                                                                       Try.of(() -> outFile.toURI()
                                                                                                           .toURL())
                                                                                          .get(),
                                                                                       "execID-" + executionId,
                                                                                       outFile.length(),
                                                                                       "MD5",
                                                                                       Try.of(() -> Files.hash(outFile,
                                                                                                               Hashing.md5())
                                                                                                         .toString())
                                                                                          .get(),
                                                                                       requestEvent.getInputFiles()
                                                                                                   .map(PInputFile::getInputCorrelationId)
                                                                                                   .toList())),
                                                            List.empty()));
            }).start();
            return Try.success(requestEvent);
        }

        public ConcurrentLinkedQueue<PExecutionRequestEvent> getExecRequestEvents() {
            return execRequestEvents;
        }
    }

    @Component
    public static class ExecResultHandlerResultEventHandler
        implements ApplicationListener<ExecResultHandlerResultEvent> {

        protected final java.util.Queue<ExecResultHandlerResultEvent> events = new ConcurrentLinkedQueue<>();

        protected Consumer<ExecResultHandlerResultEvent> consumer = e -> {
        };

        public void setConsumer(Consumer<ExecResultHandlerResultEvent> consumer) {
            this.consumer = consumer;
        }

        @Override
        public synchronized void onApplicationEvent(ExecResultHandlerResultEvent event) {
            events.add(event);
            consumer.accept(event);
        }

        public List<ExecResultHandlerResultEvent> getEvents() {
            return List.ofAll(events);
        }

        public void clear() {
            events.clear();
        }
    }

}