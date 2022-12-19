package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

public class PExecutionRepositoryImplIT extends AbstractRepoIT {

    private BatchEntity batchEntity;

    private ExecutionEntity finishedExecutionEntity;

    private ExecutionEntity longUnfinishedExecutionEntity;

    private ExecutionEntity shortUnfinishedExecutionEntity;

    @Before
    public void init() {
        batchEntity = randomInstance(BatchEntity.class).withPersisted(false);

        // This execution has succeeded, and so, it will not be found as timed out.
        finishedExecutionEntity = randomInstance(ExecutionEntity.class).withBatchId(batchEntity.getId())
                                                                       .withTenant(batchEntity.getTenant())
                                                                       .withUserEmail(batchEntity.getUserEmail())
                                                                       .withProcessBusinessId(batchEntity.getProcessBusinessId())
                                                                       .withCurrentStatus(SUCCESS)
                                                                       .withLastUpdated(nowUtc().minusMinutes(3))
                                                                       .withTimeoutAfterMillis(1_000L)
                                                                       .withPersisted(false);

        // This execution has not terminated and has short timeout, and so, it will be found as timed out.
        shortUnfinishedExecutionEntity = randomInstance(ExecutionEntity.class).withBatchId(batchEntity.getId())
                                                                              .withTenant(batchEntity.getTenant())
                                                                              .withUserEmail(batchEntity.getUserEmail())
                                                                              .withProcessBusinessId(batchEntity.getProcessBusinessId())
                                                                              .withCurrentStatus(RUNNING)
                                                                              .withLastUpdated(nowUtc().minusMinutes(4))
                                                                              .withTimeoutAfterMillis(1_000L)
                                                                              .withPersisted(false);
        LOGGER.info("Test should find this execution as timedout: {}", shortUnfinishedExecutionEntity.getId());

        // This execution has not terminated but has long timeout, and so, it will not be found as timed out.
        longUnfinishedExecutionEntity = randomInstance(ExecutionEntity.class).withBatchId(batchEntity.getId())
                                                                             .withTenant(batchEntity.getTenant())
                                                                             .withUserEmail(batchEntity.getUserEmail())
                                                                             .withProcessBusinessId(batchEntity.getProcessBusinessId())
                                                                             .withCurrentStatus(RUNNING)
                                                                             .withLastUpdated(nowUtc().minusHours(4))
                                                                             .withTimeoutAfterMillis(1_000_000L)
                                                                             .withPersisted(false);

        entityBatchRepo.save(this.batchEntity).doOnError(t -> LOGGER.error("Could not save batch", t)).block();

        entityExecRepo.saveAll(Flux.just(finishedExecutionEntity,
                                         shortUnfinishedExecutionEntity,
                                         longUnfinishedExecutionEntity))
                      .doOnError(t -> LOGGER.error("Could not save execs", t))
                      .collectList()
                      .block();
    }

    @After
    public void reset() {
        entityBatchRepo.deleteAll();
        entityExecRepo.deleteAll();
    }

    @Test
    public void test_findAllForMonitoringSearch() {
        // GIVEN
        // WHEN
        List<PExecution> pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                                 new SearchExecutionEntityParameters().withStatusIncluded(
                                                                                                                          Arrays.asList(SUCCESS))
                                                                                                                      .withCreationDateBefore(
                                                                                                                          nowUtc().minusHours(
                                                                                                                              5))
                                                                                                                      .withCreationDateAfter(
                                                                                                                          nowUtc().plusHours(
                                                                                                                              5)),
                                                                                 PageRequest.of(0, 5))
                                                     .collectList()
                                                     .block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(finishedExecutionEntity.getId());

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters().withProcessBusinessId(
                                                                                                         batchEntity.getProcessBusinessId().toString())
                                                                                                     .withUserEmail(
                                                                                                         batchEntity.getUserEmail())
                                                                                                     .withStatusIncluded(
                                                                                                         Arrays.asList(
                                                                                                             RUNNING,
                                                                                                             PREPARE,
                                                                                                             CLEANUP))
                                                                                                     .withCreationDateBefore(
                                                                                                         nowUtc().minusHours(
                                                                                                             5))
                                                                                                     .withCreationDateAfter(
                                                                                                         nowUtc().plusHours(
                                                                                                             5)),
                                                                PageRequest.of(0, 1)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(longUnfinishedExecutionEntity.getId());

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters().withUserEmail(
                                                                                                         batchEntity.getUserEmail())
                                                                                                     .withStatusIncluded(
                                                                                                         Arrays.asList(
                                                                                                             RUNNING,
                                                                                                             PREPARE,
                                                                                                             CLEANUP))
                                                                                                     .withCreationDateBefore(
                                                                                                         nowUtc().minusHours(
                                                                                                             5))
                                                                                                     .withCreationDateAfter(
                                                                                                         nowUtc().plusHours(
                                                                                                             5)),
                                                                PageRequest.of(1, 1)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(shortUnfinishedExecutionEntity.getId());

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters(),
                                                                PageRequest.of(0, 10)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(3);
    }

    @Test
    public void test_countAllForMonitoringSearch() {
        // GIVEN
        // WHEN
        Integer countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                              new SearchExecutionEntityParameters().withStatusIncluded(
                                                                                                                       Arrays.asList(SUCCESS))
                                                                                                                   .withCreationDateBefore(
                                                                                                                       nowUtc().minusHours(
                                                                                                                           5))
                                                                                                                   .withCreationDateAfter(
                                                                                                                       nowUtc().plusHours(
                                                                                                                           5)))
                                                 .block();
        // THEN
        assertThat(countPExecutions).isEqualTo(1);

        // WHEN
        countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                      new SearchExecutionEntityParameters().withProcessBusinessId(
                                                                                                               batchEntity.getProcessBusinessId().toString())
                                                                                                           .withStatusIncluded(
                                                                                                               Arrays.asList(
                                                                                                                   RUNNING))
                                                                                                           .withCreationDateBefore(
                                                                                                               nowUtc().minusHours(
                                                                                                                   5))
                                                                                                           .withCreationDateAfter(
                                                                                                               nowUtc().plusHours(
                                                                                                                   5)))
                                         .block();
        // THEN
        assertThat(countPExecutions).isEqualTo(2);

        // WHEN
        countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                      new SearchExecutionEntityParameters().withUserEmail(
                                                                                                               batchEntity.getUserEmail())
                                                                                                           .withStatusIncluded(
                                                                                                               Arrays.asList(
                                                                                                                   RUNNING))
                                                                                                           .withCreationDateBefore(
                                                                                                               nowUtc().minusHours(
                                                                                                                   1))
                                                                                                           .withCreationDateAfter(
                                                                                                               nowUtc().plusHours(
                                                                                                                   1)))
                                         .block();
        // THEN
        assertThat(countPExecutions).isEqualTo(1);

        // WHEN
        countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                      new SearchExecutionEntityParameters()).block();
        // THEN
        assertThat(countPExecutions).isEqualTo(3);
    }

}