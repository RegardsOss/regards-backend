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
package fr.cnes.regards.framework.modules.session.commons.service.jobs;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Listen to JobEvent Completion to update {@link fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
public class SnapshotJobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SnapshotJobEventService snapshotJobEventService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public Errors validate(JobEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<JobEvent> messages) {
        LOGGER.trace("[AGENT SNAPSHOT JOB EVENT HANDLER] Handling {} JobEvents...", messages.size());
        long start = System.currentTimeMillis();
        // sort job
        snapshotJobEventService.updateSnapshotProcess(messages);
        LOGGER.trace("[AGENT SNAPSHOT JOB EVENT HANDLER] {} JobEvents handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}