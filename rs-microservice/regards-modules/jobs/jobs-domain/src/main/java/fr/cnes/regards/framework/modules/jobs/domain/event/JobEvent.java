package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * This event is used to propagate information from a job to the rest of the world. It is ideal to monitor what's happening to a job.
 *
 * @author oroussel
 * @author svissier
 */
@Event(target = Target.MICROSERVICE)
public class JobEvent implements ISubscribable {

    /**
     * the job id
     */
    protected UUID jobId;

    /**
     * the job event type
     */
    protected JobEventType jobEventType;

    /**
     * Default constructor
     */
    public JobEvent() {
    }

    /**
     * Constructor setting the job id and job event type
     * @param jobId
     * @param jobEventType
     */
    public JobEvent(UUID jobId, JobEventType jobEventType) {
        this.jobId = jobId;
        this.jobEventType = jobEventType;
    }

    /**
     * @return the job id
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * Set the job id
     * @param jobId
     */
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the job event type
     */
    public JobEventType getJobEventType() {
        return jobEventType;
    }

    /**
     * Set the job event type
     * @param jobEventType
     */
    public void setJobEventType(JobEventType jobEventType) {
        this.jobEventType = jobEventType;
    }
}
