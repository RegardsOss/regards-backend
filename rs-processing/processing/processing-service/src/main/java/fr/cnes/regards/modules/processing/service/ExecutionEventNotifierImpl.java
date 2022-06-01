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

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.dto.POutputFileDTO;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.handlers.IExecutionResultEventSender;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.PERSIST_EXECUTION_STEP_ERROR;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.PERSIST_OUTPUT_FILES_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.addInContext;

/**
 * This class is the implementation for the {@link IExecutionEventNotifier} interface.
 *
 * @author gandrieu
 */
public class ExecutionEventNotifierImpl implements IExecutionEventNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEventNotifierImpl.class);

    private final IPExecutionRepository execRepo;

    private final IPOutputFilesRepository outputFilesRepo;

    private final IExecutionResultEventSender execResultSender;

    private final IPProcessRepository processRepo;

    private PExecution execution;

    public ExecutionEventNotifierImpl(IPExecutionRepository execRepo,
                                      IPOutputFilesRepository outputFilesRepo,
                                      IExecutionResultEventSender execResultSender,
                                      IPProcessRepository processRepo,
                                      PExecution execution) {
        this.execRepo = execRepo;
        this.outputFilesRepo = outputFilesRepo;
        this.execResultSender = execResultSender;
        this.processRepo = processRepo;
        this.execution = execution;
    }

    @Override
    public Mono<PExecution> notifyEvent(ExecutionEvent event) {
        return execRepo.findById(execution.getId())
                       .flatMap(exec -> registerOutputFiles(exec, event.outputFiles()))
                       .flatMap(exec -> registerStep(event, exec))
                       .flatMap(exec -> sendResult(event, exec))
                       .subscriberContext(addInContext(PExecution.class, execution));
    }

    private Mono<PExecution> sendResult(ExecutionEvent event, PExecution exec) {
        if (event instanceof ExecutionEvent.FinalEvent) {
            return sendFinalResult((ExecutionEvent.FinalEvent) event, exec);
        } else {
            return Mono.just(exec);
        }
    }

    private Mono<PExecution> sendFinalResult(ExecutionEvent.FinalEvent event, PExecution exec) {
        if (event.isFinal()) {
            UUID processBusinessId = exec.getProcessBusinessId();
            return processRepo.findByTenantAndProcessBusinessID(exec.getTenant(), processBusinessId)
                              .flatMap(process -> {
                                  PExecutionResultEvent resultEvent = new PExecutionResultEvent(exec.getId(),
                                                                                                exec.getExecutionCorrelationId(),
                                                                                                exec.getBatchId(),
                                                                                                exec.getBatchCorrelationId(),
                                                                                                processBusinessId,
                                                                                                process.getProcessInfo(),
                                                                                                event.getStep()
                                                                                                     .getStatus(),
                                                                                                event.getOutputFiles()
                                                                                                     .map(POutputFileDTO::toDto),
                                                                                                List.of(event.getStep()
                                                                                                             .getMessage()));
                                  return execResultSender.send(exec.getTenant(), resultEvent).map(x -> exec);
                              });
        } else {
            return Mono.just(exec);
        }
    }

    private Mono<PExecution> registerOutputFiles(PExecution exec, Seq<POutputFile> outputFiles) {
        if (outputFiles.isEmpty()) {
            return Mono.just(exec);
        } else {
            return outputFilesRepo.save(Flux.fromIterable(outputFiles))
                                  .last()
                                  .map(x -> exec)
                                  .onErrorMap(mustWrap(),
                                              t -> new PersistOutputFilesException(exec,
                                                                                   "Persisting output file failed: "
                                                                                   + t.getMessage(),
                                                                                   t));
        }
    }

    private Mono<PExecution> registerStep(ExecutionEvent event, PExecution exec) {
        return event.step().map(step -> registerStep(exec.getId(), step)).getOrElse(() -> Mono.just(exec));
    }

    private Mono<PExecution> registerStep(UUID execId, PStep step) {
        return execRepo.findById(execId).flatMap(exec -> addExecutionStep(exec, step));
    }

    private Mono<PExecution> addExecutionStep(PExecution exec, PStep step) {
        return execRepo.update(exec.addStep(step)).onErrorResume(OptimisticLockingFailureException.class, e -> {
            LOGGER.warn("Optimistic locking failure when adding step {} to exec {}", step, exec.getId());
            return Mono.defer(() -> execRepo.findById(exec.getId())
                                            .flatMap(freshExec -> addExecutionStep(freshExec, step)));
        }).onErrorMap(mustWrap(), t -> new PersistExecutionStepException(exec, "Persisting step failed: " + step, t));
    }

    public static class PersistOutputFilesException extends ProcessingExecutionException {

        public PersistOutputFilesException(PExecution exec, String message, Throwable throwable) {
            super(PERSIST_OUTPUT_FILES_ERROR, exec, message, throwable);
        }
    }

    public static class PersistExecutionStepException extends ProcessingExecutionException {

        public PersistExecutionStepException(PExecution exec, String message, Throwable throwable) {
            super(PERSIST_EXECUTION_STEP_ERROR, exec, message, throwable);
        }
    }

}
