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
package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.constraints.Violation;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IBatchService;
import fr.cnes.regards.modules.processing.service.exception.ProcessConstraintViolationsException;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * This class is the implementation for the {@link IBatchService} interface.
 *
 * @author gandrieu
 */
@Service
public class BatchServiceImpl implements IBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchServiceImpl.class);
    
    private final IPProcessRepository processRepo;

    private final IPBatchRepository batchRepo;

    private final long batchRipeForDeleteAgeMs;

    @Autowired
    public BatchServiceImpl(
            IPProcessRepository processRepo,
            IPBatchRepository batchRepo,
            @Value("${regards.processing.cleanup.batch.age.ms:604800000}") long batchRipeForDeleteAgeMs
    ) {
        this.processRepo = processRepo;
        this.batchRepo = batchRepo;
        this.batchRipeForDeleteAgeMs = batchRipeForDeleteAgeMs;
    }

    @Override
    public Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data) {
        return processRepo.findByTenantAndProcessBusinessID(auth.getTenant(), data.getProcessBusinessId())
                .flatMap(p -> createBatch(p, data).flatMap(b -> checkBatch(p, b)))
                .flatMap(batchRepo::save);
    }

    @Scheduled(fixedDelayString = "${regards.processing.cleanup.batch.rate.ms:3600000}")
    public void deleteOldBatches() {
        batchRepo.deleteAllFinishedForMoreThan(batchRipeForDeleteAgeMs)
                .subscribeOn(Schedulers.immediate())
                .doOnError(t -> LOGGER.error("Failed to delete old batches", t))
                .subscribe();
    }

    private Mono<Seq<Violation>> check(PProcess process, PBatch batch) {
        return process.getBatchChecker().validate(batch);
    }

    private Mono<PBatch> createBatch(PProcess process, PBatchRequest data) {
        return Mono.just(new PBatch(
            data.getCorrelationId(),
            UUID.randomUUID(),
            process.getProcessId(),
            data.getTenant(),
            data.getUser(),
            data.getUserRole(),
            paramValues(data),
            data.getFilesetsByDataset(),
            false)
        );
    }

    private Seq<ExecutionStringParameterValue> paramValues(PBatchRequest data) {
        return data.getParameters().toList().map(entry -> new ExecutionStringParameterValue(entry._1, entry._2));
    }

    private Mono<? extends PBatch> checkBatch(PProcess process, PBatch batch) {
        return check(process, batch).flatMap(vs -> {
            if (vs.isEmpty()) {
                return Mono.just(batch);
            } else {
                return Mono.error(new ProcessConstraintViolationsException(vs));
            }
        }).switchIfEmpty(Mono.just(batch));
    }

}
