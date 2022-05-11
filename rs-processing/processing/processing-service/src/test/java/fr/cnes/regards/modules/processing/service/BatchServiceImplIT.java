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
package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.service.exception.ProcessConstraintViolationsException;
import fr.cnes.regards.modules.processing.testutils.servlet.TestSpringConfiguration;
import io.vavr.collection.HashMap;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@ContextConfiguration(
        classes = { TestSpringConfiguration.class, AbstractProcessingServiceIT.Config.class }
)
public class BatchServiceImplIT extends AbstractProcessingServiceIT {

    @Test
    public void checkAndCreateBatch() {
        UUID processBusinessId = UUID.randomUUID();

        PBatchRequest batchRequest = new PBatchRequest(
            "bcid",
            processBusinessId,
                THE_TENANT,
                THE_USER,
                THE_ROLE,
            HashMap.empty(),
            HashMap.of("dataset1", new FileSetStatistics("dataset1", 5, 123456L))
        );

        PBatch batch = batchService.checkAndCreateBatch(new PUserAuth(THE_TENANT, THE_USER, THE_ROLE, THE_TOKEN), batchRequest).block();

        assertThat(batch.getCorrelationId()).isEqualTo(batchRequest.getCorrelationId());

        PBatch inDatabase = batchRepo.findById(batch.getId()).block();
        assertThat(inDatabase.getCorrelationId()).isEqualTo(batchRequest.getCorrelationId());
    }

    @Test
    public void checkAndCreateBatchError() {

        configureProcessUpdater(p -> p.withBatchChecker(ConstraintChecker.violation(() -> "wrong for some reason")));

        UUID processBusinessId = UUID.randomUUID();

        PBatchRequest batchRequest = new PBatchRequest(
                "bcid",
                processBusinessId,
                THE_TENANT,
                THE_USER,
                THE_ROLE,
                HashMap.empty(),
                HashMap.of("dataset1", new FileSetStatistics("dataset1", 5, 123456L))
        );

        AtomicReference<Throwable> throwableRef = new AtomicReference<>();

        batchService.checkAndCreateBatch(new PUserAuth(THE_TENANT, THE_USER, THE_ROLE, THE_TOKEN), batchRequest)
            .subscribeOn(Schedulers.immediate())
            .subscribe(
                b -> fail("Should not return a value"),
                e -> throwableRef.set(e));

        assertThat(throwableRef.get()).isNotNull();
        assertThat(throwableRef.get()).isInstanceOf(ProcessConstraintViolationsException.class);
        assertThat(((ProcessConstraintViolationsException)throwableRef.get()).getViolations()).anyMatch(v -> v.getMessage().equals("wrong for some reason"));

        assertThat(batchEntityRepo.findAll().collectList().block()).isEmpty();
    }

}