package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionStepRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.service.events.PExecutionRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

public class ExecutionServiceImpl implements IExecutionService {

    private final IJobInfoService jobInfoService;

    private final IPExecutionStepRepository stepRepo;
    private final IPExecutionRepository execRepo;
    private final IPBatchRepository batchRepo;
    private final IPProcessRepository processRepo;

    @Autowired
    public ExecutionServiceImpl(IJobInfoService jobInfoService, IPExecutionStepRepository stepRepo,
            IPExecutionRepository execRepo, IPBatchRepository batchRepo, IPProcessRepository processRepo) {
        this.jobInfoService = jobInfoService;
        this.stepRepo = stepRepo;
        this.execRepo = execRepo;
        this.batchRepo = batchRepo;
        this.processRepo = processRepo;
    }

    @Override public Mono<PExecution> launchExecution(PExecutionRequestEvent request) {
        return makeExec(request)
            .flatMap(this::runEngine)
                .flatMap(this::saveRegisteredStep);
    }

    private Mono<PExecution> runEngine(PExecution exec) {
        return batchRepo.findById(exec.getBatchId()).flatMap(batch ->
            processRepo.findByName(batch.getProcessName()).flatMap(process -> {
                ExecutionContext ctx = new ExecutionContext(exec, batch, process);
                return process.getEngine().run(ctx);
            })
        );
    }

    private Mono<PExecution> makeExec(PExecutionRequestEvent request) {
        return batchRepo.findById(request.getBatchId())
            .flatMap(batch -> estimateDuration(batch, request)
                .map(duration -> makeExecFromBatchAndDurationAndRequest(request, batch, duration)))
                .flatMap(execRepo::save);
    }

    private PExecution makeExecFromBatchAndDurationAndRequest(PExecutionRequestEvent request, PBatch batch, Duration duration) {
        return new PExecution(
            UUID.randomUUID(),
            batch.getId(),
            duration,
            request.getInputFiles(),
            false);
    }

    @Override public Mono<Duration> estimateDuration(PBatch batch, PExecutionRequestEvent request) {
        return processRepo.findByName(batch.getProcessName())
            .map(PProcess::getRunningDurationForecast)
            .map(forecast -> {
                Long totalSize = request.getInputFiles()
                    .map(ExecutionFileParameterValue::getBytes)
                    .fold(0L, Long::sum);
                return forecast.expectedRunningDurationInBytes(totalSize);
            });
    }

    @Override public Mono<PExecutionStepSequence> saveExecutionStep(PExecutionStep step) {
        return stepRepo.save(step)
                .map(PExecutionStep::getExecutionId)
                .flatMap(stepRepo::findAllForExecution);
    }

    public Mono<PExecution> saveRegisteredStep(PExecution exec) {
        return saveExecutionStep(new PExecutionStep(null, exec.getId(), ExecutionStatus.REGISTERED, nowUtc(), ""))
                .map(s -> exec);
    }
}
