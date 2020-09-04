package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static io.vavr.control.Option.none;

@Service
public class ExecutionServiceImpl implements IExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final IPExecutionRepository execRepo;
    private final IPBatchRepository batchRepo;
    private final IPProcessRepository processRepo;
    private final IPOutputFilesRepository outputFilesRepo;

    private final IExecutionLocalWorkdirService workdirService;
    private final ISharedStorageService storageService;

    @Autowired
    public ExecutionServiceImpl(
            IPExecutionRepository execRepo,
            IPBatchRepository batchRepo,
            IPProcessRepository processRepo,
            IExecutionLocalWorkdirService executionLocalWorkdirService,
            ISharedStorageService sharedStorageService,
            IPOutputFilesRepository outputFilesRepo
    ) {
        this.execRepo = execRepo;
        this.batchRepo = batchRepo;
        this.processRepo = processRepo;
        this.workdirService = executionLocalWorkdirService;
        this.storageService = sharedStorageService;
        this.outputFilesRepo = outputFilesRepo;
    }

    @Override
    public Mono<ExecutionContext> createContext(PExecution exec, PBatch batch, PProcess process) {
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

    @Override
    public Mono<PExecution> launchExecution(PExecutionRequestEvent request) {
        return makeExec(request)
            .flatMap(this::runEngine);
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

    @Override
    public Mono<Duration> estimateDuration(PBatch batch, Seq<ExecutionFileParameterValue> inputFiles) {
        return processRepo.findByBatch(batch)
            .map(PProcess::getRunningDurationForecast)
            .map(forecast -> {
                Long totalSize = inputFiles
                    .map(ExecutionFileParameterValue::getBytes)
                    .fold(0L, Long::sum);
                return forecast.expectedRunningDurationInBytes(totalSize);
            });
    }


    @Override
    public Mono<PExecution> runExecutable(UUID execId) {
        return execRepo.findById(execId)
                .flatMap(exec -> batchRepo.findById(exec.getBatchId())
                        .flatMap(batch -> processRepo.findByBatch(batch)
                                .flatMap(process -> createContext(exec, batch, process)
                                        .flatMap(process.getEngine()::run))));
    }


    @Override public IExecutionEventNotifier notifierFor(PExecution execution) {
        return event -> registerOutputFiles(execution, event.outputFiles())
            .flatMap(exec -> event.step()
                .map(step -> registerStep(execution.getId(), step))
                .getOrElse(() -> Mono.just(execution)));
    }

    private Mono<? extends PExecution> registerOutputFiles(PExecution exec, Seq<POutputFile> outputFiles) {
        if (outputFiles.isEmpty()) { return Mono.just(exec); }
        else {
            return outputFilesRepo.save(Flux.fromIterable(outputFiles))
                .last()
                .map(x -> exec);
        }
    }

    private Mono<PExecution> registerStep(UUID execId, PStep step) {
        return execRepo.findById(execId)
                .flatMap(exec -> addExecutionStep(exec, step));
    }

    private Mono<PExecution> addExecutionStep(PExecution exec, PStep step) {
        return execRepo.update(exec.addStep(step))
                .onErrorResume(OptimisticLockingFailureException.class, e -> {
                    LOGGER.warn("Optimistic locking failure when adding step {} to exec {}", step, exec.getId());
                    return Mono.defer(() -> execRepo.findById(exec.getId())
                            .flatMap(freshExec -> addExecutionStep(freshExec, step)));
                });
    }

}
