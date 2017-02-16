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
    private final String tenant;

    public AbstractTenantEvent(String pTenant) {
        this.tenant = pTenant;
    }

    public String getTenant() {
        return tenant;
    }
}
