package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.service.events.PExecutionRequestEvent;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

@Service
public class ExecutionServiceImpl implements IExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final IJobInfoService jobInfoService;

    private final IPExecutionRepository execRepo;
    private final IPBatchRepository batchRepo;
    private final IPProcessRepository processRepo;

    @Autowired
    public ExecutionServiceImpl(IJobInfoService jobInfoService,
            IPExecutionRepository execRepo, IPBatchRepository batchRepo, IPProcessRepository processRepo) {
        this.jobInfoService = jobInfoService;
        this.execRepo = execRepo;
        this.batchRepo = batchRepo;
        this.processRepo = processRepo;
    }

    @Override public Mono<PExecution> launchExecution(PExecutionRequestEvent request) {
        return makeExec(request)
            .flatMap(this::runEngine);
    }

    private Mono<PExecution> runEngine(PExecution exec) {
        return batchRepo.findById(exec.getBatchId()).flatMap(batch ->
            processRepo.findByBatch(batch).flatMap(process -> {
                ExecutionContext ctx = new ExecutionContext(exec, batch, process);
                return process.getEngine().run(ctx);
            })
        );
    }

    private Mono<PExecution> makeExec(PExecutionRequestEvent request) {
        return batchRepo.findById(request.getBatchId())
            .flatMap(batch -> estimateDuration(batch, request)
                .map(duration -> makeExecFromBatchAndDurationAndRequest(request, batch, duration)))
                .flatMap(execRepo::create);
    }

    private PExecution makeExecFromBatchAndDurationAndRequest(PExecutionRequestEvent request, PBatch batch, Duration duration) {
        return new PExecution(
            UUID.randomUUID(),
            batch.getId(),
            duration,
            request.getInputFiles(),
            List.of(new PStep(ExecutionStatus.REGISTERED, nowUtc(), "")),
            batch.getTenant(),
            batch.getUser(),
            batch.getProcessBusinessId(),
            batch.getProcessName(),
            null,
            null,
            0,
            false
        );
    }

    @Override public Mono<Duration> estimateDuration(PBatch batch, PExecutionRequestEvent request) {
        return processRepo.findByBatch(batch)
            .map(PProcess::getRunningDurationForecast)
            .map(forecast -> {
                Long totalSize = request.getInputFiles()
                    .map(ExecutionFileParameterValue::getBytes)
                    .fold(0L, Long::sum);
                return forecast.expectedRunningDurationInBytes(totalSize);
            });
    }

    @Override public Mono<PExecution> addExecutionStep(PExecution exec, PStep step) {
        return execRepo.update(exec.addStep(step))
            .onErrorResume(OptimisticLockingFailureException.class, e -> {
                LOGGER.warn("Optimistic locking failure when adding step {} to exec {}", step, exec.getId());
                return Mono.defer(() -> execRepo.findById(exec.getId())
                        .flatMap(freshExec -> addExecutionStep(freshExec, step)));
            });
    }
}
