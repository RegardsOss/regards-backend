package fr.cnes.regards.modules.processing.dao;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.CLEANUP;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.PREPARE;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.RUNNING;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.SUCCESS;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import reactor.core.publisher.Flux;

public class PExecutionRepositoryImplIT extends AbstractRepoIT {

    @Test
    public void test_count_executions() throws Exception {
        // GIVEN
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        // This execution has succeeded, and so, it will not be found as timed out.
        ExecutionEntity finishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserEmail(batch.getUserEmail())
                .withProcessBusinessId(batch.getProcessBusinessId()).withCurrentStatus(SUCCESS)
                .withLastUpdated(nowUtc().minusMinutes(3)).withTimeoutAfterMillis(1_000L).withPersisted(false);

        // This execution has not terminated and has short timeout, and so, it will be found as timed out.
        ExecutionEntity shortUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserEmail(batch.getUserEmail())
                .withProcessBusinessId(batch.getProcessBusinessId()).withCurrentStatus(RUNNING)
                .withLastUpdated(nowUtc().minusMinutes(4)).withTimeoutAfterMillis(1_000L).withPersisted(false);
        LOGGER.info("Test should find this execution as timedout: {}", shortUnfinishedExec.getId());

        // This execution has not terminated but has long timeout, and so, it will not be found as timed out.
        ExecutionEntity longUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserEmail(batch.getUserEmail())
                .withProcessBusinessId(batch.getProcessBusinessId()).withCurrentStatus(RUNNING)
                .withLastUpdated(nowUtc().minusHours(4)).withTimeoutAfterMillis(1_000_000L).withPersisted(false);

        BatchEntity batchEntity = entityBatchRepo.save(batch).doOnError(t -> LOGGER.error("Could not save batch", t))
                .block();

        List<ExecutionEntity> execEntities = entityExecRepo
                .saveAll(Flux.just(finishedExec, shortUnfinishedExec, longUnfinishedExec))
                .doOnError(t -> LOGGER.error("Could not save execs", t)).collectList().block();

        // WHEN
        Integer countSuccess = domainExecRepo
                .countAllForMonitoringSearch(batch.getTenant(), null, null, singletonList(SUCCESS),
                                             nowUtc().minusHours(5), nowUtc().plusHours(5))
                .block();

        // THEN
        assertThat(countSuccess).isEqualTo(1);

        // WHEN
        List<PExecution> execs = domainExecRepo
                .findAllForMonitoringSearch(batch.getTenant(), null, null, singletonList(SUCCESS),
                                            nowUtc().minusHours(5), nowUtc().plusHours(5), PageRequest.of(0, 5))
                .collectList().block();

        // THEN
        assertThat(execs).hasSize(1);
        assertThat(execs.get(0).getId()).isEqualTo(finishedExec.getId());

        // WHEN
        Integer countRunning = domainExecRepo
                .countAllForMonitoringSearch(batch.getTenant(), batch.getProcessBusinessId().toString(), null,
                                             singletonList(RUNNING), nowUtc().minusHours(5), nowUtc().plusHours(5))
                .block();

        // THEN
        assertThat(countRunning).isEqualTo(2);

        // WHEN
        List<PExecution> execRunningFirstPage = domainExecRepo
                .findAllForMonitoringSearch(batch.getTenant(), batch.getProcessBusinessId().toString(),
                                            batch.getUserEmail(), Arrays.asList(RUNNING, PREPARE, CLEANUP),
                                            nowUtc().minusHours(5), nowUtc().plusHours(5), PageRequest.of(0, 1))
                .collectList().block();

        // THEN
        assertThat(execRunningFirstPage).hasSize(1);
        assertThat(execRunningFirstPage.get(0).getId()).isEqualTo(longUnfinishedExec.getId());

        // WHEN
        List<PExecution> execRunningSecondPage = domainExecRepo
                .findAllForMonitoringSearch(batch.getTenant(), null, batch.getUserEmail(),
                                            Arrays.asList(RUNNING, PREPARE, CLEANUP), nowUtc().minusHours(5),
                                            nowUtc().plusHours(5), PageRequest.of(1, 1))
                .collectList().block();

        // THEN
        assertThat(execRunningSecondPage).hasSize(1);
        assertThat(execRunningSecondPage.get(0).getId()).isEqualTo(shortUnfinishedExec.getId());

        // WHEN
        Integer countRunningTimed = domainExecRepo
                .countAllForMonitoringSearch(batch.getTenant(), null, batch.getUserEmail(), singletonList(RUNNING),
                                             nowUtc().minusHours(1), nowUtc().plusHours(1))
                .block();

        // THEN
        assertThat(countRunningTimed).isEqualTo(1);

    }

}