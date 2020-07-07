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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;

/**
 * Handler to handle {@link DeletionFlowItem} AMQP messages.<br>
 * Those messages are sent to delete a file reference for one owner.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class DeletionFlowHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DeletionFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletionFlowHandler.class);

    @Value("${regards.storage.deletion.items.bulk.size:10}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileDeletionRequestService fileDelReqService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DeletionFlowItem.class, this);
    }

    @Override
    public void handleBatch(String tenant, List<DeletionFlowItem> messages) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.debug("[DELETION FLOW HANDLER] Bulk saving {} DeleteFileRefFlowItem...", messages.size());
            long start = System.currentTimeMillis();
            fileDelReqService.handle(messages);
            LOGGER.debug("[DELETION FLOW HANDLER] {} DeleteFileRefFlowItem handled in {} ms", messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public boolean validate(String tenant, DeletionFlowItem message) {
        return true;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
