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
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;

/**
 * Handler to handle {@link ReferenceFlowItem} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class StorageFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StorageFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFlowItemHandler.class);

    /**
     * Bulk size limit to handle messages
     * NOTE : Over 100 performance are decreased
     */
    @Value("${regards.storage.store.items.bulk.size:10}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileStorageRequestService fileStorageReqService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StorageFlowItem.class, this);
    }

    @Override
    public void handleBatch(String tenant, List<StorageFlowItem> messages) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.debug("[STORAGE FLOW HANDLER] Bulk saving {} StorageFlowItem...", messages.size());
            long start = System.currentTimeMillis();
            fileStorageReqService.store(messages);
            LOGGER.info("[STORAGE FLOW HANDLER] {} StorageFlowItem handled in {} ms", messages.size(),
                        System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public boolean validate(String tenant, StorageFlowItem message) {
        return true;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }
}
