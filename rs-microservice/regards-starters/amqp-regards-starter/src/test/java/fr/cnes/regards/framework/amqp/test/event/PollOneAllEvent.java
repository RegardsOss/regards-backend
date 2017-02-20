/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class PollOneAllEvent extends AbstractEntityEvent implements IPollable {

}
