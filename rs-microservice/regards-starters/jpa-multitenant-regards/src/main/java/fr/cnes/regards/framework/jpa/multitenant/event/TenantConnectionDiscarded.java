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
public class TenantConnectionDiscarded implements ISubscribable {

    /**
     * New tenant
     */
    private String tenant;

    /**
     * Microservice target of this message
     */
    private String microserviceName;

    public TenantConnectionDiscarded() {
        // JSON constructor
    }

    public TenantConnectionDiscarded(String tenant, String microserviceName) {
        this.tenant = tenant;
        this.microserviceName = microserviceName;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public void setMicroserviceName(String pMicroserviceName) {
        microserviceName = pMicroserviceName;
    }
}
