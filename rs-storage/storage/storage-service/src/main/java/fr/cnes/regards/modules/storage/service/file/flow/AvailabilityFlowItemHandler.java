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
package fr.cnes.regards.modules.storage.service.file.flow;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;

/**
 * Handler of bus message events {@link AvailabilityFlowItem}s.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class AvailabilityFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<AvailabilityFlowItem> {

    @Value("${regards.storage.availability.items.bulk.size:10}")
    private static final int BULK_SIZE = 1000;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(AvailabilityFlowItem.class, this);
    }

    @Override
    public void handleBatch(String tenant, List<AvailabilityFlowItem> messages) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.debug("[AVAILABILITY REQUESTS HANDLER] Bulk saving {} AvailabilityFlowItem...", messages.size());
            long start = System.currentTimeMillis();
            fileCacheReqService.makeAvailable(messages);
            LOGGER.debug("[AVAILABILITY REQUESTS HANDLER] {} AvailabilityFlowItem handled in {} ms", messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public boolean validate(String tenant, AvailabilityFlowItem message) {
        return true;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
