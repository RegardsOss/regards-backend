package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import fr.cnes.regards.modules.processing.domain.handlers.IExecutionResultEventSender;
import fr.cnes.regards.modules.processing.domain.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.domain.storage.ISharedStorageService;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.NOTIFY_TIMEOUT_ERROR;

@Service
public class ExecutionServiceImpl implements IExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);
    
    private final IPExecutionRepository execRepo;
    private final IPBatchRepository batchRepo;
    private final IPProcessRepository processRepo;
    private final IExecutionLocalWorkdirService workdirService;

    private final ISharedStorageService storageService;
    private final IPOutputFilesRepository outputFilesRepo;
    private final IExecutionResultEventSender execResultSender;

    @Autowired
    public ExecutionServiceImpl(IPExecutionRepository execRepo, IPBatchRepository batchRepo,
            IPProcessRepository processRepo, IExecutionLocalWorkdirService executionLocalWorkdirService,
            ISharedStorageService sharedStorageService, IPOutputFilesRepository outputFilesRepo,
            IExecutionResultEventSender execResultSender) {
        this.execRepo = execRepo;
        this.batchRepo = batchRepo;
        this.processRepo = processRepo;
        this.workdirService = executionLocalWorkdirService;
        this.storageService = sharedStorageService;
        this.outputFilesRepo = outputFilesRepo;
        this.execResultSender = execResultSender;
    }

    @Override
    public Mono<PExecution> launchExecution(PExecutionRequestEvent request) {
        return makeExec(request)
            .flatMap(this::runEngine);
    }

    @Override
    public Mono<PExecution> runExecutable(UUID execId) {
        return execRepo.findById(execId)
            .flatMap(exec -> batchRepo.findById(exec.getBatchId())
                .flatMap(batch -> processRepo.findByBatch(batch)
                    .flatMap(process -> createContext(exec, batch, process)
                        .flatMap(process.getEngine()::run))));
    }

    @Scheduled(
        fixedRate = 60L * 60L * 1000L, // Every hour TODO make configurable?
        fixedDelay = 30L * 60L * 1000L // TODO add jitter
    )
    @Override public void scheduledTimeoutNotify() {
        execRepo.getTimedOutExecutions()
            .subscribe(
                this::notifyTimeout,
                t -> LOGGER.error(t.getMessage(), t)
            );
    }

    private void notifyTimeout(PExecution execution) {
        LOGGER.info("exec={} - Notifying timeout", execution.getId());
        notifierFor(execution)
            .notifyEvent(ExecutionEvent.event(PStep.timeout("")))
            .subscribe(
                exec -> LOGGER.info("exec={} - Timeout notified", exec.getId()),
                t -> LOGGER.error(t.getMessage(), t)
            );
    }

    private Mono<PExecution> runEngine(PExecution exec) {
        return batchRepo.findById(exec.getBatchId())
            .flatMap(batch -> processRepo.findByBatch(batch)
                .flatMap(process -> createContext(exec, batch, process)
                    .doOnNext(process.getEngine()::run)
                    .map(x -> exec)));
    }

    private Mono<PExecution> makeExec(PExecutionRequestEvent request) {
        return batchRepo.findById(request.getBatchId())
            .flatMap(batch -> estimateDuration(batch, request.getInputFiles())
                .map(duration -> makeExecFromBatchAndDurationAndRequest(request, batch, duration)))
            .flatMap(execRepo::create);
    }

    private PExecution makeExecFromBatchAndDurationAndRequest(PExecutionRequestEvent request, PBatch batch, Duration duration) {
        return PExecution.create(
            batch.getId(),
            duration,
            request.getInputFiles(),
            batch.getTenant(),
            batch.getUser(),
            batch.getProcessBusinessId(),
            batch.getProcessName()
        );
    }

    private Mono<Duration> estimateDuration(PBatch batch, Seq<ExecutionFileParameterValue> inputFiles) {
        return processRepo.findByBatch(batch)
            .map(PProcess::getRunningDurationForecast)
            .map(forecast -> {
                Long totalSize = inputFiles
                    .map(ExecutionFileParameterValue::getBytes)
                    .fold(0L, Long::sum);
                return forecast.expectedRunningDurationInBytes(totalSize);
            });
    }

    private Mono<ExecutionContext> createContext(PExecution exec, PBatch batch, PProcess process) {
        return workdirService.makeWorkdir(exec).map(workdir ->
            new ExecutionContext(
                workdirService,
                storageService,
                exec,
                batch,
                process,
                workdir,
                notifierFor(exec)
            ));
    }

    private IExecutionEventNotifier notifierFor(PExecution execution) {
        return new ExecutionEventNotifierImpl(execRepo, outputFilesRepo, execResultSender, execution);
    }

    public static class NotifyTimeoutException extends ProcessingExecutionException {
        public NotifyTimeoutException(PExecution exec, String message,
                Throwable throwable) {
            super(NOTIFY_TIMEOUT_ERROR, exec, message, throwable);
        }
    }

}
