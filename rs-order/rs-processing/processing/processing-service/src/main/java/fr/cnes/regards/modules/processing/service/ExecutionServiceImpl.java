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

import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.handlers.IExecutionResultEventSender;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * This class is the implementation for the {@link IExecutionService} interface.
 *
 * @author gandrieu
 */
@Service
public class ExecutionServiceImpl implements IExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final IPExecutionRepository execRepo;

    private final IPBatchRepository batchRepo;

    private final IPProcessRepository processRepo;

    private final IPOutputFilesRepository outputFilesRepo;

    private final IExecutionResultEventSender execResultSender;

    @Autowired
    public ExecutionServiceImpl(IPExecutionRepository execRepo, IPBatchRepository batchRepo,
            IPProcessRepository processRepo, IPOutputFilesRepository outputFilesRepo,
            IExecutionResultEventSender execResultSender) {
        this.execRepo = execRepo;
        this.batchRepo = batchRepo;
        this.processRepo = processRepo;
        this.outputFilesRepo = outputFilesRepo;
        this.execResultSender = execResultSender;
    }

    @Override
    public Mono<ExecutionContext> createContext(UUID execId) {
        return execRepo.findById(execId)
                .flatMap(exec -> batchRepo.findById(exec.getBatchId()).flatMap(batch -> processRepo.findByBatch(batch)
                        .flatMap(process -> createContext(exec, batch, process))));
    }

    @Override
    public Mono<PExecution> launchExecution(PExecutionRequestEvent request) {
        return makeExec(request).flatMap(this::runEngine);
    }

    @Scheduled(
            cron = "${regards.processing.executions.timedout.cleanup.cron:0 */6 * * *}" // every six hours by default
    )
    @Override
    public void scheduledTimeoutNotify() {
        execRepo.getTimedOutExecutions().subscribe(this::notifyTimeout, t -> LOGGER.error(t.getMessage(), t));
    }

    private void notifyTimeout(PExecution execution) {
        LOGGER.info("exec={} - Notifying timeout", execution.getId());
        notifierFor(execution).notifyEvent(ExecutionEvent.event(PStep.timeout("")))
                .subscribe(exec -> LOGGER.info("exec={} - Timeout notified", exec.getId()),
                           t -> LOGGER.error(t.getMessage(), t));
    }

    private Mono<PExecution> runEngine(PExecution exec) {
        return batchRepo.findById(exec.getBatchId())
            .flatMap(batch -> processRepo.findByBatch(batch)
                .flatMap(process -> createContext(exec, batch, process)
                    .flatMap(ctx -> process.getEngine().run(ctx))));
    }

    private Mono<PExecution> makeExec(PExecutionRequestEvent request) {
        return batchRepo.findById(request.getBatchId())
                .flatMap(batch -> estimateDuration(batch, request.getInputFiles())
                        .map(duration -> makeExecFromBatchAndDurationAndRequest(request, batch, duration)))
                .flatMap(execRepo::create);
    }

    private PExecution makeExecFromBatchAndDurationAndRequest(PExecutionRequestEvent request, PBatch batch, Duration duration) {
        return PExecution.create(
            request.getExecutionCorrelationId(),
            batch.getId(),
            batch.getCorrelationId(),
            duration,
            request.getInputFiles(),
            batch.getTenant(),
            batch.getUser(),
            batch.getProcessBusinessId()
        );
    }

    private Mono<Duration> estimateDuration(PBatch batch, Seq<PInputFile> inputFiles) {
        return processRepo.findByBatch(batch).map(PProcess::getRunningDurationForecast).map(forecast -> {
            Long totalSize = inputFiles.map(PInputFile::getBytes).fold(0L, Long::sum);
            return forecast.expectedRunningDurationInBytes(totalSize);
        });
    }

    private Mono<ExecutionContext> createContext(PExecution exec, PBatch batch, PProcess process) {
        return Mono.just(new ExecutionContext(exec, batch, process, notifierFor(exec)));
    }

    private IExecutionEventNotifier notifierFor(PExecution execution) {
        return new ExecutionEventNotifierImpl(execRepo, outputFilesRepo, execResultSender, processRepo, execution);
    }

}
