package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * @author oroussel
 */
public class AbstractJobEvent implements ISubscribable {

    protected UUID jobId;

    public AbstractJobEvent() {
    }

    public AbstractJobEvent(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
