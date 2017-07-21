package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event notifying job has been aborted
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class AbortedJobEvent extends AbstractJobEvent {

    public AbortedJobEvent() {
    }

    public AbortedJobEvent(UUID jobId) {
        super(jobId);
    }
}
