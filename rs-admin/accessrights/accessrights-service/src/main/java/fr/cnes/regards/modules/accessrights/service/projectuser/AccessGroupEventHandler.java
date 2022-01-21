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
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupDeletionEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.PublicAccessGroupCreationEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.PublicAccessGroupDeletionEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AccessGroupEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;
    private final ProjectUserGroupService projectUserGroupService;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessGroupEventHandler(ISubscriber subscriber, ProjectUserGroupService projectUserGroupService, IRuntimeTenantResolver runtimeTenantResolver) {
        this.subscriber = subscriber;
        this.projectUserGroupService = projectUserGroupService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PublicAccessGroupCreationEvent.class, new PublicAccessGroupCreationEventHandler());
        subscriber.subscribeTo(PublicAccessGroupDeletionEvent.class, new PublicAccessGroupDeletionEventHandler());
        subscriber.subscribeTo(AccessGroupDeletionEvent.class, new AccessGroupDeletionEventHandler());
    }

    private class PublicAccessGroupCreationEventHandler implements IHandler<PublicAccessGroupCreationEvent> {

        @Override
        public void handle(TenantWrapper<PublicAccessGroupCreationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                projectUserGroupService.addPublicGroup(wrapper.getContent().getAccessGroup().getName());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

    private class PublicAccessGroupDeletionEventHandler implements IHandler<PublicAccessGroupDeletionEvent> {

        @Override
        public void handle(TenantWrapper<PublicAccessGroupDeletionEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                projectUserGroupService.removePublicGroup(wrapper.getContent().getAccessGroup().getName());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

    private class AccessGroupDeletionEventHandler implements IHandler<AccessGroupDeletionEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupDeletionEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                projectUserGroupService.removeGroup(wrapper.getContent().getAccessGroup().getName());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

}
