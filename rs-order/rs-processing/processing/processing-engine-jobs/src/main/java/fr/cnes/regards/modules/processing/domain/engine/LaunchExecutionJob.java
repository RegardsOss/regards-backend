package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.processing.service.IExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

public class LaunchExecutionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchExecutionJob.class);
    public static final String EXEC_ID_PARAM = "execId";

    @Autowired private IExecutionService execService;

    private UUID execId;

    @Override public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        execId = getValue(parameters, EXEC_ID_PARAM);
    }

    @Override public void run() {
        LOGGER.info("exec={} - LaunchExecutionJob start", execId);
        execService.runExecutable(execId)
            .subscribe(
                exec -> LOGGER.info("exec={} - LaunchExecutionJob success", execId),
                err -> LOGGER.error("exec={} - LaunchExecutionJob failure: {}", execId, err.getMessage())
            );
    }


}
