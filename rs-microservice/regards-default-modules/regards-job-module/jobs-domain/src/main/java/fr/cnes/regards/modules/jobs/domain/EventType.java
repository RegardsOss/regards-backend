/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 * Event types between a running job and JobHandler
 */
public enum EventType {
    JOB_PERCENT_COMPLETED, SUCCEEDED, RUN_ERROR;

    @Override
    public String toString() {
        return this.name();
    }
}
