/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.microservices.administration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;

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

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        tenantResolver.cleanActiveTenantCache();
    }

    @EventListener
    public void processEvent(TenantConnectionDiscarded event) {
        tenantResolver.cleanActiveTenantCache();
    }
}
