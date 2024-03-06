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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.workermanager.amqp.events.internal.WorkerConfUpdatedEvent;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import fr.cnes.regards.modules.workermanager.service.config.WorkflowConfigCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler of {@link WorkerConfUpdatedEvent} events
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!nohandler")
public class WorkerConfUpdatedEventHandler
    implements IBatchHandler<WorkerConfUpdatedEvent>, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    @Autowired
    private WorkflowConfigCacheService workflowConfigCacheService;

    @Override
    public Class<WorkerConfUpdatedEvent> getMType() {
        return WorkerConfUpdatedEvent.class;
    }

    @Override
    public Errors validate(WorkerConfUpdatedEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<WorkerConfUpdatedEvent> messages) {
        workerConfigCacheService.cleanCache();
        workflowConfigCacheService.cleanCache();
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(WorkerConfUpdatedEvent.class, this);
    }
}
