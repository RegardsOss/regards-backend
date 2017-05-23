/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.domain.event;

import java.util.Set;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author oroussel
 */
@Event(target = Target.ALL)
public class BroadcastPluginConfEvent extends AbstractPluginConfEvent implements ISubscribable {

    public BroadcastPluginConfEvent(Long pPluginConfId, PluginServiceAction pAction, Set<String> pPluginTypes) {
        super(pPluginConfId, pAction, pPluginTypes);
    }

    public BroadcastPluginConfEvent() {
    }
}
