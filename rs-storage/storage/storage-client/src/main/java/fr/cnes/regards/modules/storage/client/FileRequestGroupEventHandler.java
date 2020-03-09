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
package fr.cnes.regards.modules.storage.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.flow.FlowItemStatus;

/**
 * Handle {@link FileRequestsGroupEvent} events.
 * @author Sébastien Binda
 */
@Component("clientRequestEventHandler")
public class FileRequestGroupEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<FileRequestsGroupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestGroupEventHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.storage.client.responses.items.bulk.size:100}")
    private int BULK_SIZE;

    @Autowired(required = false)
    private IStorageRequestListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    private final Map<String, ConcurrentLinkedQueue<FileRequestsGroupEvent>> items = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileRequestsGroupEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileRequestEvent bus messages !!");
        }
    }

    @Override
    public void handle(TenantWrapper<FileRequestsGroupEvent> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.trace("[EVENT] New FileStorageFlowItem received -- {}", wrapper.getContent().toString());

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
        FileRequestsGroupEvent item = wrapper.getContent();
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
        for (Map.Entry<String, ConcurrentLinkedQueue<FileRequestsGroupEvent>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<FileRequestsGroupEvent> tenantItems = entry.getValue();
                List<FileRequestsGroupEvent> list = new ArrayList<>();
                // Build a 100 (at most) documents bulk request
                for (int i = 0; i < BULK_SIZE; i++) {
                    FileRequestsGroupEvent doc = tenantItems.poll();
                    if (doc == null) {
                        // Less than BULK_SIZE documents, bulk save what we have already
                        break;
                    } else { // enqueue document
                        list.add(doc);
                    }
                }
                if (!list.isEmpty()) {
                    LOGGER.debug("[STORAGE RESPONSES HANDLER] Total events queue size={}", tenantItems.size());
                    LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileRequestsGroupEvent...", list.size());
                    long start = System.currentTimeMillis();
                    handle(list);
                    LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileRequestsGroupEvent handled in {} ms", list.size(),
                                 System.currentTimeMillis() - start);
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    private void handle(Collection<FileRequestsGroupEvent> events) {
        Set<FileRequestsGroupEvent> dones = Sets.newHashSet();
        Set<FileRequestsGroupEvent> granted = Sets.newHashSet();
        Set<FileRequestsGroupEvent> denied = Sets.newHashSet();
        for (FileRequestsGroupEvent event : events) {
            switch (event.getState()) {
                case SUCCESS:
                case ERROR:
                    dones.add(event);
                    break;
                case GRANTED:
                    granted.add(event);
                    break;
                case DENIED:
                    denied.add(event);
                    break;
                default:
                    break;
            }
        }
        LOGGER.trace("[STORAGE RESPONSES HANDLER] handling {} FileRequestsGroupEvent(s) dispatch by {} dones, {} granted, {} denied",
                     events.size(), dones.size(), granted.size(), denied.size());
        handleDone(dones);
        handleGranted(granted);
        handleDenied(denied);
    }

    /**
     * @param denied
     */
    private void handleDenied(Set<FileRequestsGroupEvent> events) {
        if ((events != null) && !events.isEmpty()) {
            listener.onRequestDenied(events.stream()
                    .map(e -> RequestInfo.build(e.getGroupId(), e.getSuccess(), e.getErrors()))
                    .collect(Collectors.toSet()));
        }
    }

    /**
     * @param granted
     */
    private void handleGranted(Set<FileRequestsGroupEvent> events) {
        if ((events != null) && !events.isEmpty()) {
            listener.onRequestGranted(events.stream()
                    .map(e -> RequestInfo.build(e.getGroupId(), e.getSuccess(), e.getErrors()))
                    .collect(Collectors.toSet()));
        }

    }

    private void handleDone(Set<FileRequestsGroupEvent> events) {
        Set<RequestInfo> availables = Sets.newHashSet();
        Set<RequestInfo> availableErrors = Sets.newHashSet();
        Set<RequestInfo> deleted = Sets.newHashSet();
        Set<RequestInfo> deletionErrors = Sets.newHashSet();
        Set<RequestInfo> referenced = Sets.newHashSet();
        Set<RequestInfo> referenceErrors = Sets.newHashSet();
        Set<RequestInfo> stored = Sets.newHashSet();
        Set<RequestInfo> storeErrors = Sets.newHashSet();
        Set<RequestInfo> copied = Sets.newHashSet();
        Set<RequestInfo> copyErrors = Sets.newHashSet();

        for (FileRequestsGroupEvent event : events) {
            RequestInfo ri = RequestInfo.build(event.getGroupId(), event.getSuccess(), event.getErrors());
            switch (event.getType()) {
                case AVAILABILITY:
                    if (event.getState() == FlowItemStatus.SUCCESS) {
                        availables.add(ri);
                    } else {
                        availableErrors.add(ri);
                    }
                    break;
                case DELETION:
                    if (event.getState() == FlowItemStatus.SUCCESS) {
                        deleted.add(ri);
                    } else {
                        deletionErrors.add(ri);
                    }
                    break;
                case REFERENCE:
                    if (event.getState() == FlowItemStatus.SUCCESS) {
                        referenced.add(ri);
                    } else {
                        referenceErrors.add(ri);
                    }
                    break;
                case STORAGE:
                    if (event.getState() == FlowItemStatus.SUCCESS) {
                        stored.add(ri);
                    } else {
                        storeErrors.add(ri);
                    }
                    break;
                case COPY:
                    if (event.getState() == FlowItemStatus.SUCCESS) {
                        copied.add(ri);
                    } else {
                        copyErrors.add(ri);
                    }
                    break;
                default:
                    break;
            }
        }
        if (!availables.isEmpty()) {
            listener.onAvailable(availables);
        }
        if (!availableErrors.isEmpty()) {
            listener.onAvailabilityError(availableErrors);
        }
        if (!deleted.isEmpty()) {
            listener.onDeletionSuccess(deleted);
        }
        if (!deletionErrors.isEmpty()) {
            listener.onDeletionError(deletionErrors);
        }
        if (!referenced.isEmpty()) {
            listener.onReferenceSuccess(referenced);
        }
        if (!referenceErrors.isEmpty()) {
            listener.onReferenceError(referenceErrors);
        }
        if (!stored.isEmpty()) {
            listener.onStoreSuccess(stored);
        }
        if (!storeErrors.isEmpty()) {
            listener.onStoreError(storeErrors);
        }
        if (!copied.isEmpty()) {
            listener.onCopySuccess(copied);
        }
        if (!copyErrors.isEmpty()) {
            listener.onCopyError(copyErrors);
        }
    }
}
