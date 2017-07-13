/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * JobInfo status
 * 
 * @author Léo Mieulet
 */
public enum JobStatus {
    /**
     * Job created
     */
    PENDING,
    /**
     * Job waiting to be taken into account by job service pool
     */
    QUEUED,
    /**
     * Job running
     */
    RUNNING,
    /**
     * Job finished without error
     */
    SUCCEEDED,
    /**
     * Job finished with error(s)
     */
    FAILED,
    /**
     * Job cancelled
     */
    ABORTED,
    /**
     * Unused state for job temporary suspended because requiring additional resources
     */
    SUSPENDED;

    @Override
    public String toString() {
        return this.name();
    }
}
