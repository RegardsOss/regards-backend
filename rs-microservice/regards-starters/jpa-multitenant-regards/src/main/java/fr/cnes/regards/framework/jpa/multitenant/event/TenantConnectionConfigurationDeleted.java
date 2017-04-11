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
 * Event that informs a tenant connection is delete and has to be remove from JPA management.<br/>
 * This event must only be handled by the starter in each microservice instance. When the connection is deleted, the
 * starter sends to all microservice instances of the current type a {@link TenantConnectionDiscarded}.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Event(target = Target.ALL)
public class TenantConnectionConfigurationDeleted extends AbstractTenantConnectionEvent implements ISubscribable {

    public TenantConnectionConfigurationDeleted() {
        super();
    }

    public TenantConnectionConfigurationDeleted(final TenantConnection pTenant, final String pMicroserviceName) {
        super(pTenant, pMicroserviceName);
    }
}
