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
package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PBatchRepositoryImplIT extends AbstractRepoIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PBatchRepositoryImplIT.class);

    public static final int CONCURRENT_RUNS = 200;

    @Test
    public void batch_save_then_getOne() throws Exception {
        java.util.List<Runnable> asserts = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(CONCURRENT_RUNS);

        Flux.range(0, CONCURRENT_RUNS).flatMap(i -> {
            //////////////////////////
            // TESTING BATCH SAVE/FIND
            //////////////////////////
            // GIVEN
            PBatch newBatch = randomInstance(PBatch.class).asNew();
            LOGGER.info("ATTEMPT {}, PBatch used is: {}", i, newBatch);

            // WHEN
            return saveBatch(i, newBatch).flatMap(persistedBatch -> {
                return findBatch(i, persistedBatch).flatMap(foundBatch -> {

                    // THEN
                    asserts.add(() -> assertThat(foundBatch).isEqualTo(persistedBatch));

                    //////////////////////////
                    // TESTING EXEC SAVE/FIND
                    //////////////////////////
                    // GIVEN
                    PExecution newExec = asNew(randomInstance(PExecution.class)).withBatchId(persistedBatch.getId())
                                                                                .withExpectedDuration(Duration.ofSeconds(
                                                                                    10));

                    // WHEN
                    return saveExec(i, newExec).flatMap(persistedExec -> {
                        return findExec(i, persistedExec).flatMap(foundExec -> {

                            // THEN
                            asserts.add(() -> assertThat(foundExec).isEqualTo(persistedExec));

                            //////////////////////////
                            // ADDING A STEP TO EXEC
                            //////////////////////////
                            // GIVEN
                            PStep newStep = randomInstance(PStep.class).clean().withTime(nowUtc().withNano(0));

                            // WHEN
                            return addStep(i, persistedExec, newStep).flatMap(exec -> {
                                // THEN
                                Seq<PStep> steps = exec.getSteps();
                                PStep foundStep = steps.last();
                                asserts.add(() -> {
                                    assertThat(steps).hasSize(1);
                                    assertThat(foundStep).isEqualTo(newStep);
                                });
                                return Unit.mono();
                            });
                        });
                    });
                });
            }).doOnTerminate(latch::countDown);
        }).doOnError(t -> {
            asserts.add(() -> {
                LOGGER.error(t.getMessage(), t);
                fail(t.getMessage());
            });
            Stream.range(0, latch.getCount()).forEach(j -> latch.countDown());
        }).subscribe();

        latch.await(20L, TimeUnit.SECONDS);

        LOGGER.info("Asserts to run: {}", asserts.size());
        asserts.forEach(Runnable::run);

        LOGGER.info("Done");
    }

}