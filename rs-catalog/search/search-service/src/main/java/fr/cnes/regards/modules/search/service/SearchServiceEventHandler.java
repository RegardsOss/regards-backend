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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserEvent;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Component
public class SearchServiceEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final IAccessGroupCache accessGroupCache;
    private final ISubscriber subscriber;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public SearchServiceEventHandler(IAccessGroupCache accessGroupClientService, ISubscriber subscriber, IRuntimeTenantResolver runtimeTenantResolver) {
        this.accessGroupCache = accessGroupClientService;
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ProjectUserEvent.class, new ProjectUserEventHandler());
    }

    private class ProjectUserEventHandler implements IHandler<ProjectUserEvent> {

        @Override
        public void handle(TenantWrapper<ProjectUserEvent> wrapper) {
            if (Arrays.asList(ProjectUserAction.UPDATE, ProjectUserAction.DELETE).contains(wrapper.getContent().getAction())) {
                try {
                    runtimeTenantResolver.forceTenant(wrapper.getTenant());
                    accessGroupCache.cleanAccessGroups(wrapper.getContent().getEmail(), wrapper.getTenant());
                } finally {
                    runtimeTenantResolver.clearTenant();
                }
            }
        }

    }

}
