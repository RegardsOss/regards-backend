/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 *
 * Class UpdateAuthoritiesEvent
 *
 * AMQP Event to inform that authorities has been changed by administration service.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Event(target = Target.ALL)
public class UpdateAuthoritiesEvent implements ISubscribable {
}
