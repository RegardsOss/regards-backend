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
package fr.cnes.regards.modules.storagelight.service.file.flow;

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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.RequestsGroupService;

/**
 * Handler to handle {@link ReferenceFlowItem} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class ReferenceFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<ReferenceFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceFlowItemHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    private static final int BULK_SIZE = 1_000;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileReferenceRequestService fileRefReqService;

    @Autowired
    private RequestsGroupService reqGroupService;

    private final Map<String, ConcurrentLinkedQueue<ReferenceFlowItem>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ReferenceFlowItem.class, this);
    }

    /**
     * Only add the message in the list of messages handled by bulk in the scheduled method
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    @Override
    public void handle(TenantWrapper<ReferenceFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        ReferenceFlowItem item = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.trace("[EVENT] New FileReferenceFlowItem received -- {}", wrapper.getContent().toString());
        if (item.getFiles().size() > ReferenceFlowItem.MAX_REQUEST_PER_GROUP) {
            String message = String.format("Number of reference requests for group %s exeeds maximum limit of %d",
                                           item.getGroupId(), ReferenceFlowItem.MAX_REQUEST_PER_GROUP);
            reqGroupService.denied(item.getGroupId(), FileRequestType.REFERENCE, message);
        } else {
            if (!items.containsKey(tenant)) {
                items.put(tenant, new ConcurrentLinkedQueue<>());
            }
            items.get(tenant).add(item);
            reqGroupService.granted(item.getGroupId(), FileRequestType.REFERENCE, item.getFiles().size());
        }
    }

    /**
     * Method for tests to handle synchronously one message
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    public void handleSync(TenantWrapper<ReferenceFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        ReferenceFlowItem item = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        reqGroupService.granted(item.getGroupId(), FileRequestType.REFERENCE, item.getFiles().size());
        try {
            fileRefReqService.reference(item.getFiles(), item.getGroupId());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<ReferenceFlowItem>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<ReferenceFlowItem> tenantItems = entry.getValue();
                List<ReferenceFlowItem> list = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        ReferenceFlowItem doc = tenantItems.poll();
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
                    LOGGER.info("[REFERENCE FLOW HANDLER] Bulk saving {} AddFileRefFlowItem...", list.size());
                    long start = System.currentTimeMillis();
                    reference(list);
                    LOGGER.info("[REFERENCE FLOW HANDLER] {} AddFileRefFlowItem handled in {} ms", list.size(),
                                System.currentTimeMillis() - start);
                    list.clear();
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * @param list
     */
    private void reference(List<ReferenceFlowItem> list) {
        for (ReferenceFlowItem item : list) {
            fileRefReqService.reference(item.getFiles(), item.getGroupId());
        }
    }
}
