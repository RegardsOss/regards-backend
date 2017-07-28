package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event notifying job has succeeded
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class SucceededJobEvent extends AbstractJobEvent {

    public SucceededJobEvent() {
    }

    public SucceededJobEvent(UUID jobId) {
        super(jobId);
    }
}
