package fr.cnes.regards.modules.processing.dao;


import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entities.BatchEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionStepEntity;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class IExecutionEntityRepositoryTest extends AbstractRepoTest {

    public static final ZoneId UTC = ZoneId.of("UTC");

    private static final Logger LOGGER = LoggerFactory.getLogger(IExecutionEntityRepositoryTest.class);

    @Test
    public void test_timedout_executions() throws Exception {
        // GIVEN
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        // This execution has succeeded, and so, it will not be found as timed out.
        ExecutionEntity finishedExec = randomInstance(ExecutionEntity.class)
                .withBatchId(batch.getId())
                .withTimeoutAfterMillis(1_000L)
                .withPersisted(false);
        Flux<ExecutionStepEntity> shortFinishedSteps = Flux
                .just(new ExecutionStepEntity(null, finishedExec.getId(), ExecutionStatus.REGISTERED,
                                              OffsetDateTime.now(UTC).minusMinutes(5), "pending"),
                      new ExecutionStepEntity(null, finishedExec.getId(), ExecutionStatus.RUNNING,
                                              OffsetDateTime.now(UTC).minusMinutes(4), "running"),
                      new ExecutionStepEntity(null, finishedExec.getId(), ExecutionStatus.SUCCESS,
                                              OffsetDateTime.now(UTC).minusMinutes(3), "success"));

        // This execution has not terminated and has short timeout, and so, it will be found as timed out.
        ExecutionEntity shortUnfinishedExec = randomInstance(ExecutionEntity.class)
                .withBatchId(batch.getId())
                .withTimeoutAfterMillis(1_000L)
                .withPersisted(false);
        LOGGER.info("Test should find this execution as timedout: {}", shortUnfinishedExec.getId());
        Flux<ExecutionStepEntity> shortUnfinishedSteps = Flux
                .just(new ExecutionStepEntity(null, shortUnfinishedExec.getId(), ExecutionStatus.REGISTERED,
                                              OffsetDateTime.now(UTC).minusMinutes(5), "pending"),
                      new ExecutionStepEntity(null, shortUnfinishedExec.getId(), ExecutionStatus.RUNNING,
                                              OffsetDateTime.now(UTC).minusMinutes(4), "running"));

        // This execution has not terminated but has long timeout, and so, it will not be found as timed out.
        ExecutionEntity longUnfinishedExec = randomInstance(ExecutionEntity.class)
                .withBatchId(batch.getId())
                .withTimeoutAfterMillis(1_000_000L)
                .withPersisted(false);
        Flux<ExecutionStepEntity> longUnfinishedSteps = Flux
                .just(new ExecutionStepEntity(null, longUnfinishedExec.getId(), ExecutionStatus.REGISTERED,
                                              OffsetDateTime.now(UTC).minusMinutes(5), "pending"),
                      new ExecutionStepEntity(null, longUnfinishedExec.getId(), ExecutionStatus.RUNNING,
                                              OffsetDateTime.now(UTC).minusMinutes(4), "running"));

        // Make steps for each execution.
        Map<UUID, Flux<ExecutionStepEntity>> stepsForExec = HashMap.of(
                finishedExec.getId(), shortFinishedSteps,
                shortUnfinishedExec.getId(), shortUnfinishedSteps,
                longUnfinishedExec.getId(), longUnfinishedSteps
        );

        // WHEN
        List<ExecutionEntity> timedoutExecs =
            // Save all entities in database
            entityBatchRepo.save(batch)
                .flatMapMany(persistedBatch -> entityExecRepo
                    .saveAll(Flux.just(finishedExec, shortUnfinishedExec, longUnfinishedExec))
                    .flatMap(exec -> entityStepRepo.saveAll(stepsForExec.getOrElse(exec.getId(), Flux.empty())))
                )
            .last()
            // find timed out executions
            .flatMapMany(s -> entityExecRepo.getTimedOutExecutions())
            // retrieve synchronously
            .collectList()
            .block();

        // THEN
        LOGGER.info("Timedout executions: {}", timedoutExecs);
        assertThat(timedoutExecs).hasSize(1);
        assertThat(timedoutExecs.get(0).getId()).isEqualTo(shortUnfinishedExec.getId());

    }

}