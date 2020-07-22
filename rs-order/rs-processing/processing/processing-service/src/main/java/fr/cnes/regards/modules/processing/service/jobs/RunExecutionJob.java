package fr.cnes.regards.modules.processing.service.jobs;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

import java.util.Map;
import java.util.UUID;

public class RunExecutionJob extends AbstractJob<Void> {

    public static final String EXECUTION_ID_PARAM = "executionId";

    private UUID executionId;

    @Override public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        executionId = getValue(parameters, EXECUTION_ID_PARAM, UUID.class);
    }

    @Override public void run() {
        
    }
}
