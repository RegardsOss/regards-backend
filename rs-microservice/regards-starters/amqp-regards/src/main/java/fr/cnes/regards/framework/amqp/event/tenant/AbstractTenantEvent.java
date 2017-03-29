/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event.tenant;

/**
 * Represent the common information for a tenant event
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractTenantEvent {

    /**
     * Event tenant
     */
    private String tenant;

    public AbstractTenantEvent() {
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        this.tenant = pTenant;
    }
}
