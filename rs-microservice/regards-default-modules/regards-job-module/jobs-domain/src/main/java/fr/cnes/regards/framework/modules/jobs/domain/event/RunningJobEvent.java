package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event notifying jab is running
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class RunningJobEvent extends AbstractJobEvent {

    public RunningJobEvent() {
    }

    public RunningJobEvent(UUID jobId) {
        super(jobId);
    }
}
