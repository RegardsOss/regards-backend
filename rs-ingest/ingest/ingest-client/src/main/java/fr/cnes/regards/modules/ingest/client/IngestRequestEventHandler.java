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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Set;

/**
 * Listen to {@link IngestRequestEvent} and call back the client on each one.
 *
 * @author Marc SORDI
 */
@Component
public class IngestRequestEventHandler
        implements IBatchHandler<IngestRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestEventHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.ingest.client.responses.items.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired(required = false)
    private IIngestClientListener listener;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(IngestRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect ingest request events!");
        }
    }

    @Override
    public void handleBatch(List<IngestRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info("[INGEST RESPONSES HANDLER] Handling {} IngestRequestEvent...", events.size());
        Set<RequestInfo> success = Sets.newHashSet();
        Set<RequestInfo> errors = Sets.newHashSet();
        Set<RequestInfo> granted = Sets.newHashSet();
        Set<RequestInfo> denied = Sets.newHashSet();
        for (IngestRequestEvent event : events) {
            RequestInfo info = RequestInfo
                    .build(event.getRequestId(), event.getProviderId(), event.getSipId(), event.getErrors());
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
        LOGGER.info("[INGEST RESPONSES HANDLER] {} IngestRequestEvent handled in {} ms", events.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(IngestRequestEvent message) {
        return null;
    }

    @Override
    public int getBatchSize() {
        return this.BULK_SIZE;
    }

}
