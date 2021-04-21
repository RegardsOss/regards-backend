package fr.cnes.regards.framework.modules.jobs.domain.event;

/**
 * Type of event that occured on a job.
 * @author Sylvain VISSIERE-GUERINET
 */
public enum JobEventType {

    ABORTED,
    FAILED,
    RUNNING,
    SUCCEEDED

}
