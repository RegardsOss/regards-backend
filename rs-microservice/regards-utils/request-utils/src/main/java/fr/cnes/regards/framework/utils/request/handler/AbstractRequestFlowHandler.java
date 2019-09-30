/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.request.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Common handler behaviour
 * @author Marc SORDI
 */
public abstract class AbstractRequestFlowHandler<T extends ISubscribable> implements IHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRequestFlowHandler.class);

    private final Map<String, ConcurrentLinkedQueue<T>> items = new ConcurrentHashMap<>();

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void handle(TenantWrapper<T> wrapper) {
        LOGGER.trace("New request received for tenant {}", wrapper.getTenant());
        String tenant = wrapper.getTenant();
        T item = wrapper.getContent();
        if (!items.containsKey(tenant)) {
            items.put(tenant, new ConcurrentLinkedQueue<>());
        }
        items.get(tenant).add(item);
    }

    /**
     * Bulk save queued items every.
     * Call this method when you want to process the queue.
     *
     * For instance, attach a scheduler on concrete class
     */
    protected void handleQueue() {
        for (Map.Entry<String, ConcurrentLinkedQueue<T>> entry : items.entrySet()) {
            try {
                runtimeTenantResolver.forceTenant(entry.getKey());
                ConcurrentLinkedQueue<T> tenantItems = entry.getValue();
                List<T> items = new ArrayList<>();
                do {
                    // Build a 10_000 (at most) documents bulk request
                    for (int i = 0; i < getBulkSize(); i++) {
                        T item = tenantItems.poll();
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
                    LOGGER.debug("Processing bulk of {} items", items.size());
                    long start = System.currentTimeMillis();
                    processBulk(items);
                    LOGGER.debug("{} items registered in {} ms", items.size(), System.currentTimeMillis() - start);
                    items.clear();
                } while (tenantItems.size() >= getBulkSize()); // continue while more than BULK_SIZE items are to be saved
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    protected abstract Integer getBulkSize();

    protected abstract void processBulk(List<T> items);
}
