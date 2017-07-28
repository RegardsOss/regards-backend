package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.UUID;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * AMQP event to notify job should be stopped
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class StopJobEvent extends AbstractJobEvent implements ISubscribable {

    public StopJobEvent() {
    }

    public StopJobEvent(UUID jobId) {
        super(jobId);
    }

}
