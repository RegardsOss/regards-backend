package fr.cnes.regards.framework.modules.session.commons.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event to delete a session
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class SessionDeleteEvent implements ISubscribable {

    /**
     * Source including the session to delete
     */
    private String source;

    /**
     * Session to be deleted
     */
    private String session;

    public SessionDeleteEvent(String source, String session) {
        this.source = source;
        this.session = session;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
