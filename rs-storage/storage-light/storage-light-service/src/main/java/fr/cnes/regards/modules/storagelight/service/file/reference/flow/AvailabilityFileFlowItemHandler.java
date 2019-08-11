/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * @author sbinda
 *
 */
@Component
public class AvailabilityFileFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<AvailabilityFileRefFlowItem> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(AvailabilityFileRefFlowItem.class, this);
    }

    @Override
    public void handle(TenantWrapper<AvailabilityFileRefFlowItem> wrapper) {
        runtimeTenantResolver.forceTenant(wrapper.getTenant());
        try {
            AvailabilityFileRefFlowItem item = wrapper.getContent();
            fileRefService.makeAvailable(item.getChecksums(), item.getExpirationDate(), item.getRequestId());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

}
