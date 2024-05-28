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
package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import io.vavr.collection.Array;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class IBatchEntityRepositoryIT extends AbstractRepoIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IBatchEntityRepositoryIT.class);

    @Test
    public void test_cleanable_batches() throws Exception {
        // GIVEN
        List<ExecutionEntity> executions = new ArrayList<>();

        BatchEntity batchRecentExec = randomInstance(BatchEntity.class).withPersisted(false);
        BatchEntity batchRunningExec = randomInstance(BatchEntity.class).withPersisted(false);
        BatchEntity batchNoExec = randomInstance(BatchEntity.class).withPersisted(false);
        BatchEntity batchToBeDeleted = randomInstance(BatchEntity.class).withPersisted(false);

        entityBatchRepo.saveAll(Array.of(batchRecentExec, batchRunningExec, batchToBeDeleted, batchNoExec)).blockLast();
        {
            generateExecution(executions, batchRecentExec, SUCCESS, nowUtc().minusMonths(3)); // Could be deleted
            generateExecution(executions, batchRecentExec, FAILURE, nowUtc().minusMinutes(3)); // Too soon to be deleted
        }
        {
            generateExecution(executions, batchRunningExec, SUCCESS, nowUtc().minusMonths(4)); // Could be deleted
            generateExecution(executions, batchRunningExec, RUNNING, nowUtc().minusMonths(5)); // Still running
        }
        {
            generateExecution(executions, batchToBeDeleted, SUCCESS, nowUtc().minusMonths(4)); // Could be deleted
            generateExecution(executions, batchToBeDeleted, TIMED_OUT, nowUtc().minusMonths(5)); // Could be deleted
        }
        entityExecRepo.saveAll(executions).blockLast();

        // WHEN looking for batches ripe for deletion
        List<BatchEntity> entitiesToBeDeleted = entityBatchRepo.getCleanableBatches(Duration.ofDays(7).toMillis())
                                                               .collectList()
                                                               .block();

        // THEN
        assertThat(entitiesToBeDeleted).hasSize(2);
        assertThat(entitiesToBeDeleted.stream().map(BatchEntity::getId)).containsExactlyInAnyOrder(batchNoExec.getId(),
                                                                                                   batchToBeDeleted.getId());

        // WHEN deleting these batches
        entityBatchRepo.deleteAll(entitiesToBeDeleted).block();

        // Executions have been deleted by cascade
        List<ExecutionEntity> remainingExecs = entityExecRepo.findAllById(Flux.fromIterable(executions)
                                                                              .map(ExecutionEntity::getId))
                                                             .collectList()
                                                             .block();
        assertThat(remainingExecs).hasSize(4);
        assertThat(remainingExecs.stream().map(ExecutionEntity::getBatchId).distinct()).containsExactlyInAnyOrder(
            batchRecentExec.getId(),
            batchRunningExec.getId());

    }

    private void generateExecution(List<ExecutionEntity> entities,
                                   BatchEntity batchRecentExec,
                                   ExecutionStatus success,
                                   OffsetDateTime lastUpdated) {
        entities.add(randomInstance(ExecutionEntity.class).withBatchId(batchRecentExec.getId())
                                                          .withTenant(batchRecentExec.getTenant())
                                                          .withUserEmail(batchRecentExec.getUserEmail())
                                                          .withProcessBusinessId(batchRecentExec.getProcessBusinessId())
                                                          .withCurrentStatus(success)
                                                          .withLastUpdated(lastUpdated)
                                                          .withTimeoutAfterMillis(1_000L)
                                                          .withPersisted(false));
    }

    @Test
    public void test_count_executions() throws Exception {
        // GIVEN
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        // This execution has succeeded, and so, it will not be found as timed out.
        ExecutionEntity finishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                                                                            .withTenant(batch.getTenant())
                                                                            .withUserEmail(batch.getUserEmail())
                                                                            .withProcessBusinessId(batch.getProcessBusinessId())
                                                                            .withCurrentStatus(SUCCESS)
                                                                            .withLastUpdated(nowUtc().minusMinutes(3))
                                                                            .withTimeoutAfterMillis(1_000L)
                                                                            .withPersisted(false);

        // This execution has not terminated and has short timeout, and so, it will be found as timed out.
        ExecutionEntity shortUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                                                                                   .withTenant(batch.getTenant())
                                                                                   .withUserEmail(batch.getUserEmail())
                                                                                   .withProcessBusinessId(batch.getProcessBusinessId())
                                                                                   .withCurrentStatus(RUNNING)
                                                                                   .withLastUpdated(nowUtc().minusMinutes(
                                                                                       4))
                                                                                   .withTimeoutAfterMillis(1_000L)
                                                                                   .withPersisted(false);
        LOGGER.info("Test should find this execution as timedout: {}", shortUnfinishedExec.getId());

        // This execution has not terminated but has long timeout, and so, it will not be found as timed out.
        ExecutionEntity longUnfinishedExec = randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                                                                                  .withTenant(batch.getTenant())
                                                                                  .withUserEmail(batch.getUserEmail())
                                                                                  .withProcessBusinessId(batch.getProcessBusinessId())
                                                                                  .withCurrentStatus(RUNNING)
                                                                                  .withLastUpdated(nowUtc().minusHours(4))
                                                                                  .withTimeoutAfterMillis(1_000_000L)
                                                                                  .withPersisted(false);

        BatchEntity batchEntity = entityBatchRepo.save(batch)
                                                 .doOnError(t -> LOGGER.error("Could not save batch", t))
                                                 .block();

        List<ExecutionEntity> execEntities = entityExecRepo.saveAll(Flux.just(finishedExec,
                                                                              shortUnfinishedExec,
                                                                              longUnfinishedExec))
                                                           .doOnError(t -> LOGGER.error("Could not save execs", t))
                                                           .collectList()
                                                           .block();

        // WHEN
        List<BatchEntity> entities = entityBatchRepo.findByProcessBusinessId(batch.getProcessBusinessId())
                                                    .collectList()
                                                    .block();

        // THEN
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getId()).isEqualTo(batch.getId());

        // WHEN
        Integer countSuccess = domainExecRepo.countAllForMonitoringSearch(batch.getTenant(),
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
        assertThat(countSuccess).isEqualTo(1);

        // WHEN
        Integer countRunning = domainExecRepo.countAllForMonitoringSearch(batch.getTenant(),
                                                                          new SearchExecutionEntityParameters().withStatusIncluded(
                                                                                                                   Arrays.asList(RUNNING))
                                                                                                               .withCreationDateBefore(
                                                                                                                   nowUtc().plusHours(
                                                                                                                       5))
                                                                                                               .withCreationDateAfter(
                                                                                                                   nowUtc().minusHours(
                                                                                                                       5)))
                                             .block();

        // THEN
        assertThat(countRunning).isEqualTo(2);

        // WHEN
        Integer countRunningTimed = domainExecRepo.countAllForMonitoringSearch(batch.getTenant(),
                                                                               new SearchExecutionEntityParameters().withStatusIncluded(
                                                                                                                        Arrays.asList(RUNNING))
                                                                                                                    .withCreationDateBefore(
                                                                                                                        nowUtc().plusHours(
                                                                                                                            1))
                                                                                                                    .withCreationDateAfter(
                                                                                                                        nowUtc().minusHours(
                                                                                                                            1)))
                                                  .block();
        assertThat(countRunningTimed).isEqualTo(1);

    }

    @Test
    public void test_findByStatus() throws Exception {
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        BatchEntity persistedBatch = entityBatchRepo.save(batch).block();

        List<ExecutionEntity> execs = entityExecRepo.saveAll(Flux.create(sink -> {
            for (int i = 0; i < 100; i++) {
                sink.next(randomInstance(ExecutionEntity.class).withId(UUID.randomUUID())
                                                               .withBatchId(batch.getId())
                                                               .withTenant(batch.getTenant())
                                                               .withUserEmail(batch.getUserEmail())
                                                               .withProcessBusinessId(batch.getProcessBusinessId())
                                                               .withVersion(0)
                                                               .withPersisted(false)
                                                               .withCreated(nowUtc())
                                                               .withLastUpdated(nowUtc())
                                                               .withCurrentStatus(randomInstance(ExecutionStatus.class)));
            }
            sink.complete();
        })).collectList().block();

        List<ExecutionEntity> foundExecs1010 = entityExecRepo.findByTenantAndCurrentStatusIn(batch.getTenant(),
                                                                                             singletonList(RUNNING),
                                                                                             PageRequest.of(0, 10))
                                                             .collectList()
                                                             .block();

        LOGGER.info("Found execs page(10,10): {}", foundExecs1010);

        List<ExecutionEntity> foundExecs1020 = entityExecRepo.findByTenantAndCurrentStatusIn(batch.getTenant(),
                                                                                             singletonList(RUNNING),
                                                                                             PageRequest.of(1, 10))
                                                             .collectList()
                                                             .block();

        LOGGER.info("Found execs page(10,10): {}", foundExecs1020);
        LOGGER.info("Done");

        // TODO add assertions
    }

}