/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * Interface for all regards jobs
 * @author Léo Mieulet
 */
public interface IJob extends Runnable {

    /**
     * @return The job's priority
     */
    int getPriority();

    /**
     * @return The Job's results
     */
    Set<JobResult> getResults();

    /**
     * @return The job's {@link JobStatusInfo}
     */
    JobStatusInfo getStatus();

    /**
     * @return the job has a result ?
     */
    boolean hasResult();

    /**
     * @return the job need a workspace ?
     */
    boolean needWorkspace();

    /**
     * @param pPath set workspace path
     */
    void setWorkspace(Path pPath);

    /**
     * @param pJobInfoId save the jobInfo id inside the job
     */
    void setId(final UUID pJobInfoId);

    /**
     * set the parameters and should check if all needed parameters are specified
     * @param pParameters set job parameters
     */
    void setParameters(Set<JobParameter> pParameters) throws JobParameterMissingException, JobParameterInvalidException;
}
