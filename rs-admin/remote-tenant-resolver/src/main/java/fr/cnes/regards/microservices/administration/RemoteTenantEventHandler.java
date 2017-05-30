/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;

/**
 * Manage tenant event
 * @author Marc Sordi
 *
 */
public class RemoteTenantEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteTenantEventHandler.class);

    /**
     * Tenant subscriber
     */
    private final IInstanceSubscriber subscriber;

    /**
     * Tenant resolver
     */
    private final RemoteTenantResolver tenantResolver;

    public RemoteTenantEventHandler(IInstanceSubscriber subscriber, RemoteTenantResolver tenantResolver) {
        this.subscriber = subscriber;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Listen to tenant creation
        subscriber.subscribeTo(TenantCreatedEvent.class, new TenantCreatedEventHandler());
        // Listen to tenant deletion
        subscriber.subscribeTo(TenantDeletedEvent.class, new TenantDeletedEventHandler());
        // Listen to tenant connection ready event
        subscriber.subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyHandler());
        // Listen to tenant connection discard event
        subscriber.subscribeTo(TenantConnectionDiscarded.class, new TenantConnectionDiscardedHandler());
    }

    private class TenantCreatedEventHandler implements IHandler<TenantCreatedEvent> {

        @Override
        public void handle(TenantWrapper<TenantCreatedEvent> pWrapper) {
            tenantResolver.cleanTenantCache();
        }
    }

    private class TenantDeletedEventHandler implements IHandler<TenantDeletedEvent> {

        @Override
        public void handle(TenantWrapper<TenantDeletedEvent> pWrapper) {
            tenantResolver.cleanTenantCache();
        }
    }

    private class TenantConnectionReadyHandler implements IHandler<TenantConnectionReady> {

        @Override
        public void handle(TenantWrapper<TenantConnectionReady> pWrapper) {
            tenantResolver.cleanActiveTenantCache();
        }
    }

    private class TenantConnectionDiscardedHandler implements IHandler<TenantConnectionDiscarded> {

        @Override
        public void handle(TenantWrapper<TenantConnectionDiscarded> pWrapper) {
            tenantResolver.cleanActiveTenantCache();
        }
    }
}
