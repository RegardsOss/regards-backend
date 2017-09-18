package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event to notify job should be stopped. This event aims to change the job status, that's why it is not a JobEvent
 * @author oroussel
 * @author Sylvain Vissiere-Guerinet
 */
@Event(target = Target.MICROSERVICE)
public class StopJobEvent implements ISubscribable {

    protected UUID jobId;

    public StopJobEvent() {
    }

    public StopJobEvent(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
