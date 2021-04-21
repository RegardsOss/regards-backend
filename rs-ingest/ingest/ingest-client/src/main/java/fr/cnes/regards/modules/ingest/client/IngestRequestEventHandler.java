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
package fr.cnes.regards.modules.ingest.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;

/**
 *
 * Listen to {@link IngestRequestEvent} and call back the client on each one.
 *
 * @author Marc SORDI
 */
@Component
public class IngestRequestEventHandler
        implements IHandler<IngestRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestEventHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.ingest.client.responses.items.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired(required = false)
    private IIngestClientListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    private final Map<String, ConcurrentLinkedQueue<IngestRequestEvent>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(IngestRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect ingest request events!");
        }
    }

    @Override
    public void handle(TenantWrapper<IngestRequestEvent> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.trace("[EVENT] New IngestRequestEvent received -- {}", wrapper.getContent().toString());

        while ((items.get(tenant) != null) && (items.get(tenant).size() >= (50 * BULK_SIZE))) {
            // Do not overload the concurrent queue if the configured listener does not handle queued message faster
            try {
                LOGGER.warn("Slow process detected. Waiting 30s for getting new message from amqp queue.");
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                LOGGER.error(String
                        .format("Error waiting for storage client responses handling by custom listener. Current responses pool to handle = %s",
                                items.size()),
                             e);
            }
        }
        IngestRequestEvent item = wrapper.getContent();
        if (!items.containsKey(tenant)) {
            items.put(tenant, new ConcurrentLinkedQueue<>());
        }
        items.get(tenant).add(item);
    }

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<IngestRequestEvent>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<IngestRequestEvent> tenantItems = entry.getValue();
                List<IngestRequestEvent> list = new ArrayList<>();
                do {
                    // Build a 100 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        IngestRequestEvent doc = tenantItems.poll();
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
                        LOGGER.info("[INGEST RESPONSES HANDLER] Total events queue size={}", tenantItems.size());
                        LOGGER.info("[INGEST RESPONSES HANDLER] Handling {} IngestRequestEvent...", list.size());
                        long start = System.currentTimeMillis();
                        handle(list);
                        LOGGER.info("[INGEST RESPONSES HANDLER] {} IngestRequestEvent handled in {} ms", list.size(),
                                    System.currentTimeMillis() - start);
                        list.clear();
                    }
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    public void handle(Collection<IngestRequestEvent> events) {
        Set<RequestInfo> success = Sets.newHashSet();
        Set<RequestInfo> errors = Sets.newHashSet();
        Set<RequestInfo> granted = Sets.newHashSet();
        Set<RequestInfo> denied = Sets.newHashSet();
        for (IngestRequestEvent event : events) {
            RequestInfo info = RequestInfo.build(event.getRequestId(), event.getProviderId(), event.getSipId(),
                                                 event.getErrors());
            switch (event.getState()) {
                case SUCCESS:
                    success.add(info);
                    break;
                case ERROR:
                    errors.add(info);
                    break;
                case GRANTED:
                    granted.add(info);
                    break;
                case DENIED:
                    denied.add(info);
                    break;
                default:
                    break;
            }
        }
        if (!denied.isEmpty()) {
            listener.onDenied(denied);
            denied.clear();
        }
        if (!granted.isEmpty()) {
            listener.onGranted(granted);
            granted.clear();
        }
        if (!errors.isEmpty()) {
            listener.onError(errors);
            errors.clear();
        }
        if (!success.isEmpty()) {
            listener.onSuccess(success);
            success.clear();
        }
    }
}
