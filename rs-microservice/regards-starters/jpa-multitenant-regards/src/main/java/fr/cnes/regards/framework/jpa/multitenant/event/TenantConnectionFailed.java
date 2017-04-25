/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event that informs a tenant connection fail and has to be disabled by JPA multitenant starter.<br/>
 * This event must only be handled by the starter in each microservice instance.
 *
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class TenantConnectionFailed extends AbstractTenantEvent implements ISubscribable {

    public TenantConnectionFailed() {
        // JSON constructor
    }

    public TenantConnectionFailed(String tenant, String microserviceName) {
        super(tenant, microserviceName);
    }
}
