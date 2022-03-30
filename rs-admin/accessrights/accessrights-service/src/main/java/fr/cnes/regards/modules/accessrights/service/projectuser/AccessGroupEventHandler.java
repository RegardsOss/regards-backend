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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupAction;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.PublicAccessGroupEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AccessGroupEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;

    private final ProjectUserGroupService projectUserGroupService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessGroupEventHandler(ISubscriber subscriber,
                                   ProjectUserGroupService projectUserGroupService,
                                   IRuntimeTenantResolver runtimeTenantResolver) {
        this.subscriber = subscriber;
        this.projectUserGroupService = projectUserGroupService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PublicAccessGroupEvent.class, new PublicAccessGroupEventHandler());
        subscriber.subscribeTo(AccessGroupEvent.class, new AccessGroupEventDeletionHandler());
    }

    private class PublicAccessGroupEventHandler implements IHandler<PublicAccessGroupEvent> {

        @Override
        public void handle(TenantWrapper<PublicAccessGroupEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                PublicAccessGroupEvent content = wrapper.getContent();
                if (content.getAction() == AccessGroupAction.CREATE) {
                    projectUserGroupService.addPublicGroup(content.getAccessGroup().getName());
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

    private class AccessGroupEventDeletionHandler implements IHandler<AccessGroupEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                AccessGroupEvent content = wrapper.getContent();
                if (content.getAction() == AccessGroupAction.DELETE) {
                    projectUserGroupService.removeGroup(content.getAccessGroup().getName());
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

}
