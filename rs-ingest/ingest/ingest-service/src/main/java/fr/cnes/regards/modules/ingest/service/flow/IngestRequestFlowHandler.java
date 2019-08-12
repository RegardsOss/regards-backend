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
package fr.cnes.regards.modules.ingest.service.flow;

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
import fr.cnes.regards.modules.ingest.domain.dto.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IIngestService;

/**
 * This handler absorbs the incoming SIP flow
 *
 * @author Marc SORDI
 *
 */
@Component
public class IngestRequestFlowHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<IngestRequestFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestFlowHandler.class);

    private static final int BULK_SIZE = 1_000;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private final Map<String, ConcurrentLinkedQueue<IngestRequestFlowItem>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(IngestRequestFlowItem.class, this);
    }

    @Override
    public void handle(TenantWrapper<IngestRequestFlowItem> wrapper) {
        LOGGER.trace("New ingest request for tenant {}", wrapper.getTenant());
        try {
            String tenant = wrapper.getTenant();
            IngestRequestFlowItem item = wrapper.getContent();
            if (!items.containsKey(tenant)) {
                items.put(tenant, new ConcurrentLinkedQueue<>());
            }
            items.get(tenant).add(item);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelayString = "${regards.ingest.request.delay:1000}")
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<IngestRequestFlowItem>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<IngestRequestFlowItem> tenantItems = entry.getValue();
                List<IngestRequestFlowItem> items = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        IngestRequestFlowItem item = tenantItems.poll();
                        if (item == null) {
                            if (items.isEmpty()) {
                                // Nothing to do
                                return;
                            }
                            // Less than BULK_SIZE documents, bulk save what we have already
                            break;
                        } else { // enqueue item
                            items.add(item);
                        }
                    }
                    LOGGER.info("Registering {} ingest request(s)", items.size());
                    long start = System.currentTimeMillis();
                    ingestService.registerIngestRequests(items);
                    LOGGER.info("{} ingest request(s) registered in {} ms", items.size(),
                                System.currentTimeMillis() - start);
                    items.clear();
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
