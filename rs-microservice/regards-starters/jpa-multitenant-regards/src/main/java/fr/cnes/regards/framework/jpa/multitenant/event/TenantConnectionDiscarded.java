/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event that informs the microservice instance that connection is discarded. Each install will have to manage its own
 * event.
 *
 * @author Marc Sordi
 *
 */
@Event(target = Target.INSTANCE)
public class TenantConnectionDiscarded extends AbstractTenantEvent implements ISubscribable {

    public TenantConnectionDiscarded() {
        // JSON constructor
    }

    public TenantConnectionDiscarded(String tenant, String microserviceName) {
        super(tenant, microserviceName);
    }
}
