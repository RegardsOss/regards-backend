/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import fr.cnes.regards.framework.amqp.event.EventProperties;
import fr.cnes.regards.framework.amqp.event.ISubscribableEvent;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Marc Sordi
 *
 */
@EventProperties(target = Target.ALL)
public class PublishAllEvent extends AbstractEntityEvent implements ISubscribableEvent {

}
