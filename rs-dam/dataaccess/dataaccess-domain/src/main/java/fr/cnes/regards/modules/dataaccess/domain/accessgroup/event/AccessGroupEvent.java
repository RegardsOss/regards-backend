/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.dataaccess.domain.accessgroup.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;

/**
 * {@link AccessGroup} event common information
 *
 * @author Xavier-Alexandre Brochard
 */
@Event(target = Target.ALL)
public class AccessGroupEvent implements ISubscribable {

    /**
     * The source of the event
     */
    private AccessGroup accessGroup;

    public AccessGroupEvent() {
        // Deserialization constructor
    }

    public AccessGroupEvent(AccessGroup pAccessGroup) {
        super();
        accessGroup = pAccessGroup;
    }

    public AccessGroup getAccessGroup() {
        return accessGroup;
    }

    public void setAccessGroup(AccessGroup pAccessGroup) {
        accessGroup = pAccessGroup;
    }
}
