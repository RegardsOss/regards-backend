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
     * Store the tenantName
     */
    private String tenantName;

    /**
     * Job parameters
     */
    protected Set<JobParameter> parameters;

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

    public Path getWorkspace() {
        return workspace;
    }
}
