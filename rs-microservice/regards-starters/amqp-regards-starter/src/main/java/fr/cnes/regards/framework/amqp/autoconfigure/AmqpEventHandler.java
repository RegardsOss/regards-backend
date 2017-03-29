/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;

/**
 * This class helps to configure virtual hosts at runtime listening to tenant events. The system uses the AMQP manager
 * virtual host no to be tenant dependent.
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
    private final IInstanceSubscriber instanceSubscriber;

    public AmqpEventHandler(IRabbitVirtualHostAdmin pVirtualHostAdmin, IInstanceSubscriber pInstanceSubscriber) {
        this.virtualHostAdmin = pVirtualHostAdmin;
        this.instanceSubscriber = pInstanceSubscriber;
    }

    /**
     * Manage virtual hosts according to tenants
     */
    @PostConstruct
    public void init() {
        // Listen to tenant events
        instanceSubscriber.subscribeTo(TenantCreatedEvent.class, new TenantCreationHandler());
        instanceSubscriber.subscribeTo(TenantDeletedEvent.class, new TenantDeletionHandler());
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
