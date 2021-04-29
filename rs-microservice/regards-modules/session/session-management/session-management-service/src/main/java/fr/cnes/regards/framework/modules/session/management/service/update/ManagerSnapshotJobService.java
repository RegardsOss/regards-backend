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
package fr.cnes.regards.framework.modules.session.management.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class ManagerSnapshotJobService {

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSnapshotJobService.class);

    public void scheduleJob() {
        long start = System.currentTimeMillis();
        LOGGER.info("[MANAGER SNAPSHOT SCHEDULER] Scheduling job at date {}...", OffsetDateTime.now());

        // Freeze start date to select stepEvents
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();
        List<SnapshotProcess> snapshotProcessesRetrieved = this.snapshotProcessRepo.findAll();

        // Filter out all snapshot processes currently running or with no step events to update
        Predicate<SnapshotProcess> predicateAlreadyProcessed = process -> (process.getJobId() != null) || (
                (process.getLastUpdateDate() == null && sessionStepRepo
                        .countBySourceAndLastUpdateDateBefore(process.getSource(), schedulerStartDate) == 0) || (
                        process.getLastUpdateDate() != null && sessionStepRepo
                                .countBySourceAndLastUpdateDateBetween(process.getSource(), process.getLastUpdateDate(),
                                                                       schedulerStartDate) == 0));

        snapshotProcessesRetrieved.removeIf(predicateAlreadyProcessed);

        // IF EVENTS WERE ADDED
        // launch one job per snapshotProcess, ie, one job per source
        if (!snapshotProcessesRetrieved.isEmpty()) {
            for (SnapshotProcess snapshotProcessToUpdate : snapshotProcessesRetrieved) {
                // create one job per each source
                HashSet<JobParameter> jobParameters = Sets
                        .newHashSet(new JobParameter(ManagerSnapshotJob.SNAPSHOT_PROCESS, snapshotProcessToUpdate),
                                    new JobParameter(ManagerSnapshotJob.FREEZE_DATE, schedulerStartDate));
                JobInfo jobInfo = new JobInfo(false, 0, jobParameters, null, ManagerSnapshotJob.class.getName());

                // create job
                jobInfo = jobInfoService.createAsQueued(jobInfo);

                // update snapshot process with new job id to indicate there is a current process ongoing
                snapshotProcessToUpdate.setJobId(jobInfo.getId());
                this.snapshotProcessRepo.save(snapshotProcessToUpdate);

                LOGGER.info("[MANAGER SNAPSHOT SCHEDULER] ManagerSnapshotJob scheduled in {} ms for source {}",
                            System.currentTimeMillis() - start, snapshotProcessToUpdate.getSource());
            }
        } else {
            LOGGER.info("[MANAGER SNAPSHOT SCHEDULER] No sessionSteps found to be updated. Handled in {} ms",
                        System.currentTimeMillis() - start);
        }
    }
}
