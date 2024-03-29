/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.flow.FlowItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handle {@link FileRequestsGroupEvent} events.
 *
 * @author Sébastien Binda
 */
@Component("clientRequestEventHandler")
public class FileRequestGroupEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileRequestsGroupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestGroupEventHandler.class);

    @Value("${regards.storage.client.response.batch.size:500}")
    private Integer batchSize;

    @Autowired(required = false)
    private IStorageRequestListener listener;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileRequestsGroupEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileRequestEvent bus messages !!");
        }
    }

    /**
     * Bulk save queued items every second.
     */
    @Override
    public void handleBatch(List<FileRequestsGroupEvent> messages) {
        LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileRequestsGroupEvent...", messages.size());
        long start = System.currentTimeMillis();
        handle(messages);
        LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileRequestsGroupEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
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
        LOGGER.trace(
            "[STORAGE RESPONSES HANDLER] handling {} FileRequestsGroupEvent(s) dispatch by {} dones, {} granted, {} denied",
            events.size(),
            dones.size(),
            granted.size(),
            denied.size());
        handleDone(dones);
        handleGranted(granted);
        handleDenied(denied);
    }

    private void handleDenied(Set<FileRequestsGroupEvent> denied) {
        if ((denied != null) && !denied.isEmpty()) {
            listener.onRequestDenied(denied.stream()
                                           .map(e -> RequestInfo.build(e.getGroupId(), e.getSuccess(), e.getErrors())
                                                                .withMessage(e.getMessage()))
                                           .collect(Collectors.toSet()));
        }
    }

    private void handleGranted(Set<FileRequestsGroupEvent> granted) {
        if ((granted != null) && !granted.isEmpty()) {
            listener.onRequestGranted(granted.stream()
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
            switch (event.getType()) {
                case AVAILABILITY:
                    fillSuccessOrErrorSet(event, availables, availableErrors);
                    break;
                case DELETION:
                    fillSuccessOrErrorSet(event, deleted, deletionErrors);
                    break;
                case REFERENCE:
                    fillSuccessOrErrorSet(event, referenced, referenceErrors);
                    break;
                case STORAGE:
                    fillSuccessOrErrorSet(event, stored, storeErrors);
                    break;
                case COPY:
                    fillSuccessOrErrorSet(event, copied, copyErrors);
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

    private void fillSuccessOrErrorSet(FileRequestsGroupEvent event,
                                       Set<RequestInfo> successSet,
                                       Set<RequestInfo> errorSet) {
        RequestInfo ri = RequestInfo.build(event.getGroupId(), event.getSuccess(), event.getErrors());
        if (event.getState() == FlowItemStatus.SUCCESS) {
            successSet.add(ri);
        } else {
            errorSet.add(ri);
        }
    }

    @Override
    public Errors validate(FileRequestsGroupEvent message) {
        return null;
    }

}
