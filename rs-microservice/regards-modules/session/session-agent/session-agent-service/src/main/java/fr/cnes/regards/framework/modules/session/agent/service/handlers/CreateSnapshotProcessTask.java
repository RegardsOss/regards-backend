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
package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import com.google.common.collect.Sets;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

import java.util.Set;

/**
 * Task to check for new session snappshot process to create.
 * Thsi task can only be exectuted by one instance of the microservice at a time.
 *
 * @author Sebastien Binda
 */
public class CreateSnapshotProcessTask implements Task {

    /**
     * Repository to save snapshot processes
     */
    private final SessionAgentHandlerService sessionAgentService;

    private Set<String> sources = Sets.newHashSet();

    public CreateSnapshotProcessTask(SessionAgentHandlerService sessionAgentService, Set<String> sources) {
        this.sessionAgentService = sessionAgentService;
        if (sources != null) {
            this.sources = sources;
        }
    }

    @Override
    public void call() throws Throwable {
        LockAssert.assertLocked();
        if (!sources.isEmpty()) {
            sessionAgentService.createMissingSnapshotProcesses(sources);
        }
    }
}
