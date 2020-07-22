package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

public class LaunchExecutionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchExecutionJob.class);
    public static final String EXEC_ID_PARAM = "execId";

    @Autowired private IPExecutionRepository execRepo;
    @Autowired private IPBatchRepository batchRepo;
    @Autowired private IPProcessRepository processRepo;

    private UUID execId;

    @Override public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        execId = getValue(parameters, EXEC_ID_PARAM);
    }

    @Override public void run() {
        LOGGER.info("exec={} - LaunchExecutionJob start", execId);
        execRepo.findById(execId)
            .flatMap(exec -> batchRepo.findById(exec.getBatchId())
                 .flatMap(batch -> processRepo.findByName(batch.getProcessName())
                     .flatMap(process -> {
                         ExecutionContext ctx = new ExecutionContext(exec, batch, process);
                         return process.getEngine().run(ctx);
                     })
                 )
            )
            .subscribe(
                exec -> LOGGER.info("exec={} - LaunchExecutionJob success", execId),
                err -> LOGGER.error("exec={} - LaunchExecutionJob failure: {}", execId, err.getMessage())
            );
    }
}
