/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import feign.Feign;
import feign.Headers;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.entity.*;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingIT;
import fr.cnes.regards.modules.processing.testutils.servlet.TestSpringConfiguration;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.support.PageableSpringQueryMapEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.TENANT_PARAM;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestSpringConfiguration.class, PMonitoringControllerIT.Config.class })
public class PMonitoringControllerIT extends AbstractProcessingIT {

    @Autowired
    private IBatchEntityRepository batchEntityRepository;

    @Autowired
    private IExecutionEntityRepository executionEntityRepository;

    @Autowired
    private IPProcessRepository processRepository;

    @Test
    public void executions() {

        // reset db
        executionEntityRepository.deleteAll().block();

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

        SearchExecutionEntityParameters searchExecutionEntityParameters = new SearchExecutionEntityParameters().withStatusIncluded(
            Arrays.asList(RUNNING, PREPARE));

        // Check sorting
        PagedModel<EntityModel<ExecutionMonitoringDTO>> response = client.executions(TENANT_PROJECTA,
                                                                                     searchExecutionEntityParameters,
                                                                                     PageRequest.of(2, 10));

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(response.getMetadata().getTotalElements()).isEqualTo(35);
        assertThat(response.getContent()).hasSize(10);

        // GIVEN
        searchExecutionEntityParameters = new SearchExecutionEntityParameters().withStatusIncluded(Arrays.asList(
            CANCELLED));

        // WHEN
        PagedModel<EntityModel<ExecutionMonitoringDTO>> responseEmpty = client.executions(TENANT_PROJECTA,
                                                                                          searchExecutionEntityParameters,
                                                                                          PageRequest.of(2, 10));

        LOGGER.info("Resp: {}", response);

        // THEN
        assertThat(responseEmpty.getMetadata().getTotalElements()).isEqualTo(0);
        assertThat(responseEmpty.getContent()).hasSize(0);
    }

    private void createBatches(UUID batchAId, UUID batchBId) {

        BatchEntity batchA = new BatchEntity(batchAId,
                                             UUID.randomUUID(),
                                             "",
                                             TENANT_PROJECTA,
                                             "a@a.a",
                                             "EXPLOIT",
                                             new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                                             new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty()
                                                                           .toJavaMap()));
        BatchEntity batchB = new BatchEntity(batchBId,
                                             UUID.randomUUID(),
                                             "",
                                             TENANT_PROJECTB,
                                             "a@a.a",
                                             "EXPLOIT",
                                             new ParamValues(List.<ExecutionStringParameterValue>empty().asJava()),
                                             new FileStatsByDataset(HashMap.<String, FileSetStatistics>empty()
                                                                           .toJavaMap()));

        batchEntityRepository.saveAll(List.of(batchA, batchB)).collectList().block();
    }

    private List<ExecutionEntity> createExecutions(UUID processId, UUID batchAId, UUID batchBId) {

        Random r = new Random();
        long now = System.currentTimeMillis();

        List<ExecutionEntity> failures = randomList(ExecutionEntity.class, 20).map(e -> e.withId(UUID.randomUUID())
                                                                                         .withBatchId(batchAId)
                                                                                         .withProcessBusinessId(
                                                                                             processId)
                                                                                         .withTenant(TENANT_PROJECTA)
                                                                                         .withCurrentStatus(FAILURE)
                                                                                         .withVersion(0)
                                                                                         .withPersisted(false)
                                                                                         .withSteps(new Steps(List.of(
                                                                                                                      new StepEntity(FAILURE,
                                                                                                                                     now,
                                                                                                                                     ""))
                                                                                                                  .asJava())));

        List<ExecutionEntity> entities = randomList(ExecutionEntity.class, 35).map(e -> e.withId(UUID.randomUUID())
                                                                                         .withBatchId(batchAId)
                                                                                         .withProcessBusinessId(
                                                                                             processId)
                                                                                         .withTenant(TENANT_PROJECTA)
                                                                                         .withCurrentStatus(r.nextBoolean() ?
                                                                                                                RUNNING :
                                                                                                                PREPARE)
                                                                                         .withVersion(0)
                                                                                         .withPersisted(false)
                                                                                         .withSteps(new Steps(List.of(
                                                                                                                      new StepEntity(RUNNING,
                                                                                                                                     now,
                                                                                                                                     ""))
                                                                                                                  .asJava())));

        List<ExecutionEntity> otherTenants = randomList(ExecutionEntity.class, 20).map(e -> e.withId(UUID.randomUUID())
                                                                                             .withBatchId(batchBId)
                                                                                             .withProcessBusinessId(
                                                                                                 processId)
                                                                                             .withTenant(TENANT_PROJECTB)
                                                                                             .withCurrentStatus(PREPARE)
                                                                                             .withVersion(0)
                                                                                             .withPersisted(false)
                                                                                             .withSteps(new Steps(List.of(
                                                                                                                          new StepEntity(PREPARE,
                                                                                                                                         now,
                                                                                                                                         ""))
                                                                                                                      .asJava())));

        executionEntityRepository.saveAll(otherTenants.appendAll(failures).appendAll(entities)).collectList().block();

        return entities;
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringControllerIT.class);

    private Client client;

    @Before
    public void init() {
        client = Feign.builder()
                      .decoder(new GsonLoggingDecoder(gson))
                      .encoder(new GsonLoggingEncoder(gson))
                      .contract(new SpringMvcContract())
                      .queryMapEncoder(new PageableSpringQueryMapEncoder())
                      .target(new TokenClientProvider<>(Client.class,
                                                        "http://" + serverAddress + ":" + port,
                                                        feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {

        @PostMapping(path = MONITORING_EXECUTIONS_PATH,
                     consumes = MediaType.APPLICATION_JSON_VALUE,
                     produces = MediaType.APPLICATION_JSON_VALUE)
        PagedModel<EntityModel<ExecutionMonitoringDTO>> executions(@RequestParam(TENANT_PARAM) String tenant,
                                                                   @RequestBody SearchExecutionEntityParameters filters,
                                                                   @SpringQueryMap Pageable pageable);

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