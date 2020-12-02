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
package fr.cnes.regards.modules.order.service.processing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.processing.domain.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import io.vavr.control.Try;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Service
@Profile("!test")
public class ProcessingEventSender implements IProcessingEventSender {

    private final IPublisher publisher;

    @Autowired
    public ProcessingEventSender(IPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Try<PExecutionRequestEvent> sendProcessingRequest(PExecutionRequestEvent event) {
        return Try.of(() -> {
            publisher.publish(event);
            return event;
        });
    }

    @Override
    public Try<DownloadedOutputFilesEvent> sendDownloadedFilesNotification(DownloadedOutputFilesEvent event) {
        return Try.of(() -> {
            publisher.publish(event);
            return event;
        });
    }
}
