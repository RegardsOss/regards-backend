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

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.StepEntity;
import fr.cnes.regards.modules.processing.entity.Steps;
import fr.cnes.regards.modules.processing.utils.random.RandomUtils;
import io.vavr.collection.List;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.REGISTERED;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.SUCCESS;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.toEpochMillisUTC;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

public class POutputFileRepositoryImplIT extends AbstractRepoIT {

    @Test
    public void test_save_findByExecId() throws Exception {
        // GIVEN
        ExecutionEntity exec = makeExecutionEntity();

        List<POutputFile> files = RandomUtils.randomList(POutputFile.class, 10)
                                             .map(o -> o.withExecId(exec.getId()).withPersisted(false));

        List<POutputFile> persistedOutputFiles = domainOutputFilesRepo.save(Flux.fromIterable(files))
                                                                      .collect(List.collector())
                                                                      .block();

        // WHEN
        List<POutputFile> queriedOutputFiles = domainOutputFilesRepo.findByExecId(exec.getId())
                                                                    .collect(List.collector())
                                                                    .block();

        // THEN
        assertThat(queriedOutputFiles).containsExactlyInAnyOrder(persistedOutputFiles.toJavaArray(POutputFile[]::new));

    }

    @Test
    public void test_downloadedNotDeleted() throws Exception {
        // GIVEN
        ExecutionEntity exec = makeExecutionEntity();

        POutputFile notDwnNotDlt = randomInstance(POutputFile.class).withExecId(exec.getId())
                                                                    .withDownloaded(false)
                                                                    .withDeleted(false)
                                                                    .withPersisted(false);
        POutputFile dwnNotDlt = randomInstance(POutputFile.class).withExecId(exec.getId())
                                                                 .withDownloaded(true)
                                                                 .withDeleted(false)
                                                                 .withPersisted(false);
        POutputFile dwnDlt = randomInstance(POutputFile.class).withExecId(exec.getId())
                                                              .withDownloaded(true)
                                                              .withDeleted(true)
                                                              .withPersisted(false);

        domainOutputFilesRepo.save(Flux.just(notDwnNotDlt, dwnNotDlt, dwnDlt)).collect(List.collector()).block();

        // WHEN
        List<POutputFile> fetched = domainOutputFilesRepo.findByDownloadedIsTrueAndDeletedIsFalse()
                                                         .doOnNext(o -> LOGGER.info(o.toString()))
                                                         .filter(o -> o.getExecId().equals(exec.getId()))
                                                         .collect(List.collector())
                                                         .block();

        // THEN
        assertThat(fetched.map(POutputFile::getId)).containsExactlyInAnyOrder(dwnNotDlt.getId());
    }

    private ExecutionEntity makeExecutionEntity() {
        BatchEntity batch = entityBatchRepo.save(randomInstance(BatchEntity.class).withPersisted(false)).block();

        OffsetDateTime lastUpdated = nowUtc().minusMinutes(3);
        return entityExecRepo.save(randomInstance(ExecutionEntity.class).withBatchId(batch.getId())
                                                                        .withTenant(batch.getTenant())
                                                                        .withUserEmail(batch.getUserEmail())
                                                                        .withProcessBusinessId(batch.getProcessBusinessId())
                                                                        .withCurrentStatus(SUCCESS)
                                                                        .withLastUpdated(lastUpdated)
                                                                        .withTimeoutAfterMillis(1_000L)
                                                                        .withSteps(Steps.of(new StepEntity(REGISTERED,
                                                                                                           toEpochMillisUTC(
                                                                                                               lastUpdated),
                                                                                                           "registered msg")))
                                                                        .withPersisted(false)).block();
    }

}