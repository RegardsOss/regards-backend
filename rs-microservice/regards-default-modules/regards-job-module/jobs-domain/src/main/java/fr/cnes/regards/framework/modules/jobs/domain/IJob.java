/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Collections;
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
     * URI pointed by JobResult MUST NOT be into workspace (else they will be erased after job execution)
     * @return The Job's results
     */
    default Set<JobResult> getResults() {
        return Collections.emptySet();
    }

    /**
     * @return Does the job has a result ?
     */
    default boolean hasResults() {
        return ((getResults() != null) && !getResults().isEmpty());
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @return does the job need a workspace ?
     */
    default boolean needWorkspace() {
        return false;
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @param pPath set workspace path
     */
    void setWorkspace(Path pPath);

    Path getWorkspace();

    /**
     * @param pJobInfoId save the jobInfo id inside the job
     */
    void setId(final UUID pJobInfoId);

    /**
     * Set the parameters and should check if all needed parameters are specified
     * @param pParameters set job parameters
     */
    void setParameters(Set<JobParameter> pParameters) throws JobParameterMissingException, JobParameterInvalidException;
}
