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
package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Handler for new {@link StepPropertyUpdateRequestEvent}s
 *
 * @author Iliana Ghazali
 **/
public class SessionAgentEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StepPropertyUpdateRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAgentEventHandler.class);

    private static final String SNAPSHOT_CREATE_LOCK = "SNAPSHOT_AGENT_CREATE_LOCK";

    private static final Long MAX_TASK_WAIT_DURING_SCHEDULE = 30L; // In second

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SessionAgentHandlerService sessionAgentHandlerService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StepPropertyUpdateRequestEvent.class, this);
    }

    @Override
    public Errors validate(StepPropertyUpdateRequestEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<StepPropertyUpdateRequestEvent> messages) {
        LOGGER.trace("[STEP EVENT HANDLER] Handling {} StepEvents...", messages.size());
        long start = System.currentTimeMillis();
        createMissingSnapshotProcessesTask(sessionAgentHandlerService.createStepRequests(messages), true);
        LOGGER.trace("[STEP EVENT HANDLER] {} StepEvents handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    /**
     * Ensure unique access to database during checking missing snapshot to creates for newly created steps.
     *
     * @param sources    source to check if snapshot is missing
     * @param retryError true to retry once if a lock error occurs
     */
    public void createMissingSnapshotProcessesTask(Set<String> sources, boolean retryError) {
        try {
            lockingTaskExecutors.executeWithLock(new CreateSnapshotProcessTask(sessionAgentHandlerService,
                                                                               sources,
                                                                               lockingTaskExecutors),
                                                 new LockConfiguration(SNAPSHOT_CREATE_LOCK,
                                                                       Instant.now()
                                                                              .plusSeconds(MAX_TASK_WAIT_DURING_SCHEDULE)));
        } catch (Throwable e) {
            if (retryError) {
                createMissingSnapshotProcessesTask(sources, false);
            } else {
                LOGGER.error("Error creating missing sessions sources snapshots !!", e);
            }
        }
    }
}