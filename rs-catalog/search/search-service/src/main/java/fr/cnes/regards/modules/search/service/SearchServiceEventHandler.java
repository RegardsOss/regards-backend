/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupAssociationEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupDissociationEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupPublicEvent;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;

/**
 * @author Marc Sordi
 *
 */
@Component
public class SearchServiceEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final IAccessGroupCache accessGroupCache;

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public SearchServiceEventHandler(IAccessGroupCache accessGroupClientService, ISubscriber subscriber,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.accessGroupCache = accessGroupClientService;
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AccessGroupAssociationEvent.class, new AccessGroupAssociationEventHandler());
        subscriber.subscribeTo(AccessGroupDissociationEvent.class, new AccessGroupDissociationEventHandler());
        subscriber.subscribeTo(AccessGroupPublicEvent.class, new AccessGroupPublicEventHandler());
    }

    /**
     * Handle {@link AccessGroupAssociationEvent} event to refresh group cache
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupAssociationEventHandler implements IHandler<AccessGroupAssociationEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupAssociationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanAccessGroups(wrapper.getContent().getUserEmail(), wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AccessGroupDissociationEvent} event to refresh group cache
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupDissociationEventHandler implements IHandler<AccessGroupDissociationEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupDissociationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanAccessGroups(wrapper.getContent().getUserEmail(), wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AccessGroupPublicEvent} event to clean all cache entries
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupPublicEventHandler implements IHandler<AccessGroupPublicEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupPublicEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanPublicAccessGroups(wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
