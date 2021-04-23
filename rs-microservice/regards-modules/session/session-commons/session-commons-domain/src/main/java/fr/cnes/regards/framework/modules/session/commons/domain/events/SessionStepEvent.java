package fr.cnes.regards.framework.modules.session.commons.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;

/**
 * Event to update Session
 *
 * @author Iliana Ghazali
 **/

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class SessionStepEvent implements ISubscribable {

    /**
     * Session step to be stored
     */
    private SessionStep sessionStep;

    public SessionStepEvent(SessionStep sessionStep) {
        this.sessionStep = sessionStep;
    }

    public SessionStepEvent() {
    }
}
