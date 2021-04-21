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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.processing.domain.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import io.vavr.control.Try;

import java.util.Collection;

/**
 * This interface defines signatures for a wrapper around {@link IPublisher},
 * used in  to be able to modify the sending behaviour during tests.
 *
 * @author Guillaume Andrieu
 *
 */
public interface IProcessingEventSender {

    Try<PExecutionRequestEvent> sendProcessingRequest(PExecutionRequestEvent event);

    Try<DownloadedOutputFilesEvent> sendDownloadedFilesNotification(DownloadedOutputFilesEvent event);

    Try<DownloadedOutputFilesEvent> sendDownloadedFilesNotification(Collection<OrderDataFile> dataFiles);
}
