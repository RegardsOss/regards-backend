/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * @author Léo Mieulet
 */
public abstract class AbstractJob implements IJob {

    /**
     * JobInfo id
     */
    private UUID id;

    /**
     * Job parameters
     */
    protected Set<JobParameter> parameters;

    /**
     * The workspace can be null, it should be cleaned after termination of a job
     */
    private Path workspace;

    /**
     * When the JobHandler creates this job, it saves the jobId
     */
    @Override
    public void setId(final UUID pJobInfoId) {
        id = pJobInfoId;
    }

    /**
     * @return the parameters
     */
    public Set<JobParameter> getParameters() {
        return parameters;
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    @Override
    public Path getWorkspace() {
        return workspace;
    }
}
