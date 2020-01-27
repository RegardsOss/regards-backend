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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import fr.cnes.regards.modules.storage.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;

/**
 * Handler to handle {@link CopyFlowItem} AMQP messages.<br>
 * Those messages are sent to copy a file reference to a given storage location<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class CopyFlowHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<CopyFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFlowHandler.class);

    @Value("${regards.storage.copy.items.bulk.size:100}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileCopyRequestService fileCopyReqService;

    @Autowired
    private RequestsGroupService reqGroupService;

    private final Map<String, ConcurrentLinkedQueue<CopyFlowItem>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(CopyFlowItem.class, this);
    }

    /**
     * Only add the message in the list of messages handled by bulk in the scheduled method
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    @Override
    public void handle(TenantWrapper<CopyFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        CopyFlowItem item = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        while ((items.get(tenant) != null) && (items.get(tenant).size() >= (100 * BULK_SIZE))) {
            // Do not overload the concurrent queue if the configured listener does not handle queued message faster
            try {
                LOGGER.warn("Slow process detected. Waiting 30s for getting new message from amqp queue.");
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                LOGGER.error(String
                        .format("Error waiting for requests handled by microservice. Current requests pool to handle = %s",
                                items.size()),
                             e);
            }
        }
        if (item.getFiles().size() > CopyFlowItem.MAX_REQUEST_PER_GROUP) {
            String message = String.format("Number of copy requests (%d) for group %s exeeds maximum limit of %d",
                                           item.getFiles().size(), item.getGroupId(),
                                           CopyFlowItem.MAX_REQUEST_PER_GROUP);
            reqGroupService.denied(item.getGroupId(), FileRequestType.COPY, message);
        } else {
            if (!items.containsKey(tenant)) {
                items.put(tenant, new ConcurrentLinkedQueue<>());
            }
            items.get(tenant).add(item);
        }
    }

    public void handleSync(TenantWrapper<CopyFlowItem> wrapper) {
        CopyFlowItem item = wrapper.getContent();
        fileCopyReqService.copy(item.getFiles(), item.getGroupId());
        reqGroupService.granted(item.getGroupId(), FileRequestType.COPY, item.getFiles().size());
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<CopyFlowItem>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<CopyFlowItem> tenantItems = entry.getValue();
                List<CopyFlowItem> list = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        CopyFlowItem doc = tenantItems.poll();
                        if (doc == null) {
                            // Less than BULK_SIZE documents, bulk save what we have already
                            break;
                        } else { // enqueue document
                            list.add(doc);
                        }
                    }
                    if (!list.isEmpty()) {
                        LOGGER.info("[COPY FLOW HANDLER] Bulk saving {} CopyFlowItem...", list.size());
                        long start = System.currentTimeMillis();
                        fileCopyReqService.copy(list);
                        LOGGER.info("[COPY FLOW HANDLER] {} CopyFlowItem handled in {} ms", list.size(),
                                    System.currentTimeMillis() - start);
                        list.clear();
                    }
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
