package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * An abstract job without parameter
 * @author oroussel
 */
public abstract class AbstractNoParamJob<R> extends AbstractJob<R> {

    @Override
    public void setParameters(Set<JobParameter> pParameters)
            throws JobParameterMissingException, JobParameterInvalidException {

    }
}
