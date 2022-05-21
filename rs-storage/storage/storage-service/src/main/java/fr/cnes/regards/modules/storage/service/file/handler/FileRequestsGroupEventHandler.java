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
package fr.cnes.regards.modules.storage.service.file.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.flow.FlowItemStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle actions after a group requests ends.
 *
 * @author Sébastien Binda
 */
@Component
public class FileRequestsGroupEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileRequestsGroupEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileRequestsGroupEvent.class, this);
    }

    @Override
    public Errors validate(FileRequestsGroupEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FileRequestsGroupEvent> messages) {
        messages.forEach(this::handle);
    }

    public void handle(FileRequestsGroupEvent event) {
        LOGGER.trace("Handling {}", event.toString());
        if ((event.getState() == FlowItemStatus.SUCCESS) || (event.getState() == FlowItemStatus.ERROR)) {
            switch (event.getType()) {
                case DELETION:
                    handleDeletionGroupDone(event);
                    break;
                case AVAILABILITY:
                case COPY:
                case REFERENCE:
                case STORAGE:
                default:
                    break;
            }
        }
    }

    private void handleDeletionGroupDone(FileRequestsGroupEvent event) {
        if (event.getState() == FlowItemStatus.ERROR) {
            notificationClient.notify(String.format(
                                          "Requests group %s is terminated with erros. %s success and %s errors.",
                                          event.getGroupId(),
                                          event.getSuccess().size(),
                                          event.getErrors().size()),
                                      String.format("Storage - %s process", event.getType().toString()),
                                      NotificationLevel.ERROR,
                                      DefaultRole.PROJECT_ADMIN);
        }
    }
}
