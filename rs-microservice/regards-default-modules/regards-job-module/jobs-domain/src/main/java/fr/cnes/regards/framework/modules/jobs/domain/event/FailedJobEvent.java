package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event notifying job has failed miserabily...
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class FailedJobEvent extends AbstractJobEvent {

    public FailedJobEvent() {
    }

    public FailedJobEvent(UUID jobId) {
        super(jobId);
    }
}
