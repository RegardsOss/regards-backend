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
 *
 * Event that informs a tenant connection configuration is updated and has to be handle by JPA multitenant starter to
 * update its connection.<br/>
 * This event must only be handled by the starter in each microservice instance. When the connection is ready to used,
 * the starter sends to all microservice instances of the current type a {@link TenantConnectionReady}.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Event(target = Target.ALL)
public class TenantConnectionConfigurationUpdated extends AbstractTenantConnectionEvent implements ISubscribable {

    public TenantConnectionConfigurationUpdated() {
        super();
    }

    public TenantConnectionConfigurationUpdated(final TenantConnection pTenant, final String pMicroserviceName) {
        super(pTenant, pMicroserviceName);
    }
}
