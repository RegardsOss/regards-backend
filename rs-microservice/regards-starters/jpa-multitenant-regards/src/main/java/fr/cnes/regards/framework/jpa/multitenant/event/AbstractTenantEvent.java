/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event;

/**
 *
 *
 * Common tenant event structure
 *
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractTenantEvent {

    /**
     * New tenant
     */
    private String tenant;

    /**
     * Microservice target of this message
     */
    private String microserviceName;

    public AbstractTenantEvent() {
        super();
    }

    public AbstractTenantEvent(final String pTenant, final String pMicroserviceName) {
        super();
        setTenant(pTenant);
        microserviceName = pMicroserviceName;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public void setMicroserviceName(final String pMicroserviceName) {
        microserviceName = pMicroserviceName;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

}
