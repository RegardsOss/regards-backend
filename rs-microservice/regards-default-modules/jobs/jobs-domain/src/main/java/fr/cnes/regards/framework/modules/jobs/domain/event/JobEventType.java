package fr.cnes.regards.framework.modules.jobs.domain.event;

/**
 *
 * Type of event, that occured on a job, we are propagating into the system thanks to AMQP.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum JobEventType {

    ABORTED, FAILED, RUNNING, SUCCEEDED

}
