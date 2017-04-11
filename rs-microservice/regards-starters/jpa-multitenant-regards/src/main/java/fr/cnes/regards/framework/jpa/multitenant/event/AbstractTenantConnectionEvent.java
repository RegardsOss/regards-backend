/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 *
 * Common {@link TenantConnection} event structure
 *
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractTenantConnectionEvent {

    /**
     * New tenant
     */
    private TenantConnection tenant;

    /**
     * Microservice target of this message
     */
    private String microserviceName;

    public AbstractTenantConnectionEvent() {
        super();
    }

    public AbstractTenantConnectionEvent(final TenantConnection pTenant, final String pMicroserviceName) {
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
