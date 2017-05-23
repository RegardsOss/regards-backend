package fr.cnes.regards.framework.modules.plugins.domain.event;

import java.util.Set;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author oroussel
 */
@Event(target = Target.MICROSERVICE)
public class PluginConfEvent extends AbstractPluginConfEvent implements IPollable {

    public PluginConfEvent(Long pPluginConfId, PluginServiceAction pAction, Set<String> pPluginTypes) {
        super(pPluginConfId, pAction, pPluginTypes);
    }

    public PluginConfEvent() {
    }
}
