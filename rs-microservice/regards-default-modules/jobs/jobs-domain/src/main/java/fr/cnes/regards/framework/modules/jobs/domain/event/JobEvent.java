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

    protected UUID jobId;

    protected JobEventType jobEventType;

    public JobEvent() {
    }

    public JobEvent(UUID jobId, JobEventType jobEventType) {
        this.jobId = jobId;
        this.jobEventType = jobEventType;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public JobEventType getJobEventType() {
        return jobEventType;
    }

    public void setJobEventType(JobEventType jobEventType) {
        this.jobEventType = jobEventType;
    }
}
