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
package fr.cnes.regards.modules.storage.service.file.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;

/**
 * Handler of bus message events {@link AvailabilityFlowItem}s.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class AvailabilityFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<AvailabilityFlowItem> {

    @Value("${regards.storage.availability.items.bulk.size:100}")
    private static final int BULK_SIZE = 100;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private RequestsGroupService reqGroupService;

    private final Map<String, ConcurrentLinkedQueue<AvailabilityFlowItem>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(AvailabilityFlowItem.class, this);
    }

    /**
     * Only add the message in the list of messages handled by bulk in the scheduled method
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    @Override
    public void handle(TenantWrapper<AvailabilityFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        AvailabilityFlowItem item = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        if (item.getChecksums().size() > AvailabilityFlowItem.MAX_REQUEST_PER_GROUP) {
            String message = String
                    .format("Number of availability requests (%d) for group %s exeeds maximum limit of %d",
                            item.getChecksums().size(), item.getGroupId(), AvailabilityFlowItem.MAX_REQUEST_PER_GROUP);
            reqGroupService.denied(item.getGroupId(), FileRequestType.AVAILABILITY, message);
        } else {
            if (!items.containsKey(tenant)) {
                items.put(tenant, new ConcurrentLinkedQueue<>());
            }
            items.get(tenant).add(item);
        }
    }

    public void handleSync(TenantWrapper<AvailabilityFlowItem> wrapper) {
        runtimeTenantResolver.forceTenant(wrapper.getTenant());
        try {
            AvailabilityFlowItem item = wrapper.getContent();
            reqGroupService.granted(item.getGroupId(), FileRequestType.REFERENCE, item.getChecksums().size());
            fileCacheReqService.makeAvailable(item.getChecksums(), item.getExpirationDate(), item.getGroupId());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<AvailabilityFlowItem>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<AvailabilityFlowItem> tenantItems = entry.getValue();
                List<AvailabilityFlowItem> list = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        AvailabilityFlowItem doc = tenantItems.poll();
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
                    if (!list.isEmpty()) {
                        LOGGER.info("[AVAILABILITY REQUESTS HANDLER] Bulk saving {} AvailabilityFlowItem...",
                                    list.size());
                        long start = System.currentTimeMillis();
                        fileCacheReqService.makeAvailable(list);
                        LOGGER.info("[AVAILABILITY REQUESTS HANDLER] {} AvailabilityFlowItem handled in {} ms",
                                    list.size(), System.currentTimeMillis() - start);
                        list.clear();
                    }
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
