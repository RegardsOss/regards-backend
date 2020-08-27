package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.Step;
import fr.cnes.regards.modules.processing.entity.Steps;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.toEpochMillisUTC;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class IExecutionEntityRepositoryTest extends AbstractRepoTest {

    public static final ZoneId UTC = ZoneId.of("UTC");

    private static final Logger LOGGER = LoggerFactory.getLogger(IExecutionEntityRepositoryTest.class);

    @Test public void test_timedout_executions() throws Exception {
        // GIVEN
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        // This execution has succeeded, and so, it will not be found as timed out.
        ExecutionEntity finishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserName(batch.getUserName()).withProcessName(batch.getProcessName())
                .withProcessBusinessId(batch.getProcessBusinessId())
                .withCurrentStatus(SUCCESS).withTimeoutAfterMillis(1_000L).withPersisted(false).withSteps(
                        Steps.of(new Step(REGISTERED, toEpochMillisUTC(now(UTC).minusMinutes(5)), "pending"),
                                 new Step(RUNNING, toEpochMillisUTC(now(UTC).minusMinutes(4)), "running"),
                                 new Step(SUCCESS, toEpochMillisUTC(now(UTC).minusMinutes(3)), "success")));

        // This execution has not terminated and has short timeout, and so, it will be found as timed out.
        ExecutionEntity shortUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserName(batch.getUserName()).withProcessName(batch.getProcessName())
                .withProcessBusinessId(batch.getProcessBusinessId())
                .withCurrentStatus(RUNNING).withTimeoutAfterMillis(1_000L).withPersisted(false).withSteps(
                        Steps.of(new Step(REGISTERED, toEpochMillisUTC(now(UTC).minusMinutes(5)), "pending"),
                                 new Step(RUNNING, toEpochMillisUTC(now(UTC).minusMinutes(4)), "running")));
        LOGGER.info("Test should find this execution as timedout: {}", shortUnfinishedExec.getId());

        // This execution has not terminated but has long timeout, and so, it will not be found as timed out.
        ExecutionEntity longUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                .withTenant(batch.getTenant()).withUserName(batch.getUserName()).withProcessName(batch.getProcessName())
                .withProcessBusinessId(batch.getProcessBusinessId())
                .withCurrentStatus(RUNNING).withTimeoutAfterMillis(1_000_000L).withPersisted(false).withSteps(
                        Steps.of(new Step(REGISTERED, toEpochMillisUTC(now(UTC).minusMinutes(5)), "pending"),
                                 new Step(RUNNING, toEpochMillisUTC(now(UTC).minusMinutes(4)), "running")));

        // WHEN
        List<ExecutionEntity> timedoutExecs =
                // Save all entities in database
                entityBatchRepo.save(batch).doOnError(t -> LOGGER.error("Could not save batch", t)).flatMapMany(
                        persistedBatch -> entityExecRepo
                                .saveAll(Flux.just(finishedExec, shortUnfinishedExec, longUnfinishedExec))
                                .doOnError(t -> LOGGER.error("Could not save execs", t))).last()
                        // find timed out executions
                        .flatMapMany(s -> {
                            return entityExecRepo.getTimedOutExecutions();
                        })
                        // retrieve synchronously
                        .collectList().block();

        // THEN
        LOGGER.info("Timedout executions: {}", timedoutExecs);
        assertThat(timedoutExecs).hasSize(1);
        assertThat(timedoutExecs.get(0).getId()).isEqualTo(shortUnfinishedExec.getId());

    }

    @Test public void test_findByStatus() throws Exception {
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        BatchEntity persistedBatch = entityBatchRepo.save(batch).block();

        List<ExecutionEntity> execs = entityExecRepo.saveAll(Flux.create(sink -> {
            for (int i = 0; i < 100; i++) {
                sink.next(randomInstance(ExecutionEntity.class)
                    .withId(UUID.randomUUID())
                    .withBatchId(batch.getId()).withTenant(batch.getTenant())
                    .withUserName(batch.getUserName()).withProcessName(batch.getProcessName())
                    .withProcessBusinessId(batch.getProcessBusinessId())
                    .withVersion(0).withPersisted(false).withCreated(nowUtc()).withLastUpdated(nowUtc())
                    .withCurrentStatus(randomInstance(ExecutionStatus.class)));
            }
            sink.complete();
        })).collectList().block();

        List<ExecutionEntity> foundExecs1010 = entityExecRepo
                .findByTenantAndCurrentStatusIn(batch.getTenant(), singletonList(RUNNING), PageRequest.of(0, 10))
                .collectList().block();

        LOGGER.info("Found execs page(10,10): {}", foundExecs1010);


        List<ExecutionEntity> foundExecs1020 = entityExecRepo
                .findByTenantAndCurrentStatusIn(batch.getTenant(), singletonList(RUNNING), PageRequest.of(1, 10))
                .collectList().block();

        LOGGER.info("Found execs page(10,10): {}", foundExecs1020);
        LOGGER.info("Done");

        // TODO add assertions
    }

}