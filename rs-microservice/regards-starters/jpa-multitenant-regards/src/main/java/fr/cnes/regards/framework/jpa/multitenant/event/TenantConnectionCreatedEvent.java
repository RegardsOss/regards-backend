/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Class NewTenantEvent
 *
 * AMQP Message to broadcast information that a new tenant is available
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Event(target = Target.ALL)
public class TenantConnectionCreatedEvent implements ISubscribable {

    /**
     * New tenant
     */
    private TenantConnection tenant;

    /**
     * Microservice target of this message
     */
    private String microserviceName;

    public TenantConnectionCreatedEvent() {
        super();
    }

    public TenantConnectionCreatedEvent(final TenantConnection pTenant, final String pMicroserviceName) {
        super();
        tenant = pTenant;
        microserviceName = pMicroserviceName;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public TenantConnection getTenant() {
        return tenant;
    }

    public void setMicroserviceName(final String pMicroserviceName) {
        microserviceName = pMicroserviceName;
    }

    public void setTenant(final TenantConnection pTenant) {
        tenant = pTenant;
    }

}
