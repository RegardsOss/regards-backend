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
package fr.cnes.regards.modules.sessionmanager.service.events;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.service.ISessionService;

/**
 * Handler for SessionNotificationEvent events.
 * @author Léo Mieulet
 */
@Component
public class SessionNotificationHandler implements IHandler<SessionMonitoringEvent>, ISessionNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNotificationHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.admin.session.notification.items.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private ISubscriber subscriber;

    private final Map<String, ConcurrentLinkedQueue<SessionMonitoringEvent>> items = new ConcurrentHashMap<>();

    /**
     * Only add the message in the list of messages handled by bulk in the scheduled method
     * @param wrapper containing {@link ReferenceFlowItem} to handle
     */
    @Override
    public void handle(TenantWrapper<SessionMonitoringEvent> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.trace("[EVENT] New SessionMonitoringEvent received -- {}", wrapper.getContent().toString());
        while ((items.get(tenant) != null) && (items.get(tenant).size() >= (10 * BULK_SIZE))) {
            // Do not overload the concurrent queue if the configured listener does not handle queued message faster
            try {
                LOG.warn("Slow process detected. Waiting 30s for getting new message from amqp queue.");
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                LOG.error(String
                        .format("Error waiting for SessionMonitoringEvent handled by microservice. Current events pool to handle = %s",
                                items.size()),
                          e);
            }
        }
        SessionMonitoringEvent item = wrapper.getContent();
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
        for (Map.Entry<String, ConcurrentLinkedQueue<SessionMonitoringEvent>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<SessionMonitoringEvent> tenantItems = entry.getValue();
                List<SessionMonitoringEvent> list = new ArrayList<>();
                do {
                    // Build a 100 (at most) documents bulk request
                    for (int i = 0; i < BULK_SIZE; i++) {
                        SessionMonitoringEvent doc = tenantItems.poll();
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
                        LOG.info("[SESSION NOTIFICATIONS HANDLER] Bulk saving {} notifications...", list.size());
                        long start = System.currentTimeMillis();
                        sessionService.updateSessionProperties(list);
                        LOG.info("[SESSION NOTIFICATIONS HANDLER] {} Notifications handled in {} ms", list.size(),
                                 System.currentTimeMillis() - start);
                        list.clear();
                    }
                } while (tenantItems.size() >= BULK_SIZE); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.service.IPlop#onApplicationEvent(org.springframework.boot.context.event.ApplicationReadyEvent)
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(SessionMonitoringEvent.class, this);
    }
}
