/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 * JobInfo status
 * 
 * @author Léo Mieulet
 */
public enum JobStatus {

    /**
     * Job waiting to get some resources allocated
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
