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
package fr.cnes.regards.framework.modules.session.commons.service.jobs;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * Service for {@link SnapshotJobEventHandler}. Update {@link SnapshotProcess} according to the
 * status
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class SnapshotJobEventService {

    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotJobEventService.class);

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link JobEvent}s
     */
    public void updateSnapshotProcess(List<JobEvent> events) {
        List<UUID> terminatedJobIds = events.stream()
                                            .filter(event -> JobEventType.runnings().contains(event.getJobEventType()))
                                            .map(JobEvent::getJobId)
                                            .toList();
        this.snapshotRepo.removeTerminatedJobsById(terminatedJobIds);
    }
}