package fr.cnes.regards.modules.entities.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;

/**
 * AbstractEntityEvent specialization for Dataset
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class DatasetEvent extends AbstractEntityEvent {
    private DatasetEvent() {
        super();
    }

    public DatasetEvent(UniformResourceName... pIpIds) {
        super(pIpIds);
    }
}
