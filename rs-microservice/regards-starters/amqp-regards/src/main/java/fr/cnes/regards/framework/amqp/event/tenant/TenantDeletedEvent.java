/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event.tenant;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event published when a tenant is deleted
 *
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class TenantDeletedEvent extends AbstractTenantEvent implements ISubscribable {

    public TenantDeletedEvent(String pTenant) {
        super(pTenant);
    }

}
