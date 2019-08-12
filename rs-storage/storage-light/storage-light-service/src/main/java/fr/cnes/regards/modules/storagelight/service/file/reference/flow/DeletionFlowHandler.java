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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * Handler to handle {@link DeletionFlowItem} AMQP messages.<br/>
 * Those messages are sent to delete a file reference for one owner.
 *
 * @author Sébastien Binda
 */
@Component
public class DeletionFlowHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<DeletionFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletionFlowHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    private static final int BULK_SIZE = 100;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileReferenceService fileRefService;

    private final Map<String, ConcurrentLinkedQueue<DeletionFlowItem>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DeletionFlowItem.class, this);
    }

    /**
     * Only add the message in the list of messages handled by bulk in the scheduled method
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    @Override
    public void handle(TenantWrapper<DeletionFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        DeletionFlowItem item = wrapper.getContent();
        if (!items.containsKey(tenant)) {
            items.put(tenant, new ConcurrentLinkedQueue<>());
        }
        items.get(tenant).add(item);
    }

    public void handleSync(TenantWrapper<DeletionFlowItem> wrapper) {
        DeletionFlowItem item = wrapper.getContent();
        fileRefService.delete(Lists.newArrayList(item));
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<DeletionFlowItem>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<DeletionFlowItem> tenantItems = entry.getValue();
                List<DeletionFlowItem> list = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        DeletionFlowItem doc = tenantItems.poll();
                        if (doc == null) {
                            if (list.isEmpty()) {
                                // nothing to do
                                return;
                            }
                            // Less than BULK_SIZE documents, bulk save what we have already
                            break;
                        } else { // enqueue document
                            list.add(doc);
                        }
                    }
                    LOGGER.info("Bulk saving {} DeleteFileRefFlowItem...", list.size());
                    long start = System.currentTimeMillis();
                    fileRefService.delete(list);
                    LOGGER.info("...{} DeleteFileRefFlowItem handled in {} ms", list.size(),
                                System.currentTimeMillis() - start);
                    list.clear();
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
