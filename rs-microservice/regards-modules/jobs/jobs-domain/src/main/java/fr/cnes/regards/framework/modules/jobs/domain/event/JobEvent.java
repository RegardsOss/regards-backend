package fr.cnes.regards.framework.modules.jobs.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

import java.util.Objects;
import java.util.UUID;

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
    private UUID jobId;

    /**
     * the job event type
     */
    private JobEventType jobEventType;

    /**
     * Class name of the job, which sent the event (it corresponds to JobInfo#getClassName, basically it is just the
     * value returned by java.lang.Class#getName())
     */
    private String jobClassName;

    public JobEvent() {
        // default constructor for serialization/deserialization
    }

    /**
     * Constructor setting the job id and job event type
     */
    public JobEvent(UUID id, JobEventType jobEventType, String jobClassName) {
        this.jobId = id;
        this.jobEventType = jobEventType;
        this.jobClassName = jobClassName;
    }

    /**
     * @return the job id
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * @return the job event type
     */
    public JobEventType getJobEventType() {
        return jobEventType;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobEvent jobEvent = (JobEvent) o;
        return Objects.equals(jobId, jobEvent.jobId) && jobEventType == jobEvent.jobEventType && Objects.equals(
            jobClassName,
            jobEvent.jobClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, jobEventType, jobClassName);
    }

    @Override
    public String toString() {
        return "JobEvent{"
               + "jobId="
               + jobId
               + ", jobEventType="
               + jobEventType
               + ", jobClassName='"
               + jobClassName
               + '\''
               + '}';
    }
}
