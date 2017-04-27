/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 *
 * This event must be sent when a new tenant is created and after default role creation to register microservice
 * resources.
 *
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class ResourceAccessInit implements ISubscribable {

}
