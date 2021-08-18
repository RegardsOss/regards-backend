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
package fr.cnes.regards.framework.modules.session.commons.service.jobs;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotJobEventService.class);

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link JobEvent}s
     */
    public void updateSnapshotProcess(List<JobEvent> events) {
        // Filter out all jobs with RUNNING state
        events.removeIf(jobEvent -> jobEvent.getJobEventType().equals(JobEventType.RUNNING));

        // Filter out all jobs not related to AgentSnapshotJob ids saved in snapshot processes repo
        List<UUID> jobIds = events.stream().map(JobEvent::getJobId).collect(Collectors.toList());
        Set<SnapshotProcess> snapshotProcesses = this.snapshotRepo.findByJobIdIn(jobIds);

        // Update snapshot processes
        for (SnapshotProcess snapshotProcess : snapshotProcesses) {
            Optional<JobInfo> jobInfoOpt = this.jobInfoRepo.findById(snapshotProcess.getJobId());

            if (jobInfoOpt.isPresent()) {
                JobInfo jobInfo = jobInfoOpt.get();
                JobStatusInfo jobInfoStatus = jobInfo.getStatus();

                switch (jobInfoStatus.getStatus()) {
                    case SUCCEEDED:
                    case FAILED:
                    case ABORTED:
                        snapshotProcess.setJobId(null);
                        LOGGER.trace("Updated SnapshotProcess with source {} following a SnapshotJobEvent",
                                     snapshotProcess.getSource());
                        break;
                    default:
                        break;
                }
            }
        }
        this.snapshotRepo.saveAll(snapshotProcesses);
    }
}