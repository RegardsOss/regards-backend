package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
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
                                                                             .withTimeoutAfterMillis(1_000_000_000L)
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
                                                                                                                          nowUtc().plusHours(
                                                                                                                              5))
                                                                                                                      .withCreationDateAfter(
                                                                                                                          nowUtc().minusHours(
                                                                                                                              5)),
                                                                                 PageRequest.of(0, 5))
                                                     .collectList()
                                                     .block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(finishedExecutionEntity.getId());
        assertThat(pExecutions.get(0).isPersisted()).isTrue();

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters().withProcessBusinessId(
                                                                                                         batchEntity.getProcessBusinessId().toString())
                                                                                                     .withUserEmailIncluded(
                                                                                                         Collections.singleton(
                                                                                                             batchEntity.getUserEmail()))
                                                                                                     .withStatusIncluded(
                                                                                                         Arrays.asList(
                                                                                                             RUNNING,
                                                                                                             PREPARE,
                                                                                                             CLEANUP))
                                                                                                     .withCreationDateBefore(
                                                                                                         nowUtc().plusHours(
                                                                                                             5))
                                                                                                     .withCreationDateAfter(
                                                                                                         nowUtc().minusHours(
                                                                                                             5)),
                                                                PageRequest.of(0, 1)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(longUnfinishedExecutionEntity.getId());
        assertThat(pExecutions.get(0).isPersisted()).isTrue();

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters().withUserEmailIncluded(
                                                                                                         Collections.singleton(batchEntity.getUserEmail()))
                                                                                                     .withStatusIncluded(
                                                                                                         Arrays.asList(
                                                                                                             RUNNING,
                                                                                                             PREPARE,
                                                                                                             CLEANUP))
                                                                                                     .withCreationDateBefore(
                                                                                                         nowUtc().plusHours(
                                                                                                             5))
                                                                                                     .withCreationDateAfter(
                                                                                                         nowUtc().minusHours(
                                                                                                             5)),
                                                                PageRequest.of(1, 1)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(1);
        assertThat(pExecutions.get(0).getId()).isEqualTo(shortUnfinishedExecutionEntity.getId());
        assertThat(pExecutions.get(0).isPersisted()).isTrue();

        // WHEN
        pExecutions = domainExecRepo.findAllForMonitoringSearch(batchEntity.getTenant(),
                                                                new SearchExecutionEntityParameters(),
                                                                PageRequest.of(0, 10)).collectList().block();
        // THEN
        assertThat(pExecutions).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(pExecutions.get(i).isPersisted()).isTrue();
        }
    }

    @Test
    public void test_countAllForMonitoringSearch() {
        // GIVEN
        // WHEN
        Integer countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                              new SearchExecutionEntityParameters().withStatusIncluded(
                                                                                                                       Arrays.asList(SUCCESS))
                                                                                                                   .withCreationDateBefore(
                                                                                                                       nowUtc().plusHours(
                                                                                                                           5))
                                                                                                                   .withCreationDateAfter(
                                                                                                                       nowUtc().minusHours(
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
                                                                                                               nowUtc().plusHours(
                                                                                                                   5))
                                                                                                           .withCreationDateAfter(
                                                                                                               nowUtc().minusHours(
                                                                                                                   5)))
                                         .block();
        // THEN
        assertThat(countPExecutions).isEqualTo(2);

        // WHEN
        countPExecutions = domainExecRepo.countAllForMonitoringSearch(batchEntity.getTenant(),
                                                                      new SearchExecutionEntityParameters().withUserEmailIncluded(
                                                                                                               Collections.singleton(batchEntity.getUserEmail()))
                                                                                                           .withStatusIncluded(
                                                                                                               Arrays.asList(
                                                                                                                   RUNNING))
                                                                                                           .withCreationDateBefore(
                                                                                                               nowUtc().plusHours(
                                                                                                                   1))
                                                                                                           .withCreationDateAfter(
                                                                                                               nowUtc().minusHours(
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

    @Test
    public void test_getTimedOutExecutions() {
        // WHEN
        List<PExecution> executions = domainExecRepo.getTimedOutExecutions().collectList().block();
        //        System.out.println(executions);
        // THEN
        assertThat(executions).hasSize(1);
        PExecution execution = executions.get(0);
        //        System.out.println(execution);
        // WHEN
        domainExecRepo.findById(execution.getId()).flatMap(exec -> addExecutionStep(exec, PStep.timeout("bou")));
        domainExecRepo.create(execution.withLastUpdated(OffsetDateTime.now()));
        // THEN
        assertThat(execution.isPersisted()).isTrue();
    }

    private Mono<PExecution> addExecutionStep(PExecution exec, PStep step) {
        return domainExecRepo.update(exec.addStep(step)).onErrorResume(OptimisticLockingFailureException.class, e -> {
            LOGGER.warn("Optimistic locking failure when adding step {} to exec {}", step, exec.getId());
            return Mono.defer(() -> domainExecRepo.findById(exec.getId())
                                                  .flatMap(freshExec -> addExecutionStep(freshExec, step)));
        }).onErrorMap(mustWrap(), t -> new Exception("Persisting step failed: " + step, t));
    }

}