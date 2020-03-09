/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.access.services.domain.event.LinkUiPluginsDatasetsEvent;
import fr.cnes.regards.modules.access.services.domain.event.UIPluginConfigurationEvent;
import fr.cnes.regards.modules.catalog.services.domain.event.LinkPluginsDatasetsEvent;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;

/**
 * Module-common handler for AMQP events.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("!test")
@Component
public class ServiceAggregatorClientEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IServiceAggregatorClient serviceAggregatorClient;

    /**
     * @param subscriber
     * @param runtimeTenantResolver
     * @param serviceAggregatorClient
     */
    public ServiceAggregatorClientEventHandler(ISubscriber subscriber, IRuntimeTenantResolver runtimeTenantResolver,
            IServiceAggregatorClient serviceAggregatorClient) {
        super();
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.serviceAggregatorClient = serviceAggregatorClient;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(LinkUiPluginsDatasetsEvent.class, new LinkUiPluginsDatasetsEventHandler());
        subscriber.subscribeTo(LinkPluginsDatasetsEvent.class, new LinkPluginsDatasetsEventHandler());
        subscriber.subscribeTo(UIPluginConfigurationEvent.class, new UIPluginConfigurationEventHandler());
        subscriber.subscribeTo(PluginConfEvent.class, new PluginConfEventHandler());
    }

    /**
     * Handle {@link PluginConfEvent} event to clear "servicesAggregated" cache
     *
     * @author Xavier-Alexandre Brochard
     */
    private class PluginConfEventHandler implements IHandler<PluginConfEvent> {

        @Override
        public void handle(TenantWrapper<PluginConfEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                if ((wrapper.getContent() != null)
                        && wrapper.getContent().getPluginTypes().contains(IService.class.getName())) {
                    serviceAggregatorClient.clearServicesAggregatedCache();
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link LinkUiPluginsDatasetsEvent} event to clear "servicesAggregated" cache
     *
     * @author Xavier-Alexandre Brochard
     */
    private class LinkUiPluginsDatasetsEventHandler implements IHandler<LinkUiPluginsDatasetsEvent> {

        @Override
        public void handle(TenantWrapper<LinkUiPluginsDatasetsEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                serviceAggregatorClient.clearServicesAggregatedCache();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link LinkPluginsDatasetsEvent} event to clear "servicesAggregated" cache
     *
     * @author Xavier-Alexandre Brochard
     */
    private class LinkPluginsDatasetsEventHandler implements IHandler<LinkPluginsDatasetsEvent> {

        @Override
        public void handle(TenantWrapper<LinkPluginsDatasetsEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                serviceAggregatorClient.clearServicesAggregatedCache();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link UIPluginConfigurationEvent} event to clear "servicesAggregated" cache.
     * We maybe could optimize and change cache content instead of clearing...
     *
     * @author Xavier-Alexandre Brochard
     */
    private class UIPluginConfigurationEventHandler implements IHandler<UIPluginConfigurationEvent> {

        @Override
        public void handle(TenantWrapper<UIPluginConfigurationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                serviceAggregatorClient.clearServicesAggregatedCache();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
