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
package fr.cnes.regards.modules.workermanager.service.cache.confupdated;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.workermanager.dto.events.internal.WorkerConfUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publisher to send AMQP message notification when there is any change on Worker configurations.
 *
 * @author Léo Mieulet
 */
@Component
public class WorkerConfUpdatedEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConfUpdatedEventPublisher.class);

    @Autowired
    private IPublisher publisher;

    /**
     * Notify all WorkerManager there is a modification on WorkerConfigurations saved on database
     */
    public void publishEvent() {
        LOGGER.trace("Publishing WorkerConfUpdatedEvent.");
        publisher.publish(new WorkerConfUpdatedEvent());
    }
}
