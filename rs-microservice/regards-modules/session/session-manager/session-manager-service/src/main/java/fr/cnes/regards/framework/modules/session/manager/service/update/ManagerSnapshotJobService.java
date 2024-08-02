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
package fr.cnes.regards.framework.modules.session.manager.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Create {@link ManagerSnapshotJob} to create sources and sessions from session steps
 *
 * @author Iliana Ghazali
 **/
public class ManagerSnapshotJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSnapshotJobService.class);

    private static final String LOG_HEADER = "[MANAGER SNAPSHOT SCHEDULER] >>>";

    public static final int JOB_PRIORITY = 1000;

    private final int snapshotPropertyPageSize;

    private final JobInfoService jobInfoService;

    private final ISnapshotProcessRepository snapshotRepo;

    private final ISessionStepRepository sessionStepRepo;

    private final ManagerSnapshotJobService self;

    public ManagerSnapshotJobService(JobInfoService jobInfoService,
                                     ISnapshotProcessRepository snapshotRepo,
                                     ISessionStepRepository sessionStepRepo,
                                     ManagerSnapshotJobService managerSnapshotJobService,
                                     int snapshotPropertyPageSize) {
        this.jobInfoService = jobInfoService;
        this.snapshotRepo = snapshotRepo;
        this.snapshotPropertyPageSize = snapshotPropertyPageSize;
        this.sessionStepRepo = sessionStepRepo;
        this.self = managerSnapshotJobService;
    }

    public void scheduleJob() {
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();
        LOGGER.debug("{} Scheduling ManagerSnapshotJobs at {}...", LOG_HEADER, schedulerStartDate);
        int totalNbJobsScheduled = 0;
        Pageable pageable = PageRequest.of(0, snapshotPropertyPageSize, Sort.by(Sort.Order.asc("source")));
        boolean hasNext = true;

        while (hasNext) {
            Pair<Boolean, Integer> pairHasNextNbJobs = self.handlePageSnapshots(schedulerStartDate, pageable);
            hasNext = pairHasNextNbJobs.getFirst();
            totalNbJobsScheduled += pairHasNextNbJobs.getSecond();
            if (hasNext) {
                pageable = pageable.next();
            }
        }

        LOGGER.debug("{} Scheduled a total of {} ManagerSnapshotJobs in {} ms",
                     LOG_HEADER,
                     totalNbJobsScheduled,
                     Duration.between(schedulerStartDate, OffsetDateTime.now()).toMillis());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Pair<Boolean, Integer> handlePageSnapshots(OffsetDateTime schedulerStartDate, Pageable pageable) {
        Page<SnapshotProcess> snapshotPage = this.snapshotRepo.findByJobIdIsNull(pageable);
        // Filter out all snapshot processes currently running or with no step events to update
        Predicate<SnapshotProcess> predicateSnapAlreadyProcessed = process -> process.getLastUpdateDate() == null ?
            sessionStepRepo.countBySourceAndRegistrationDateBefore(process.getSource(), schedulerStartDate) == 0 :
            sessionStepRepo.countBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(process.getSource(),
                                                                                                   process.getLastUpdateDate(),
                                                                                                   schedulerStartDate)
            == 0;
        Set<SnapshotProcess> snapshotsRetrieved = new HashSet<>(snapshotPage.getContent());
        snapshotsRetrieved.removeIf(predicateSnapAlreadyProcessed);
        // launch one job per snapshotProcess, ie, one job per source
        if (!snapshotsRetrieved.isEmpty()) {
            return Pair.of(snapshotPage.hasNext(), createOneJobPerSnapshot(schedulerStartDate, snapshotsRetrieved));
        } else {
            LOGGER.trace("{} No ManagerSnapshotJobs scheduled for page number {}",
                         LOG_HEADER,
                         pageable.getPageNumber());
            return Pair.of(snapshotPage.hasNext(), 0);
        }
    }

    private int createOneJobPerSnapshot(OffsetDateTime schedulerStartDate,
                                        Set<SnapshotProcess> snapshotProcessesRetrieved) {
        int nbJobsScheduled = 0;
        for (SnapshotProcess snapshotProcessToUpdate : snapshotProcessesRetrieved) {
            // create one job per each source
            HashSet<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(ManagerSnapshotJob.SNAPSHOT_PROCESS,
                                                                                   snapshotProcessToUpdate),
                                                                  new JobParameter(ManagerSnapshotJob.FREEZE_DATE,
                                                                                   schedulerStartDate));
            JobInfo jobInfo = new JobInfo(false, JOB_PRIORITY, jobParameters, null, ManagerSnapshotJob.class.getName());

            // create job
            jobInfo = jobInfoService.createAsQueued(jobInfo);

            // update snapshot process with new job id to indicate there is a current process ongoing
            snapshotProcessToUpdate.setJobId(jobInfo.getId());
            this.snapshotRepo.save(snapshotProcessToUpdate);
            nbJobsScheduled++;

            LOGGER.trace("{} ManagerSnapshotJob scheduled for source {}",
                         LOG_HEADER,
                         snapshotProcessToUpdate.getSource());
        }
        return nbJobsScheduled;
    }
}