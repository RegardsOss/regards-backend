package fr.cnes.regards.framework.modules.session.commons.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event to delete a source
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class SourceDeleteEvent implements ISubscribable {

    /**
     * Source to be deleted
     */
    private String source;

    public SourceDeleteEvent(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
