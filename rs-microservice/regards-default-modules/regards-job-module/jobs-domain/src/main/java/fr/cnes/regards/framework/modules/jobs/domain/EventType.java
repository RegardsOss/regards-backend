/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * Event types between a running job and JobHandler
 * 
 * @author Léo Mieulet
 */
public enum EventType {
    /**
     * Used to update the job progress
     */
    JOB_PERCENT_COMPLETED,
    /**
     * Used to propagate the job success
     */
    SUCCEEDED,
    /**
     * Used to propagate a job error
     */
    RUN_ERROR;

    @Override
    public String toString() {
        return this.name();
    }
}
