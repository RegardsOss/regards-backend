/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;

/**
 * This class helps to configure virtual hosts at runtime listening to tenant events.
 *
 * @author Marc Sordi
 *
 */
public class AmqpEventHandler {

    /**
     * Used to configure tenant virtual hosts
     */
    private final IRabbitVirtualHostAdmin virtualHostAdmin;

    /**
     * Used to listen to tenant events
     */
    private final ISubscriber subscriber;

    public AmqpEventHandler(IRabbitVirtualHostAdmin pVirtualHostAdmin, ISubscriber pSubscriber) {
        this.virtualHostAdmin = pVirtualHostAdmin;
        this.subscriber = pSubscriber;
    }

    /**
     * Manage virtual hosts according to tenants
     */
    @PostConstruct
    public void init() {
        // Listen to tenant events
        subscriber.subscribeTo(TenantCreatedEvent.class, new TenantCreationHandler());
        subscriber.subscribeTo(TenantDeletedEvent.class, new TenantDeletionHandler());
    }

    /**
     * Handle tenant creation
     *
     * @author Marc Sordi
     *
     */
    private class TenantCreationHandler implements IHandler<TenantCreatedEvent> {

        @Override
        public void handle(TenantWrapper<TenantCreatedEvent> pWrapper) {
            TenantCreatedEvent tce = pWrapper.getContent();
            virtualHostAdmin.addVhost(tce.getTenant());
        }
    }

    /**
     * Handle tenant deletion
     *
     * @author Marc Sordi
     *
     */
    private class TenantDeletionHandler implements IHandler<TenantDeletedEvent> {

        @Override
        public void handle(TenantWrapper<TenantDeletedEvent> pWrapper) {
            TenantDeletedEvent tde = pWrapper.getContent();
            virtualHostAdmin.removeVhost(tde.getTenant());
        }
    }
}
