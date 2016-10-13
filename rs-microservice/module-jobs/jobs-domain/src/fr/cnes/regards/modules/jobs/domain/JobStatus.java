/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

public enum JobStatus {

    QUEUD, RUNNING, SUCCEEDED, FAILED, PENDING, ABORTED, SUSPENDED, ARCHIVED;

    @Override
    public String toString() {
        return this.name();
    }
}
