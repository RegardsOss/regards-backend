package fr.cnes.regards.modules.indexer.dao.event;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import org.springframework.context.ApplicationEvent;

public class IndexCreatedEvent extends ApplicationEvent {

    private final String name;

    public IndexCreatedEvent(Object source, String name) {
        super(source);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
