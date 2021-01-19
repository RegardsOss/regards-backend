/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.rest;

import feign.*;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.entity.*;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.testutils.servlet.TestSpringConfiguration;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PAGE_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.SIZE_PARAM;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(
        classes = { TestSpringConfiguration.class, PMonitoringControllerTest.Config.class }
)
public class PMonitoringControllerTest extends AbstractProcessingTest {

    @Autowired private IBatchEntityRepository batchRepo;
    @Autowired private IExecutionEntityRepository execRepo;
    @Autowired private IPProcessRepository processRepository;

    @Test public void executions() {

        // GIVEN
        UUID processId = UUID.randomUUID();
        PProcess mockProcess = mock(PProcess.class);
        when(mockProcess.getProcessName()).thenReturn("processName");
        when(mockProcess.getProcessId()).thenReturn(processId);
        when(processRepository.findByTenantAndProcessBusinessID(any(), any())).thenAnswer(i -> Mono.just(mockProcess));

        UUID batchAId = UUID.randomUUID();
        UUID batchBId = UUID.randomUUID();
        createBatches(batchAId, batchBId);
        List<ExecutionEntity> entities = createExecutions(processId, batchAId, batchBId);

        // WHEN
        PagedModel<EntityModel<ExecutionMonitoringDTO>> response = client
            .executions(TENANT_PROJECTA, asList(RUNNING, PREPARE), toMap(PageRequest.of(2, 10)));

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(response.getMetadata().getTotalElements()).isEqualTo(35);
        assertThat(response.getContent()).hasSize(10);


        // WHEN
        PagedModel<EntityModel<ExecutionMonitoringDTO>> responseEmpty = client
            .executions(TENANT_PROJECTA, asList(CANCELLED), toMap(PageRequest.of(2, 10)));

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(responseEmpty.getMetadata().getTotalElements()).isEqualTo(0);
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
                new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty().toJavaMap()));
        BatchEntity batchB = new BatchEntity(
                batchBId,
                UUID.randomUUID(),
                "",
                TENANT_PROJECTB,
                "a@a.a",
                "EXPLOIT",
                new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty().toJavaMap()));

        batchRepo.saveAll(List.of(batchA, batchB)).collectList().block();
    }

    private List<ExecutionEntity> createExecutions(UUID processId, UUID batchAId, UUID batchBId) {

        Random r = new Random();
        long now = System.currentTimeMillis();

        List<ExecutionEntity> failures = randomList(ExecutionEntity.class, 20).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchAId)
                .withProcessBusinessId(processId)
                .withTenant(TENANT_PROJECTA)
                .withCurrentStatus(FAILURE)
                .withVersion(0)
                .withPersisted(false)
                .withSteps(new Steps(List.of(new StepEntity(FAILURE, now, "")).asJava()))
        );

        List<ExecutionEntity> entities = randomList(ExecutionEntity.class, 35).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchAId)
                .withProcessBusinessId(processId)
                .withTenant(TENANT_PROJECTA)
                .withCurrentStatus(r.nextBoolean() ? RUNNING : PREPARE)
                .withVersion(0)
                .withPersisted(false)
                .withSteps(new Steps(List.of(new StepEntity(RUNNING, now, "")).asJava()))
        );

        List<ExecutionEntity> otherTenants = randomList(ExecutionEntity.class, 20).map(e -> e
                .withId(UUID.randomUUID())
                .withBatchId(batchBId)
                .withProcessBusinessId(processId)
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringControllerTest.class);

    private Client client;

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
                .decoder(new GsonLoggingDecoder(gson))
                .encoder(new GsonLoggingEncoder(gson))
                .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + port, feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + MONITORING_EXECUTIONS_PATH + "?tenant={tenant}&status={status}")
        PagedModel<EntityModel<ExecutionMonitoringDTO>> executions(
                @Param("tenant") String tenant,
                @Param("status") java.util.List<ExecutionStatus> status,
                @QueryMap Map<String,String> params
        );
    }

    @Configuration
    static class Config {
        @Bean
        public IWorkloadEngineRepository workloadEngineRepository() {
            return mock(IWorkloadEngineRepository.class);
        }

        @Primary
        @Bean
        public IPProcessRepository processRepo() {
            return mock(IPProcessRepository.class);
        }
    }

}