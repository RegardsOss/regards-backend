/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class PublishAllEvent extends AbstractEntityEvent implements ISubscribable {

}
