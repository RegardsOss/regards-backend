/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.access.services.domain.event.LinkUiPluginsDatasetsEvent;
import fr.cnes.regards.modules.catalog.services.domain.event.LinkPluginsDatasetsEvent;

/**
 * Module-common handler for AMQP events.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AccessServicesClientEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessServicesClientEventHandler.class);

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Constructor
     *
     * @param subscriber
     * @param runtimeTenantResolver
     */
    public AccessServicesClientEventHandler(ISubscriber subscriber, IRuntimeTenantResolver runtimeTenantResolver) {
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(LinkUiPluginsDatasetsEvent.class, new LinkUiPluginsDatasetsEventHandler());
        subscriber.subscribeTo(LinkPluginsDatasetsEvent.class, new LinkPluginsDatasetsEventHandler());
    }

    /**
     * Handle {@link LinkUiPluginsDatasetsEvent} event to refresh group cache
     *
     * @author Xavier-Alexandre Brochard
     */
    private class LinkUiPluginsDatasetsEventHandler implements IHandler<LinkUiPluginsDatasetsEvent> {

        @Override
        public void handle(TenantWrapper<LinkUiPluginsDatasetsEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                clearServicesAggregatedCache();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link LinkPluginsDatasetsEvent} event to refresh group cache
     *
     * @author Xavier-Alexandre Brochard
     */
    private class LinkPluginsDatasetsEventHandler implements IHandler<LinkPluginsDatasetsEvent> {

        @Override
        public void handle(TenantWrapper<LinkPluginsDatasetsEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                clearServicesAggregatedCache();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Empty the whole "servicesAggregated" cache. Maybe we can perform a finer eviction?
     */
    @CacheEvict(cacheNames = "servicesAggregated", allEntries = true)
    private void clearServicesAggregatedCache() {
        LOGGER.debug("Rejecting all entries of servicesAggregated cache");
    }

}
