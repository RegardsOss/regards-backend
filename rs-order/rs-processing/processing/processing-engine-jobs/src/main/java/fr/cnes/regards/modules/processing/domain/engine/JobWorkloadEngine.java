package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.jobs.service.JobService;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

@Component
public class JobWorkloadEngine implements IWorkloadEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobWorkloadEngine.class);

    private final IJobInfoService jobInfoService;
    private final IJobService jobService;
    private final IWorkloadEngineRepository engineRepo;

    @Autowired
    public JobWorkloadEngine(IJobInfoService jobInfoService, IJobService jobService, IWorkloadEngineRepository engineRepo) {
        this.jobInfoService = jobInfoService;
        this.jobService = jobService;
        this.engineRepo = engineRepo;
    }

    @Override public String name() {
        return "JOB";
    }

    @PostConstruct
    public void selfRegisterInRepo() { engineRepo.register(this); }

    @Override public Mono<PExecution> run(ExecutionContext context) {
        return Mono.fromCallable(() -> {
            JobInfo jobInfo = new JobInfo(
                false,
                0,
                List.of(new JobParameter(LaunchExecutionJob.EXEC_ID_PARAM, context.getExec().getId())).toJavaSet(),
                context.getBatch().getUser(),
                LaunchExecutionJob.class.getName()
            );

            jobInfo.setExpirationDate(nowUtc().plus(context.getExec().getExpectedDuration()));
            JobInfo pendingJob = jobInfoService.createAsPending(jobInfo);

            LOGGER.info("correlationId={} batch={} exec={} - Job created with ID {}",
                context.getBatch().getCorrelationId(),
                context.getBatch().getId(),
                context.getExec().getId(),
                pendingJob.getId()
            );

            return context.getExec();
        });

    }
}
