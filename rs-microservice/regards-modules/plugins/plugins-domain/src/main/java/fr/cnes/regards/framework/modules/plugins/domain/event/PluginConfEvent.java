package fr.cnes.regards.framework.modules.plugins.domain.event;

import java.util.Set;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Target.ONE_PER_MICROSERVICE_TYPE because only one ingesterService should manage a PluginConf change (Database
 * updated).
 * BROADCAST because UNICAST/ONE_PER_MICROSERVICE_TYPE doesn't exist...
 * @author oroussel
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class PluginConfEvent extends AbstractPluginConfEvent implements ISubscribable {

    public PluginConfEvent(Long pPluginConfId, PluginServiceAction pAction, Set<String> pPluginTypes) {
        super(pPluginConfId, pAction, pPluginTypes);
    }

    public PluginConfEvent() {
    }
}
