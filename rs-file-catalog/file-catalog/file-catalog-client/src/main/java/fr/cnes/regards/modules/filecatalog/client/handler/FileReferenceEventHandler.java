/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.client.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle Bus messages {@link FileReferenceEvent}
 *
 * @author Sébastien Binda
 */
@Profile("!test")
@Component("clientFileRefEventHandler")
public class FileReferenceEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileReferenceEvent> {

    @Autowired(required = false)
    private IStorageFileListener listener;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileReferenceEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileReferenceEvent bus messages !!");
        }
    }

    @Override
    public Errors validate(FileReferenceEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FileReferenceEvent> messages) {
        LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileReferenceEvent...", messages.size());
        long start = System.currentTimeMillis();
        handle(messages);
        LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileReferenceEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link FileReferenceEvent}s
     */
    private void handle(List<FileReferenceEvent> events) {
        List<FileReferenceEvent> availabilityError = new ArrayList<>();
        List<FileReferenceEvent> available = new ArrayList<>();
        List<FileReferenceEvent> deletedForOwner = new ArrayList<>();
        List<FileReferenceEvent> stored = new ArrayList<>();
        List<FileReferenceEvent> storedError = new ArrayList<>();
        for (FileReferenceEvent event : events) {
            switch (event.getType()) {
                case AVAILABILITY_ERROR:
                    availabilityError.add(event);
                    break;
                case AVAILABLE:
                    available.add(event);
                    break;
                case DELETED_FOR_OWNER:
                    deletedForOwner.add(event);
                    break;
                case DELETION_ERROR:
                case FULLY_DELETED:
                    // request handling in storage is done so that these type of event are never sent
                    break;
                case STORED:
                    stored.add(event);
                    break;
                case STORE_ERROR:
                    storedError.add(event);
                    break;
                default:
                    break;
            }
        }
        //Handle each type of action in right order
        if (!storedError.isEmpty()) {
            handleStoredError(storedError);
        }
        if (!stored.isEmpty()) {
            handleStored(stored);
        }
        if (!available.isEmpty()) {
            handleAvailable(available);
        }
        if (!availabilityError.isEmpty()) {
            handleAvailabilityError(availabilityError);
        }
        if (!deletedForOwner.isEmpty()) {
            handleDeletedForOwner(deletedForOwner);
        }
    }

    private void handleDeletedForOwner(List<FileReferenceEvent> deletedForOwner) {
        ListMultimap<String, FileReferenceEvent> deletedForOwnerPerOwner = ArrayListMultimap.create();
        for (FileReferenceEvent dto : deletedForOwner) {
            for (String owner : dto.getOwners()) {
                deletedForOwnerPerOwner.put(owner, dto);
            }
        }
        deletedForOwnerPerOwner.keySet()
                               .forEach(owner -> listener.onFileDeletedForOwner(owner,
                                                                                deletedForOwnerPerOwner.get(owner)));
    }

    private void handleAvailabilityError(List<FileReferenceEvent> availabilityError) {
        listener.onFileNotAvailable(availabilityError);
    }

    private void handleAvailable(List<FileReferenceEvent> available) {
        listener.onFileAvailable(available);
    }

    private void handleStored(List<FileReferenceEvent> stored) {
        listener.onFileStored(stored);
    }

    private void handleStoredError(List<FileReferenceEvent> storedError) {
        listener.onFileStoreError(storedError);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }

}
