package fr.cnes.regards.modules.processing.rest;

import feign.*;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.entity.*;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import fr.cnes.regards.modules.processing.testutils.TestSpringConfiguration;
import fr.cnes.regards.modules.processing.utils.random.RandomUtils;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import lombok.Getter;
import lombok.Setter;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(
        classes = { TestSpringConfiguration.class, PMonitoringReactiveControllerTest.Config.class }
)
public class PMonitoringReactiveControllerTest extends AbstractProcessingTest {

    @Autowired private IBatchEntityRepository batchRepo;
    @Autowired private IExecutionEntityRepository execRepo;

    @Test public void executions() {
        // GIVEN
        UUID batchAId = UUID.randomUUID();
        UUID batchBId = UUID.randomUUID();
        createBatches(batchAId, batchBId);
        List<ExecutionEntity> entities = createExecutions(batchAId, batchBId);

        // WHEN
        MyPageImpl<PExecution> response = client
            .executions(TENANT_PROJECTA, asList(RUNNING, PREPARE),
                toMap(
                    PageRequest.of(2, 10)
                        //, Tuple.of(USER_EMAIL_PARAM, "a@a.a")
                        //, Tuple.of(DATE_FROM_PARAM, nowUtc().withNano(0).minusHours(2).toString())
                        //, Tuple.of(DATE_TO_PARAM, nowUtc().withNano(0).minusHours(1).toString())
                )
            );

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(response.getTotal()).isEqualTo(35);
        assertThat(response.getContent()).hasSize(10);


        // WHEN
        MyPageImpl<PExecution> responseEmpty = client
            .executions(TENANT_PROJECTA, asList(CANCELLED),
                toMap(PageRequest.of(2, 10))
            );

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(responseEmpty.getTotal()).isEqualTo(0);
        assertThat(responseEmpty.getContent()).hasSize(0);
    }

    private void createBatches(UUID batchAId, UUID batchBId) {

        BatchEntity batchA = new BatchEntity(
                batchAId,
                UUID.randomUUID(),
                "",
                TENANT_PROJECTA,
                "a@a.a",
                "EXPLOIT",
                "processname",
                new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty().toJavaMap()));
        BatchEntity batchB = new BatchEntity(
                batchBId,
                UUID.randomUUID(),
                "",
                TENANT_PROJECTB,
                "a@a.a",
                "EXPLOIT",
                "processname",
                new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty().toJavaMap()));

        batchRepo.saveAll(List.of(batchA, batchB)).collectList().block();
    }

    private List<ExecutionEntity> createExecutions(UUID batchAId, UUID batchBId) {

        Random r = new Random();
        long now = System.currentTimeMillis();

        List<ExecutionEntity> failures = randomList(ExecutionEntity.class, 20).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchAId)
                .withTenant(TENANT_PROJECTA)
                .withCurrentStatus(FAILURE)
                .withVersion(0)
                .withPersisted(false)
                .withSteps(new Steps(List.of(new StepEntity(FAILURE, now, "")).asJava()))
        );

        List<ExecutionEntity> entities = randomList(ExecutionEntity.class, 35).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchAId)
                .withTenant(TENANT_PROJECTA)
                .withCurrentStatus(r.nextBoolean() ? RUNNING : PREPARE)
                .withVersion(0)
                .withPersisted(false)
                .withSteps(new Steps(List.of(new StepEntity(RUNNING, now, "")).asJava()))
        );

        List<ExecutionEntity> otherTenants = randomList(ExecutionEntity.class, 20).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchBId)
                .withTenant(TENANT_PROJECTB)
                .withCurrentStatus(PREPARE)
                .withVersion(0)
                .withPersisted(false)
                .withSteps(new Steps(List.of(new StepEntity(PREPARE, now, "")).asJava()))
        );

        execRepo.saveAll(otherTenants.appendAll(failures).appendAll(entities))
                .collectList()
                .block();

        return entities;
    }

    private Map<String, String> toMap(Pageable page, Tuple2<String,String>... rest) {
        return Stream.of(rest).foldLeft(HashMap.of(
            PAGE_PARAM, "" + page.getPageNumber(),
            SIZE_PARAM, "" + page.getPageSize()
        ), (acc, t) -> acc.put(t)).toJavaMap();
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringReactiveControllerTest.class);

    private Client client;

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
                .decoder(new GsonLoggingDecoder(gson))
                .encoder(new GsonLoggingEncoder(gson))
                .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + port, feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asSystem();
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + MONITORING_EXECUTIONS_PATH + "?tenant={tenant}&status={status}")
        MyPageImpl<PExecution> executions(
                @Param("tenant") String tenant,
                @Param("status") java.util.List<ExecutionStatus> status,
                @QueryMap Map<String,String> params
        );
    }

    @Configuration
    @EnableFeignClients(basePackageClasses = { IRolesClient.class, IStorageRestClient.class })
    static class Config {
        @Bean
        public IWorkloadEngineRepository workloadEngineRepository() {
            return Mockito.mock(IWorkloadEngineRepository.class);
        }
    }

    @lombok.Value @lombok.AllArgsConstructor
    public static class MyPageImpl<T> {
        List<T> content;
        PageRequest pageable;
        long total;
    }

}