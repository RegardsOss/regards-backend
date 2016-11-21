/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

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
public class NewTenantEvent {

    /**
     * New tenant
     */
    private final TenantConnection tenant;

    /**
     * Microservice target of this message
     */
    private final String microserviceName;

    public NewTenantEvent(final TenantConnection pTenant, final String pMicroserviceName) {
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

}
