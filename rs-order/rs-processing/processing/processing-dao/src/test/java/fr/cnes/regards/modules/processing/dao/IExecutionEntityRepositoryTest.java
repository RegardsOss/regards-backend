/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.RUNNING;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.SUCCESS;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import reactor.core.publisher.Flux;

public class IExecutionEntityRepositoryTest extends AbstractRepoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IExecutionEntityRepositoryTest.class);

    @Test
    public void test_timedout_executions() throws Exception {

        // First delete previous executions if any
        entityBatchRepo.deleteAll();

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
                .withLastUpdated(nowUtc().minusMinutes(4)).withTimeoutAfterMillis(1_000_000L).withPersisted(false);

        // WHEN
        List<ExecutionEntity> timedoutExecs =
                // Save all entities in database
                entityBatchRepo.save(batch).doOnError(t -> LOGGER.error("Could not save batch", t))
                        .flatMapMany(persistedBatch -> entityExecRepo
                                .saveAll(Flux.just(finishedExec, shortUnfinishedExec, longUnfinishedExec))
                                .doOnError(t -> LOGGER.error("Could not save execs", t)))
                        .last()
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

    @Test
    public void test_findByStatus() throws Exception {
        BatchEntity batch = randomInstance(BatchEntity.class).withPersisted(false);

        BatchEntity persistedBatch = entityBatchRepo.save(batch).block();

        List<ExecutionEntity> execs = entityExecRepo.saveAll(Flux.create(sink -> {
            for (int i = 0; i < 100; i++) {
                sink.next(randomInstance(ExecutionEntity.class).withId(UUID.randomUUID()).withBatchId(batch.getId())
                        .withTenant(batch.getTenant()).withUserEmail(batch.getUserEmail())
                        .withProcessBusinessId(batch.getProcessBusinessId()).withVersion(0).withPersisted(false)
                        .withCreated(nowUtc()).withLastUpdated(nowUtc())
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