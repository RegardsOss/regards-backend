package fr.cnes.regards.framework.modules.plugins.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event raised when the plugin cache must be reloaded on specific tenant
 *
 * @author Léo Mieulet
 */
@Event(target = Target.MICROSERVICE)
public class PluginConfClearCacheEvent implements ISubscribable {

}
